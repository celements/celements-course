package com.celements.course.service;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.course.classcollections.CourseClasses;
import com.celements.course.registration.Person;
import com.celements.course.registration.RegistrationData;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentAlreadyExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.celements.web.service.IWebUtilsService;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiRequest;

@Component
public class CourseService implements ICourseServiceRole {

  private static Logger LOGGER = LoggerFactory.getLogger(CourseService.class);

  @Requirement("CelCourseClasses")
  private CourseClasses courseClasses;

  @Requirement
  IWebUtilsService webUtilsService;

  @Requirement
  IModelAccessFacade modelAccess;

  @Requirement
  ModelContext modelContext;

  @Requirement
  INextFreeDocRole nextFreeDoc;

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
  public boolean registerParticipantFromRequest(boolean sendValidationMail) {
    XWikiRequest req = getContext().getRequest();
    RegistrationData data = new RegistrationData();
    data.setData(req);
    try {
      XWikiDocument regDoc = modelAccess.createDocument(nextFreeDoc.getNextUntitledPageDocRef(
          getSpaceForEventId(data.getEventid())));
      DocumentReference classRef = courseClasses.getCourseParticipantClassRef(
          modelContext.getWikiRef().getName());
      String validationKey = data.getValidationKey();
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
          obj.setStringValue("email", person.getEmail());
          obj.setDateValue("dob", person.getDateOfBirth());
          obj.setStringValue("status", person.getStatus());
          if (obj.getNumber() == 0) {
            obj.setStringValue("validkey", validationKey);
            obj.setDateValue("timestamp", new Date());
            obj.setStringValue("client", getClientInfo());
          }
        }
      }
      modelAccess.saveDocument(regDoc, "created new registration");
      if (sendValidationMail) {
        // String mainEmail = data.getMainEmail();
        // validationKey
      }
      return true;
    } catch (DocumentAlreadyExistsException | DocumentSaveException excp) {
      LOGGER.error("exception while creating new registration", excp);
    }
    return false;
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
    return courseClasses;
  }

}
