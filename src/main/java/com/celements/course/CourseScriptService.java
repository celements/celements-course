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

  private static Log mLogger = LogFactory.getFactory().getInstance(
      CourseScriptService.class);

  @Requirement
  Execution execution;

  @Requirement
  EntityReferenceResolver<String> stringRefResolver;

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
        mLogger.trace("validateParticipant: email [" + normalizeEmail(emailAdr)
            + "], hashedCode [" + hashedCode + "], savedHash [" + savedHash + "].");
        if (hashedCode.equals(savedHash)) {
          if ("unconfirmed".equals(partiObj.getStringValue("status"))) {
            partiObj.setStringValue("status", "confirmed");
            getContext().getWiki().saveDocument(courseDoc, "validate email addresse by"
                + " link.", getContext());
            return true;
          } else {
            mLogger.debug("validateParticipant failed because initial status is not"
                + "'unconfirmed' but [" + partiObj.getStringValue("status") + "].");
          }
        } else {
          mLogger.debug("validateParticipant failed because activationCode does not match"
              + " object key. email [" + normalizeEmail(emailAdr) + "], hashedCode ["
          		+ hashedCode + "], savedHash [" + savedHash + "]");
        }
      } else {
        mLogger.debug("validateParticipant failed because no partizipant object for"
            + " email [" + normalizeEmail(emailAdr) + "], on course [" + courseFN
            + "] found.");
      }
    } catch (XWikiException exp) {
      mLogger.error("Failed to validateParticipant for [" + courseFN + "], [" + emailAdr
          + "], [" + activationCode + "].", exp);
    }
    return false;
  }

}
