package com.celements.course.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ImmutableObjectReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.auth.user.User;
import com.celements.course.classes.CourseParticipantClass.Attendance;
import com.celements.course.classes.CourseParticipantClass.ParticipantStatus;
import com.celements.search.lucene.LuceneSearchException;

@ComponentRole
public interface ICourseServiceRole {

  @Nullable
  DocumentReference getCourseTypeForCourse(@NotNull DocumentReference courseDocRef);

  String getCourseTypeName(DocumentReference courseTypeDocRef);

  boolean registerParticipantFromRequest(boolean sendValidationMail);

  @NotNull
  DocumentReference createParticipantDocRef(@NotNull DocumentReference courseDocRef);

  @NotNull
  DocumentReference createParticipantDocRef(@NotNull DocumentReference courseDocRef,
      @NotNull User user);

  @NotNull
  SpaceReference getRegistrationSpace(@NotNull DocumentReference courseDocRef);

  boolean prepareRegistrationSpace(@NotNull SpaceReference regSpaceRef,
      @NotNull ClassReference... additionalGroups);

  String normalizeEmail(String emailAdr);

  String passwordHashString(String str);

  boolean validateParticipant(DocumentReference regDocRef, String emailAdr, String activationCode);

  @NotNull
  RegistrationState getConfirmState(@NotNull DocumentReference regDocRef);

  @NotNull
  List<DocumentReference> getRegistrationsForCourse(@NotNull SpaceReference regSpaceRef,
      @Nullable List<String> sortFields) throws LuceneSearchException;

  @NotNull
  List<DocumentReference> getRegistrationsForCourse(@NotNull DocumentReference courseDocRef)
      throws LuceneSearchException;

  long getRegistrationCount(@NotNull DocumentReference courseDocRef) throws LuceneSearchException;

  long getRegistrationCount(@NotNull DocumentReference courseDocRef,
      @Nullable ParticipantStatus state) throws LuceneSearchException;

  long getRegistrationCount(DocumentReference courseDocRef,
      List<ParticipantStatus> ignoreParticipantStates) throws LuceneSearchException;

  @NotNull
  Optional<Attendance> getAttendance(@NotNull DocumentReference participantDocRef);

  /**
   * copy all participant objects defined by objRefs to new registration docs for the given
   * targetCourseDocRef.
   *
   * @return list of (unsaved) registration docs
   */
  @NotNull
  List<ImmutableObjectReference> copyParticipants(@NotNull DocumentReference targetCourseDocRef,
      @NotNull Stream<ImmutableObjectReference> objRefs);

  boolean sendConfirmationMail(@NotNull DocumentReference regDocRef, int participantObjNb);

  boolean setStatusConfirmedFromUnconfirmed(@NotNull DocumentReference regDocRef,
      int participantObjNb);

}
