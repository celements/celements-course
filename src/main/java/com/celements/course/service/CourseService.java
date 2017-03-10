package com.celements.course.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.model.ModelConfiguration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.classes.IClassCollectionRole;
import com.celements.course.classcollections.CourseClasses;
import com.celements.course.registration.Person;
import com.celements.course.registration.RegistrationData;
import com.celements.mailsender.IMailSenderRole;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentAccessException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.celements.rendering.RenderCommand;
import com.celements.web.plugin.cmd.CelMailConfiguration;
import com.celements.web.plugin.cmd.ConvertToPlainTextException;
import com.celements.web.plugin.cmd.PlainTextCommand;
import com.celements.web.service.IWebUtilsService;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.web.XWikiRequest;

@Component
public class CourseService implements ICourseServiceRole {

  private static Logger LOGGER = LoggerFactory.getLogger(CourseService.class);

  static final String CFGSRC_PARTICIPANT_DOC_NAME_PREFIX = "celements.course.participant.docNamePrefix";

  @Requirement("CelCourseClasses")
  private IClassCollectionRole courseClasses;

  @Requirement
  private IWebUtilsService webUtilsService;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelContext modelContext;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelConfiguration modelConfig;

  @Requirement
  private INextFreeDocRole nextFreeDoc;

  @Requirement
  private IMailSenderRole mailSender;

  @Requirement
  private ConfigurationSource cfgSrc;

  @Requirement
  private Execution execution;

  @Deprecated
  private XWikiContext getContext() {
    return (XWikiContext) execution.getContext().getProperty("xwikicontext");
  }

  @Override
  public DocumentReference getCourseTypeForCourse(DocumentReference courseDocRef)
      throws XWikiException {
    DocumentReference typeDocRef = null;
    WikiReference wikiRef = webUtilsService.getWikiRef(courseDocRef);
    BaseObject courseObj = null;
    try {
      courseObj = modelAccess.getXObject(courseDocRef, getCourseClasses().getCourseClassRef(
          wikiRef.getName()));
      if (courseObj != null) {
        String typeFN = courseObj.getStringValue("type");
        if (StringUtils.isNotBlank(typeFN)) {
          typeDocRef = webUtilsService.resolveDocumentReference(typeFN, wikiRef);
        }
      }
    } catch (DocumentNotExistsException dnee) {
      LOGGER.error("getCourseTypeForCourse: Course document {} does not exist", courseDocRef, dnee);
    }
    return typeDocRef;
  }

  @Override
  public String getCourseTypeName(DocumentReference courseTypeDocRef) throws XWikiException {
    String typeName = "";
    BaseObject typeObj = null;
    try {
      typeObj = modelAccess.getXObject(courseTypeDocRef, getCourseClasses().getCourseTypeClassRef(
          webUtilsService.getWikiRef(courseTypeDocRef).getName()));
      if (typeObj != null) {
        typeName = typeObj.getStringValue("typeName");
      }
    } catch (DocumentNotExistsException dnee) {
      LOGGER.error("getCourseTypeName: Course document {} does not exist", courseTypeDocRef, dnee);
    }
    return typeName;
  }

  @Override
  public boolean registerParticipantFromRequest(boolean sendConfirmationMail) {
    XWikiRequest req = getContext().getRequest();
    LOGGER.trace("registerParticipantFromRequest: request '{}': {}", req.hashCode(), req);
    RegistrationData data = new RegistrationData();
    data.setData(req);
    if (!Strings.isNullOrEmpty(data.getEventid())) {
      DocumentReference courseDocRef = modelUtils.resolveRef(data.getEventid(),
          DocumentReference.class);
      data.setRegDocRef(createParticipantDocRef(courseDocRef));
      try {
        XWikiDocument regDoc = modelAccess.createDocument(data.getRegDocRef());
        Optional<DocumentReference> templRef = getTemplRef();
        if (templRef.isPresent()) {
          regDoc.readFromTemplate(templRef.get(), modelContext.getXWikiContext());
        }
        setMandatoryRegSpaceDocs(regDoc);
        createParticipantObjects(regDoc, data);
        modelAccess.saveDocument(regDoc, "created new registration");
        if (sendConfirmationMail) {
          sendConfirmationMails(data);
        }
        return true;
      } catch (DocumentAccessException | XWikiException excp) {
        LOGGER.error("exception while creating new registration", excp);
      }
    } else {
      LOGGER.info("registerParticipantFromRequest: request '{}' no eventid", req.hashCode());
    }
    return false;
  }

