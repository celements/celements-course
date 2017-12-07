/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package com.celements.course;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;

import com.celements.common.classes.IClassCollectionRole;
import com.celements.course.classcollections.CourseClasses;
import com.celements.course.classes.CourseParticipantClass.ParticipantStatus;
import com.celements.course.service.ICourseServiceRole;
import com.celements.course.service.RegistrationState;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.context.ModelContext;
import com.celements.model.util.ModelUtils;
import com.celements.rights.access.EAccessLevel;
import com.celements.rights.access.IRightsAccessFacadeRole;
import com.celements.search.lucene.LuceneSearchException;
import com.google.common.base.Optional;
import com.google.common.base.Strings;

@Component("celcourse")
public class CourseScriptService implements ScriptService {

  private static Logger LOGGER = LoggerFactory.getLogger(CourseScriptService.class);

  @Requirement
  private ICourseServiceRole courseService;

  @Requirement
  EntityReferenceResolver<String> stringRefResolver;

  @Requirement("CelCourseClasses")
  IClassCollectionRole courseClasses;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private IRightsAccessFacadeRole rightsService;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private ModelContext modelContext;

  private CourseClasses getCourseClasses() {
    return (CourseClasses) courseClasses;
  }

  public DocumentReference getCourseClassRef() {
    return getCourseClasses().getCourseClassRef(modelContext.getWikiRef().getName());
  }

  public String getCourseClass() {
    return CourseClasses.COURSE_CLASSES_SPACE + "." + CourseClasses.COURSE_CLASS_DOC;
  }

  public DocumentReference getCourseParticipantClassRef() {
    return getCourseClasses().getCourseParticipantClassRef(modelContext.getWikiRef().getName());
  }

  public String getCourseParticipantClass() {
    return CourseClasses.COURSE_CLASSES_SPACE + "." + CourseClasses.COURSE_PARTICIPANT_CLASS_DOC;
  }

  public DocumentReference getCourseTypeClassRef() {
    return getCourseClasses().getCourseTypeClassRef(modelContext.getWikiRef().getName());
  }

  public String getCourseTypeClass() {
    return CourseClasses.COURSE_CLASSES_SPACE + "." + CourseClasses.COURSE_TYPE_CLASS_DOC;
  }

  public String passwordHashString(String str) {
    return courseService.passwordHashString(str);
  }

  public String normalizeEmail(String emailAdr) {
    return courseService.normalizeEmail(emailAdr);
  }

  public boolean validateParticipant(String regFN, String emailAdr, String activationCode) {
    if (!Strings.isNullOrEmpty(regFN) && !Strings.isNullOrEmpty(emailAdr)) {
      return courseService.validateParticipant(modelUtils.resolveRef(regFN,
          DocumentReference.class), emailAdr, activationCode);
    }
    return false;
  }

  /**
   * Required fields: eventid, surname[], email[] <br />
   * Optional fields: givenName[], addressEqualsMain[], street[], zip[], city[], phone[],
   * comment
   *
   * @return true if registration was successful
   */
  public boolean registerParticipantFromRequest(boolean sendValidationMail) {
    return courseService.registerParticipantFromRequest(sendValidationMail);
  }

  public DocumentReference createParticipantDocRef(DocumentReference courseDocRef) {
    if (courseDocRef != null) {
      return courseService.createParticipantDocRef(courseDocRef);
    }
    return null;
  }

  public SpaceReference getRegistrationSpace(DocumentReference courseDocRef) {
    if (courseDocRef != null) {
      return courseService.getRegistrationSpace(courseDocRef);
    }
    return null;
  }

  @NotNull
  public RegistrationState getConfirmeState(@Nullable DocumentReference regDocRef) {
    if (regDocRef != null) {
      return courseService.getConfirmState(regDocRef);
    }
    return RegistrationState.UNDEFINED;
  }

  /**
   * @param docRef
   *          course or course type docRef
   * @return the course type name
   */
  public String getCourseTypeName(DocumentReference docRef) {
    if (docRef != null) {
      DocumentReference courseTypeDocRef = Optional.fromNullable(
          courseService.getCourseTypeForCourse(docRef)).or(docRef);
      return courseService.getCourseTypeName(courseTypeDocRef);
    }
    return "";
  }

  public long getRegistrationCount(DocumentReference courseDocRef) {
    long retVal = 0;
    try {
      retVal = courseService.getRegistrationCount(courseDocRef);
    } catch (LuceneSearchException exp) {
      LOGGER.info("Failed to get Results for courseDocRef '{}'", courseDocRef, exp);
    }
    return retVal;
  }

  public long getRegistrationCount(DocumentReference courseDocRef, ParticipantStatus state) {
    long retVal = 0;
    try {
      retVal = courseService.getRegistrationCount(courseDocRef, state);
    } catch (LuceneSearchException exp) {
      LOGGER.info("Failed to get Results for courseDocRef '{}' and state '{}'", courseDocRef, state,
          exp);
    }
    return retVal;
  }

  public List<DocumentReference> getRegistrationsForCourse(DocumentReference courseDocRef,
      List<String> sortFields) {
    List<DocumentReference> retVal = new ArrayList<>();
    try {
      retVal = courseService.getRegistrationsForCourse(getRegistrationSpace(courseDocRef),
          sortFields);
    } catch (LuceneSearchException exp) {
      LOGGER.info("Failed to get Results for courseDocRef '{}' and sortFields '{}'", courseDocRef,
          sortFields, exp);
    }
    return retVal;
  }

  public boolean sendConfirmationMail(DocumentReference regDocRef, int participantObjNb) {
    if ((regDocRef != null) && rightsService.hasAccessLevel(regDocRef, EAccessLevel.EDIT)) {
      return courseService.sendConfirmationMail(regDocRef, participantObjNb);
    }
    return false;
  }

  public ParticipantStatus getCourseConfirmState(String state) {
    return ParticipantStatus.valueOf(state);
  }

}
