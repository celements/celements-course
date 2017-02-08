package com.celements.course.service;

import java.text.SimpleDateFormat;
import java.util.Date;

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
import com.celements.model.access.exception.DocumentAlreadyExistsException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.celements.rendering.RenderCommand;
import com.celements.web.plugin.cmd.ConvertToPlainTextException;
import com.celements.web.plugin.cmd.PlainTextCommand;
import com.celements.web.service.IWebUtilsService;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiMessageTool;
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
          sendConfirmationMail(data);
        }
        return true;
      } catch (DocumentAlreadyExistsException | DocumentSaveException | XWikiException excp) {
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
    DocumentReference classRef = getCourseClasses().getCourseParticipantClassRef(
        modelContext.getWikiRef().getName());
    for (Person person : data.getPersons()) {
      if (!person.isEmpty() || (modelAccess.getXObjects(regDoc, classRef).size() == 0)) {
        BaseObject obj = modelAccess.newXObject(regDoc, classRef);
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
        if (obj.getNumber() == 0) {
          obj.setLargeStringValue("comment", data.getComment());
          // using set since there is no setPassword method
          obj.set("validkey", data.getValidationKey(), getContext());
          obj.setDateValue("timestamp", new Date());
          obj.setStringValue("client", getClientInfo());
        }
      }
    }
  }

  void sendConfirmationMail(RegistrationData data) throws XWikiException {
    VelocityContext vcontext = (VelocityContext) getContext().get("vcontext");
    vcontext.put("registrationData", data);
    String htmlContent = new RenderCommand().renderCelementsCell(new DocumentReference(
        modelContext.getWikiRef().getName(), "MailContent", "NeueAnmeldung"));
    String textContent = "-";
    try {
      textContent = new PlainTextCommand().convertHtmlToPlainText(htmlContent);
    } catch (ConvertToPlainTextException ctpte) {
      LOGGER.error("could not convert mail html content to plain text", ctpte);
    }
    XWikiMessageTool msgTool = webUtilsService.getMessageTool(getContext().getLanguage());
    // TODO CELDEV-357 (Event Registration Confirmation Mail from / replyTo addresses)
    // TODO add generic dictionary key to general dictionary
    // (event_reg_verification_mail_subject)
    // XXX from/replyTo should be read out from preferences when null
    mailSender.sendMail(null, null, data.getMainEmail(), null, null, msgTool.get(
        "event_reg_verification_mail_subject"), htmlContent, textContent, null, null);
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

  CourseClasses getCourseClasses() {
    return (CourseClasses) courseClasses;
  }

}
