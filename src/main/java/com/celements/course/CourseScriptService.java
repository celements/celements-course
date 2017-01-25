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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.script.service.ScriptService;

import com.celements.common.classes.IClassCollectionRole;
import com.celements.course.classcollections.CourseClasses;
import com.celements.course.service.ICourseServiceRole;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.util.ModelUtils;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;

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
    return new PasswordClass().getEquivalentPassword("hash:SHA-512:", str);
  }

  public String normalizeEmail(String emailAdr) {
    return courseService.normalizeEmail(emailAdr);
  }

  public boolean validateParticipant(String courseFN, String emailAdr, String activationCode) {
    LOGGER.debug(
        "validateParticipant with registration doc [{}], email [{}] and activation code [{}]",
        courseFN, emailAdr, activationCode);
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(),
        CourseClasses.COURSE_CLASSES_SPACE, CourseClasses.COURSE_PARTICIPANT_CLASS_DOC);

    try {
      XWikiDocument courseDoc = modelAccess.getDocument(new DocumentReference(modelUtils.resolveRef(
          courseFN)));
      BaseObject partiObj = courseDoc.getXObject(partiClassRef, "email", normalizeEmail(emailAdr),
          false);
      LOGGER.debug("validateParticipant courseDoc [{}] found participant: [{}]", courseDoc,
          partiObj != null);
      if (partiObj != null) {
        String hashedCode = passwordHashString(activationCode);
        String savedHash = partiObj.getStringValue("validkey");
        LOGGER.trace("validateParticipant: email [" + normalizeEmail(emailAdr) + "], hashedCode ["
            + hashedCode + "], savedHash [" + savedHash + "].");
        if (hashedCode.equals(savedHash)) {
          if ("unconfirmed".equals(partiObj.getStringValue("status"))) {
            partiObj.setStringValue("status", "confirmed");
            modelAccess.saveDocument(courseDoc, "validate email addresse by" + " link.");
            return true;
          } else {
            LOGGER.debug("validateParticipant failed because initial status is not"
                + "'unconfirmed' but [" + partiObj.getStringValue("status") + "].");
          }
        } else {
          LOGGER.debug("validateParticipant failed because activationCode does not match"
              + " object key. email [" + normalizeEmail(emailAdr) + "], hashedCode [" + hashedCode
              + "], savedHash [" + savedHash + "]");
        }
      } else {
        LOGGER.debug("validateParticipant failed because no partizipant object for" + " email ["
            + normalizeEmail(emailAdr) + "], on course [" + courseFN + "] found.");
      }
    } catch (DocumentNotExistsException | DocumentSaveException exp) {
      LOGGER.error("Failed to validateParticipant for [" + courseFN + "], [" + emailAdr + "], ["
          + activationCode + "].", exp);
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

}
