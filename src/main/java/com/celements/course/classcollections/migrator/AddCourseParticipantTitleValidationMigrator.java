package com.celements.course.classcollections.migrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.StringClass;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component("AddCourseParticipantTitleValidationMigrator")
public class AddCourseParticipantTitleValidationMigrator extends
    AbstractCelementsHibernateMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      AddCourseParticipantTitleValidationMigrator.class);

  @Requirement
  private QueryManager queryManager;

  @Requirement
  private IWebUtilsService webUtilsService;

  @Override
  public String getName() {
    return "AddCourseParticipantTitleValidationMigrator";
  }

  @Override
  public String getDescription() {
    return "Add a RegExp to the Title in CourseParticipantClass";
  }

  /**
   * getVersion is using days since 1.1.2010 until the day of committing this
   * migration 03.01.2018 -> 2924
   * https://www.convertunits.com/dates/from/Jan+1,+2010/to/Jan+3,+2018
   */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(2924);
  }

  @Override
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext context)
      throws XWikiException {
    XWikiDocument doc = context.getWiki().getDocument(new DocumentReference(context.getDatabase(),
        "CourseClasses", "CourseParticipantClass"), context);
    if (context.getWiki().exists(doc.getDocumentReference(), context)) {
      BaseClass bClass = doc.getXClass();
      StringClass startDateElement = (StringClass) bClass.get("title");
      startDateElement.setValidationRegExp("/^.{0,8}$/");
      startDateElement.setValidationMessage("cel_course_validation_titleToLong");
      context.getWiki().saveDocument(doc, context);
    }
  }
}
