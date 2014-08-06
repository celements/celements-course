package com.celements.course.classcollections.migrator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.query.QueryManager;

import com.celements.migrations.SubSystemHibernateMigrationManager;
import com.celements.migrator.AbstractCelementsHibernateMigrator;
import com.celements.web.service.IWebUtilsService;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component("AddDateValidationMigrator")
public class AddDateValidationMigrator extends AbstractCelementsHibernateMigrator {

  private static final Log LOGGER = LogFactory.getFactory().getInstance(
      AddDateValidationMigrator.class);
  
  @Requirement
  private QueryManager queryManager;
  
  @Requirement
  private IWebUtilsService webUtilsService;

  public String getName() {
    return "AddDateValidationMigrator";
  }

  public String getDescription() {
    return "Add a RegExp to the Datefields";
  }

  /**
   * getVersion is using days since 1.1.2010 until the day of committing this
   * migration 27.5.2014 -> 1607
   * http://www.convertunits.com/dates/from/Jan+1,+2010/to/Jul+21,+2014
   */
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(1678);
  }

  @Override
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext context)
      throws XWikiException {
    XWikiDocument doc = context.getWiki().getDocument(new DocumentReference(
        context.getDatabase(), "CourseClasses", "CourseClass"), context);
    BaseClass bClass = doc.getXClass();
    DateClass startDateElement = (DateClass) bClass.get("startTimeStamp");
    DateClass endDateElement = (DateClass) bClass.get("endTimeStamp");
    startDateElement.setValidationRegExp(
        "/^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[012])\\.([0-9]{4})$/");
    startDateElement.setValidationMessage("cel_course_validation_startTimeStamp");
    endDateElement.setValidationRegExp(
        "/^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[012])\\.([0-9]{4})$/");
    endDateElement.setValidationMessage("cel_course_validation_endTimeStamp");
    context.getWiki().saveDocument(doc, context);
  }
}