  private Optional<DocumentReference> getTemplRef() {
    DocumentReference templRef = null;
    String template = modelContext.getRequest().isPresent() ? Strings.nullToEmpty(
        modelContext.getRequest().get().getParameter("template")).trim() : "";
    if (!template.isEmpty()) {
      templRef = modelUtils.resolveRef(template, DocumentReference.class);
      templRef = modelAccess.exists(templRef) ? templRef : null;
    }
    return Optional.fromNullable(templRef);
  }

  void setMandatoryRegSpaceDocs(XWikiDocument regDoc) {
    if (regDoc.isNew()) {
      SpaceReference regSpace = regDoc.getDocumentReference().getLastSpaceReference();
      DocumentReference webPrefRef = new DocumentReference("WebPreferences", regSpace);
      if (!modelAccess.exists(webPrefRef)) {
        DocumentReference globalRightsRef = new DocumentReference("XWikiGlobalRights",
            new SpaceReference("XWiki", modelContext.getWikiRef()));
        XWikiDocument webPrefDoc = modelAccess.getOrCreateDocument(webPrefRef);
        BaseObject rightsObj = modelAccess.newXObject(webPrefDoc, globalRightsRef);
        modelAccess.setProperty(rightsObj, "groups", "XWiki.XWikiAdminGroup");
        modelAccess.setProperty(rightsObj, "levels", "view,edit,delete,undelete");
        modelAccess.setProperty(rightsObj, "users", "");
        modelAccess.setProperty(rightsObj, "allow", 1);
        try {
          modelAccess.saveDocument(webPrefDoc, "createdAndSetContent");
        } catch (DocumentSaveException dse) {
          LOGGER.error("Exception saving registration space WebPreferences document.", dse);
        }
      }
      DocumentReference webHomeRef = new DocumentReference("WebHome", regSpace);
      if (!modelAccess.exists(webHomeRef)) {
        XWikiDocument webHomeDoc = modelAccess.getOrCreateDocument(webHomeRef);
        webHomeDoc.setContent("#parse('celMacros/getRegistrationListing.vm')");
        try {
          modelAccess.saveDocument(webHomeDoc, "createdAndSetContent");
        } catch (DocumentSaveException dse) {
          LOGGER.error("Exception saving registration space WebHome document.", dse);
        }
      }
    }
  }

  private void createParticipantObjects(XWikiDocument regDoc, RegistrationData data) {
    DocumentReference classRef = getParticipantClassRef();
    List<Person> persons = data.getPersons();
    for (int nb = 0; nb < persons.size(); nb++) {
      Person person = persons.get(nb);
      if (!person.isEmpty() || (modelAccess.getXObjects(regDoc, classRef).size() == 0)) {
        BaseObject obj;
        if (modelAccess.getXObject(regDoc, classRef, nb).isPresent()) {
          obj = modelAccess.getXObject(regDoc, classRef, nb).get();
        } else {
          obj = modelAccess.newXObject(regDoc, classRef);
        }
        obj.setStringValue("eventid", data.getEventid());
        obj.setStringValue("title", person.getTitle());
        obj.setStringValue("firstname", person.getGivenName());
        obj.setStringValue("lastname", person.getSurname());
        obj.setStringValue("address", person.getAddress());
        obj.setStringValue("zip", person.getZip());
        obj.setStringValue("city", person.getCity());
        obj.setStringValue("phone", person.getPhone());
        obj.setStringValue("email", normalizeEmail(person.getEmail()));
        obj.setDateValue("dob", person.getDateOfBirth());
        obj.setStringValue("status", person.getStatus());
        obj.setStringValue("payed", "unpayed");
        obj.setLargeStringValue("comment", data.getComment());
        // using set since there is no setPassword method
        obj.set("validkey", data.getValidationKey(), getContext());
        obj.setDateValue("timestamp", new Date());
        obj.setStringValue("client", getClientInfo());
      }
    }
  }

  private void sendConfirmationMails(RegistrationData data) throws DocumentNotExistsException,
      XWikiException {
    getVeloContext().put("registrationData", data);
    XWikiDocument emailContentDoc = modelAccess.getDocument(new DocumentReference(
        modelContext.getWikiRef().getName(), "MailContent", "NeueAnmeldung"));
    for (Person person : data.getPersons()) {
      sendMail(null, person, emailContentDoc, false);
    }
  }

