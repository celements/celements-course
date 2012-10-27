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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;

@Component("celcourse")
public class CourseScriptService implements ScriptService {

  private static Log LOGGER = LogFactory.getFactory().getInstance(
      CourseScriptService.class);

  @Requirement
  EntityReferenceResolver<String> stringRefResolver;

  @Requirement
  Execution execution;

  private XWikiContext getContext() {
    return (XWikiContext)execution.getContext().getProperty("xwikicontext");
  }

  DocumentReference getDocRefForFullName(String courseFN) {
    DocumentReference courseDocRef = new DocumentReference(stringRefResolver.resolve(
        courseFN, EntityType.DOCUMENT));
    courseDocRef.setWikiReference(new WikiReference(getContext().getDatabase()));
    return courseDocRef;
  }

  public String passwordHashString(String str) {
    return new PasswordClass().getEquivalentPassword("hash:SHA-512:", str);
  }

  public String normalizeEmail(String emailAdr) {
    return emailAdr.toLowerCase().trim();
  }

  public boolean validateParticipant(String courseFN, String emailAdr,
      String activationCode) {
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(), 
        "Classes", "CourseParticipantClass");

    try {
      XWikiDocument courseDoc = getContext().getWiki().getDocument(getDocRefForFullName(
          courseFN), getContext());
      BaseObject partiObj = courseDoc.getXObject(partiClassRef, "email",
          normalizeEmail(emailAdr), false);
      if (partiObj != null) {
        String hashedCode = passwordHashString(activationCode);
        String savedHash = partiObj.getStringValue("validkey");
        LOGGER.trace("validateParticipant: email [" + normalizeEmail(emailAdr)
            + "], hashedCode [" + hashedCode + "], savedHash [" + savedHash + "].");
        if (hashedCode.equals(savedHash)) {
          if ("unconfirmed".equals(partiObj.getStringValue("status"))) {
            partiObj.setStringValue("status", "confirmed");
            getContext().getWiki().saveDocument(courseDoc, "validate email addresse by"
                + " link.", getContext());
            return true;
          } else {
            LOGGER.debug("validateParticipant failed because initial status is not"
                + "'unconfirmed' but [" + partiObj.getStringValue("status") + "].");
          }
        } else {
          LOGGER.debug("validateParticipant failed because activationCode does not match"
              + " object key. email [" + normalizeEmail(emailAdr) + "], hashedCode ["
          		+ hashedCode + "], savedHash [" + savedHash + "]");
        }
      } else {
        LOGGER.debug("validateParticipant failed because no partizipant object for"
            + " email [" + normalizeEmail(emailAdr) + "], on course [" + courseFN
            + "] found.");
      }
    } catch (XWikiException exp) {
      LOGGER.error("Failed to validateParticipant for [" + courseFN + "], [" + emailAdr
          + "], [" + activationCode + "].", exp);
    }
    return false;
  }

}
