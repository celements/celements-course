package com.celements.course.service;

import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

@ComponentRole
public interface ICourseServiceRole {

  public DocumentReference getCourseTypeForCourse(DocumentReference courseDocRef);

  public String getCourseTypeName(DocumentReference courseTypeDocRef);

  public boolean registerParticipantFromRequest(boolean sendConfirmationMail);

  public @NotNull DocumentReference createParticipantDocRef(
      @NotNull DocumentReference courseDocRef);

  public @NotNull SpaceReference getRegistrationSpace(@NotNull DocumentReference courseDocRef);

  public String normalizeEmail(String emailAdr);

  public String passwordHashString(String str);

  public boolean validateParticipant(DocumentReference regDocRef, String emailAdr,
      String activationCode);

  public CourseConfirmState getConfirmeState(DocumentReference objDocRef);

}