  @Override
  public String normalizeEmail(String emailAdr) {
    return emailAdr.toLowerCase().trim();
  }

  String getClientInfo() {
    StringBuilder clientInfo = new StringBuilder();
    clientInfo.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    clientInfo.append(" @ ");
    clientInfo.append(getContext().getRequest().getHttpServletRequest().getHeader("user-agent"));
    return clientInfo.toString();
  }

  @Override
  public DocumentReference createParticipantDocRef(DocumentReference courseDocRef) {
    DocumentReference ret;
    SpaceReference spaceRef = getRegistrationSpace(Preconditions.checkNotNull(courseDocRef));
    String name = cfgSrc.getProperty(CFGSRC_PARTICIPANT_DOC_NAME_PREFIX, "");
    if (name.isEmpty()) {
      ret = nextFreeDoc.getNextUntitledPageDocRef(spaceRef);
    } else {
      ret = nextFreeDoc.getNextTitledPageDocRef(spaceRef, name);
    }
    return ret;
  }

  @Override
  public SpaceReference getRegistrationSpace(DocumentReference courseDocRef) {
    Preconditions.checkNotNull(courseDocRef);
    String spaceName = Joiner.on("_").join(courseDocRef.getParent().getName(),
        courseDocRef.getName());
    return new SpaceReference(spaceName, courseDocRef.getWikiReference());
  }

  @Override
  public String passwordHashString(String str) {
    return new PasswordClass().getEquivalentPassword("hash:SHA-512:", str);
  }

  @Override
  public boolean validateParticipant(DocumentReference regDocRef, String emailAdr,
      String activationCode) {
    boolean success = false;
    LOGGER.debug("validateParticipant: with regDoc [{}], email [{}] and activation code [{}]",
        regDocRef, emailAdr, activationCode);
    try {
      XWikiDocument regDoc = modelAccess.getDocument(regDocRef);
      Optional<BaseObject> partiObj = getParticipantObj(regDocRef, emailAdr, regDoc);
      if (partiObj.isPresent()) {
        success = "confirmed".equals(partiObj.get().getStringValue("status"));
        if (!success) {
          if (validateParticipant(activationCode, partiObj.get())) {
            modelAccess.saveDocument(regDoc, "validate email addresse by link.");
            sendValidationMail(partiObj.get());
            success = true;
          } else {
            LOGGER.debug("validateParticipant failed because activationCode does not match"
                + " object key. email [" + normalizeEmail(emailAdr) + "]");
          }
        } else {
          LOGGER.debug("validateParticipant was successful because status is confirmed");
        }
      } else {
        LOGGER.debug("validateParticipant failed because no partizipant object for" + " email ["
            + normalizeEmail(emailAdr) + "], on course [" + regDocRef + "] found.");
      }
    } catch (DocumentAccessException | XWikiException exp) {
      LOGGER.error("Failed to validateParticipant for [" + regDocRef + "], [" + emailAdr + "], ["
          + activationCode + "].", exp);
    }
    return success;
  }

  @Override
  public CourseConfirmState getConfirmeState(DocumentReference objDocRef) {
    DocumentReference courseParticipantClassRef = getCourseClasses().getCourseParticipantClassRef(
        modelContext.getXWikiContext().getDatabase());
    CourseConfirmState confirmState = CourseConfirmState.UNCONFIRMED;
    try {
      List<BaseObject> partiObjs = modelAccess.getXObjects(objDocRef, courseParticipantClassRef);
      int index = 0;
      for (BaseObject obj : partiObjs) {
        String state = obj.getStringValue("status");
        if ((state.equals(CourseConfirmState.CONFIRMED.id) && (index == 0)) || (state.equals(
            CourseConfirmState.CONFIRMED.id) && confirmState.equals(
                CourseConfirmState.CONFIRMED))) {
          confirmState = CourseConfirmState.CONFIRMED;
        } else if ((state.equals(CourseConfirmState.CONFIRMED.id) && !confirmState.equals(
            CourseConfirmState.CONFIRMED)) || (!state.equals(CourseConfirmState.CONFIRMED.id)
                && confirmState.equals(CourseConfirmState.CONFIRMED)) || (!state.equals(
                    CourseConfirmState.CONFIRMED.id) && confirmState.equals(
                        CourseConfirmState.PARTIALCONFIRMED))) {
          confirmState = CourseConfirmState.PARTIALCONFIRMED;
        } else {
          confirmState = CourseConfirmState.UNCONFIRMED;
        }
        index++;
      }
    } catch (DocumentNotExistsException exp) {
      LOGGER.info("Failed to get XObjects for docRef '{}' and classRef '{}'", objDocRef,
          courseParticipantClassRef);
    }
    return confirmState;
  }

