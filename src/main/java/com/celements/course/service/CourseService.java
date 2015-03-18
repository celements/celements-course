package com.celements.course.service;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.classes.IClassCollectionRole;
import com.celements.course.classcollections.CourseClasses;
import com.celements.web.service.IWebUtilsService;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

@Component
public class CourseService implements ICourseServiceRole {

  @Requirement("CelCourseClasses")
  private IClassCollectionRole courseClasses;

  @Requirement
  IWebUtilsService webUtilsService;

  @Requirement
  private Execution execution;

  private XWikiContext getContext() {
    return (XWikiContext) execution.getContext().getProperty("xwikicontext");
  }

  @Override
  public DocumentReference getCourseTypeForCourse(DocumentReference courseDocRef
      ) throws XWikiException {
    DocumentReference typeDocRef = null;
    WikiReference wikiRef = webUtilsService.getWikiRef(courseDocRef);
    XWikiDocument courseDoc = getContext().getWiki().getDocument(courseDocRef, 
        getContext());
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
  public String getCourseTypeName(DocumentReference courseTypeDocRef
      ) throws XWikiException {
    String typeName = "";
    XWikiDocument typeDoc = getContext().getWiki().getDocument(courseTypeDocRef, 
        getContext());
    BaseObject typeObj = typeDoc.getXObject(getCourseClasses().getCourseTypeClassRef(
        webUtilsService.getWikiRef(courseTypeDocRef).getName()));
    if (typeObj != null) {
      typeName = typeObj.getStringValue("typeName");
    }
    return typeName;
  }

  CourseClasses getCourseClasses() {
    return (CourseClasses) courseClasses;
  }

}
