package com.celements.course.service;

import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.search.lucene.LuceneSearchException;

@ComponentRole
public interface ICourseServiceRole {

  @Nullable
  public DocumentReference getCourseTypeForCourse(@NotNull DocumentReference courseDocRef);

  public String getCourseTypeName(DocumentReference courseTypeDocRef);

  public boolean registerParticipantFromRequest(boolean sendConfirmationMail);

  public @NotNull DocumentReference createParticipantDocRef(
      @NotNull DocumentReference courseDocRef);

  public @NotNull SpaceReference getRegistrationSpace(@NotNull DocumentReference courseDocRef);

  public String normalizeEmail(String emailAdr);

  public String passwordHashString(String str);

  public boolean validateParticipant(DocumentReference regDocRef, String emailAdr,
      String activationCode);

  @NotNull
  public CourseConfirmState getConfirmState(@NotNull DocumentReference regDocRef);

  @NotNull
  public List<DocumentReference> getAnnouncementsForCourse(@NotNull SpaceReference regSpaceRef,
      @Nullable List<String> sortFields) throws LuceneSearchException;

  public long getRegistrationCount(@NotNull DocumentReference courseDocRef)
      throws LuceneSearchException;

  public long getRegistrationCount(@NotNull DocumentReference courseDocRef,
      @Nullable CourseConfirmState state) throws LuceneSearchException;

}
