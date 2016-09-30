package com.celements.course.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
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
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.celements.rendering.RenderCommand;
import com.celements.web.plugin.cmd.ConvertToPlainTextException;
import com.celements.web.plugin.cmd.PlainTextCommand;
import com.celements.web.service.IWebUtilsService;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiMessageTool;
import com.xpn.xwiki.web.XWikiRequest;

@Component
public class CourseService implements ICourseServiceRole {

  private static Logger LOGGER = LoggerFactory.getLogger(CourseService.class);

  @Requirement("CelCourseClasses")
  private IClassCollectionRole courseClasses;

  @Requirement
  IWebUtilsService webUtilsService;

  @Requirement
  IModelAccessFacade modelAccess;

  @Requirement
  ModelContext modelContext;

  @Requirement
  ModelConfiguration modelConfig;

  @Requirement
  INextFreeDocRole nextFreeDoc;

  @Requirement
  IMailSenderRole mailSender;

  @Requirement
  private Execution execution;

  private XWikiContext getContext() {
    return (XWikiContext) execution.getContext().getProperty("xwikicontext");
  }

  @Override
  public DocumentReference getCourseTypeForCourse(DocumentReference courseDocRef)
      throws XWikiException {
    DocumentReference typeDocRef = null;
    WikiReference wikiRef = webUtilsService.getWikiRef(courseDocRef);
    XWikiDocument courseDoc = getContext().getWiki().getDocument(courseDocRef, getContext());
    BaseObject courseObj = courseDoc.getXObject(getCourseClasses().getCourseClassRef(
        wikiRef.getName()));
    if (courseObj != null) {
      String typeFN = courseObj.getStringValue("type");
      if (StringUtils.isNotBlank(typeFN)) {
        typeDocRef = webUtilsService.resolveDocumentReference(typeFN, wikiRef);
      }
    }
    return typeDocRef;
  }

  @Override
  public String getCourseTypeName(DocumentReference courseTypeDocRef) throws XWikiException {
    String typeName = "";
    XWikiDocument typeDoc = getContext().getWiki().getDocument(courseTypeDocRef, getContext());
    BaseObject typeObj = typeDoc.getXObject(getCourseClasses().getCourseTypeClassRef(
        webUtilsService.getWikiRef(courseTypeDocRef).getName()));
    if (typeObj != null) {
      typeName = typeObj.getStringValue("typeName");
    }
    return typeName;
  }

  @Override
  public boolean registerParticipantFromRequest(boolean sendConfirmationMail) {
    XWikiRequest req = getContext().getRequest();
    RegistrationData data = new RegistrationData();
    data.setData(req);
    data.setRegDocRef(nextFreeDoc.getNextUntitledPageDocRef(getSpaceForEventId(data.getEventid())));
    try {
      XWikiDocument regDoc = modelAccess.createDocument(data.getRegDocRef());
      setRegDocRights(regDoc);
      DocumentReference classRef = getCourseClasses().getCourseParticipantClassRef(
          modelContext.getWikiRef().getName());
      for (Person person : data.getPersons()) {
        if (!person.isEmpty() || (modelAccess.getXObjects(regDoc, classRef).size() == 0)) {
          BaseObject obj = modelAccess.newXObject(regDoc, classRef);
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
            obj.setStringValue("validkey", data.getValidationKey());
            obj.setDateValue("timestamp", new Date());
            obj.setStringValue("client", getClientInfo());
          }
        }
      }
      modelAccess.saveDocument(regDoc, "created new registration");
      if (sendConfirmationMail) {
        sendConfirmationMail(data);
      }
      return true;
    } catch (DocumentAlreadyExistsException | DocumentSaveException | XWikiException excp) {
      LOGGER.error("exception while creating new registration", excp);
    }
    return false;
  }

  void setRegDocRights(XWikiDocument regDoc) {
    // if(regDoc.isNew() && modelAccess.exists(regDoc.)) {
    // TODO
    // }
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
    // TODO get correct from / replyTo addresses
    // TODO add generic dictionary key to general dictionary
    // (event_reg_verification_mail_subject)
    mailSender.sendMail("reg@bellis.cel.sneakapeek.ch", "reg@bellis.cel.sneakapeek.ch",
        data.getMainEmail(), null, null, msgTool.get("event_reg_verification_mail_subject"),
        htmlContent, textContent, null, null);
  }

  @Override
  public String normalizeEmail(String emailAdr) {
    return emailAdr.toLowerCase().trim();
  }

  String getClientInfo() {
    String clientInfo = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    clientInfo += " @ " + getContext().getRequest().getHttpServletRequest().getHeader("user-agent");
    return clientInfo;
  }

  SpaceReference getSpaceForEventId(String eventid) {
    return new SpaceReference(eventid.replaceAll("\\.", "_").replaceAll(":", "_"),
        modelContext.getWikiRef());
  }

  CourseClasses getCourseClasses() {
    return (CourseClasses) courseClasses;
  }

}
