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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.script.service.ScriptService;

import com.celements.common.classes.IClassCollectionRole;
import com.celements.course.classcollections.CourseClasses;
import com.celements.course.service.CourseConfirmState;
import com.celements.course.service.ICourseServiceRole;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.util.ModelUtils;
import com.celements.search.lucene.query.LuceneQuery;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.objects.BaseObject;

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
  private ModelUtils modelUtils;

  @Requirement
  Execution execution;

  private XWikiContext getContext() {
    return (XWikiContext) execution.getContext().getProperty("xwikicontext");
  }

  private CourseClasses getCourseClasses() {
    return (CourseClasses) courseClasses;
  }

  public DocumentReference getCourseClassRef() {
    return getCourseClasses().getCourseClassRef(getContext().getDatabase());
  }

  public String getCourseClass() {
    return CourseClasses.COURSE_CLASSES_SPACE + "." + CourseClasses.COURSE_CLASS_DOC;
  }

  public DocumentReference getCourseParticipantClassRef() {
    return getCourseClasses().getCourseParticipantClassRef(getContext().getDatabase());
  }

  public String getCourseParticipantClass() {
    return CourseClasses.COURSE_CLASSES_SPACE + "." + CourseClasses.COURSE_PARTICIPANT_CLASS_DOC;
  }

  public DocumentReference getCourseTypeClassRef() {
    return getCourseClasses().getCourseTypeClassRef(getContext().getDatabase());
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
    if (!Strings.isNullOrEmpty(regFN)) {
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
  public boolean registerParticipantFromRequest(boolean sendConfirmationMail) {
    return courseService.registerParticipantFromRequest(sendConfirmationMail);
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
  public CourseConfirmState getConfirmeState(@Nullable DocumentReference regDocRef) {
    if (regDocRef != null) {
      return courseService.getConfirmState(regDocRef);
    }
    return CourseConfirmState.UNDEFINED;
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

  public Map<String, Integer> getNumberCourseAnnouncement(LuceneQuery query,
      List<String> sortFields) {
    List<EntityReference> announcementList = courseService.getAnnouncementsForCourse(query,
        sortFields);
    DocumentReference partiClassRef = getCourseParticipantClassRef();
    Integer totalAnnouncements = 0;
    Integer confirmAnnouncements = 0;
    for (EntityReference announcement : announcementList) {
      DocumentReference announcementDocRef = new DocumentReference(announcement);
      try {
        List<BaseObject> partiObjs = modelAccess.getXObjects(announcementDocRef, partiClassRef);
        for (BaseObject obj : partiObjs) {
          Optional<CourseConfirmState> state = CourseConfirmState.convertStringToEnum(
              obj.getStringValue("status"));
          if (state.isPresent() && (state.get() == CourseConfirmState.CONFIRMED)) {
            confirmAnnouncements++;
          }
        }
        totalAnnouncements += partiObjs.size();

      } catch (DocumentNotExistsException exp) {
        LOGGER.info("Failed to get XObjects for announcementDocRef {} and partiClassRef '{}'",
            announcementDocRef, partiClassRef, exp);
      }
    }
    Map<String, Integer> retMap = new HashMap<>();
    retMap.put("totalAnnouncement", totalAnnouncements);
    retMap.put("confirmAnnouncements", confirmAnnouncements);
    return retMap;
  }

}