  private boolean validateParticipant(String activationCode, BaseObject partiObj) {
    String hashedCode = passwordHashString(activationCode);
    String savedHash = partiObj.getStringValue("validkey");
    if (hashedCode.equals(savedHash)) {
      partiObj.setStringValue("status", "confirmed");
      return true;
    }
    return false;
  }

  private Optional<BaseObject> getParticipantObj(DocumentReference regDocRef, String emailAdr,
      XWikiDocument regDoc) {
    Optional<BaseObject> partiObj = Optional.fromNullable(modelAccess.getXObject(regDoc,
        getParticipantClassRef(), "email", normalizeEmail(emailAdr)));
    LOGGER.debug("validateParticipant courseDoc [{}] found participant: [{}]", regDocRef,
        partiObj.isPresent());
    return partiObj;
  }

  private boolean sendValidationMail(BaseObject partiObj) throws DocumentNotExistsException,
      XWikiException {
    getVeloContext().put("courseDocRef", modelUtils.resolveRef(partiObj.getStringValue("eventid"),
        DocumentReference.class));
    Person person = createPersonFromParticipant(partiObj);
    XWikiDocument emailContentDoc = modelAccess.getDocument(getValidationEmailDocRef());
    return sendMail(null, person, emailContentDoc, true);
  }

  DocumentReference getValidationEmailDocRef() {
    return new DocumentReference(modelContext.getWikiRef().getName(), "MailContent",
        "AnmeldungBestaetigt");
  }

  private Person createPersonFromParticipant(BaseObject bObj) {
    Person person = new Person();
    person.setTitle(bObj.getStringValue("title"));
    person.setGivenName(bObj.getStringValue("firstname"));
    person.setSurname(bObj.getStringValue("lastname"));
    person.setAddress(bObj.getStringValue("address"));
    person.setZip(bObj.getStringValue("zip"));
    person.setCity(bObj.getStringValue("city"));
    person.setPhone(bObj.getStringValue("phone"));
    person.setEmail(bObj.getStringValue("email"));
    person.setDateOfBirth(bObj.getDateValue("dob"));
    person.setStatus(bObj.getStringValue("status"));
    return person;
  }

  private boolean sendMail(String sender, Person person, XWikiDocument emailContentDoc,
      boolean sendToSender) throws XWikiException {
    boolean success = false;
    if (!Strings.nullToEmpty(person.getEmail()).trim().isEmpty()) {
      sender = MoreObjects.firstNonNull(Strings.emptyToNull(sender),
          new CelMailConfiguration().getDefaultAdminSenderAddress());
      getVeloContext().put("registrationPerson", person);
      String htmlContent = getRenderCommand().renderCelementsDocument(emailContentDoc, "view");
      String textContent = "-";
      try {
        textContent = new PlainTextCommand().convertHtmlToPlainText(htmlContent);
      } catch (ConvertToPlainTextException ctpte) {
        LOGGER.error("could not convert mail html content to plain text", ctpte);
      }
      success = mailSender.sendMail(sender, null, person.getEmail(), null, null,
          emailContentDoc.getTitle(), htmlContent, textContent, null, null) >= 0;
      if (sendToSender) {
        success = mailSender.sendMail(sender, null, sender, null, null, emailContentDoc.getTitle(),
            htmlContent, textContent, null, null) >= 0;
      }
    }
    return success;
  }

  RenderCommand injected_RenderCommand;

  private RenderCommand getRenderCommand() {
    if (injected_RenderCommand != null) {
      return injected_RenderCommand;
    }
    return new RenderCommand();
  }

  private VelocityContext getVeloContext() {
    return (VelocityContext) getContext().get("vcontext");
  }

  private DocumentReference getParticipantClassRef() {
    return getCourseClasses().getCourseParticipantClassRef(modelContext.getWikiRef().getName());
  }

  CourseClasses getCourseClasses() {
    return (CourseClasses) courseClasses;
  }

}
