package com.celements.course.service;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiException;

@ComponentRole
public interface ICourseServiceRole {

  public DocumentReference getCourseTypeForCourse(DocumentReference courseDocRef
      ) throws XWikiException;

  public String getCourseTypeName(DocumentReference courseTypeDocRef
      ) throws XWikiException;

}
