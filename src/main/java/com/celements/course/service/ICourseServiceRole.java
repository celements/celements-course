package com.celements.course.service;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.xpn.xwiki.XWikiException;

@ComponentRole
public interface ICourseServiceRole {

  public DocumentReference getCourseTypeForCourse(DocumentReference courseDocRef)
      throws XWikiException;

  public String getCourseTypeName(DocumentReference courseTypeDocRef) throws XWikiException;

  public boolean registerParticipantFromRequest(boolean sendConfirmationMail);

  public @NotNull DocumentReference createParticipantDocRef(
      @NotNull DocumentReference courseDocRef);

  public @NotNull SpaceReference getRegistrationSpace(@NotNull DocumentReference courseDocRef);

  public String normalizeEmail(String emailAdr);
}
