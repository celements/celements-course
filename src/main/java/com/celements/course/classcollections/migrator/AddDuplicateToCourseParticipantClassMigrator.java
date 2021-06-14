package com.celements.course.classcollections.migrator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.course.classes.CourseParticipantClass;
import com.celements.course.classes.CourseParticipantClass.ParticipantStatus;
import com.celements.migrations.SubSystemHibernateMigrationManager;
import com.celements.migrator.AbstractCelementsHibernateMigrator;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.context.ModelContext;
import com.google.common.base.Joiner;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.store.migration.XWikiDBVersion;

@Component("AddDuplicateToCourseParticipantClassMigrator")
public class AddDuplicateToCourseParticipantClassMigrator extends
    AbstractCelementsHibernateMigrator {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      AddDuplicateToCourseParticipantClassMigrator.class);

  @Requirement(CourseParticipantClass.CLASS_DEF_HINT)
  private ClassDefinition courseParticipantClass;

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement
  private ModelContext modelContext;

  @Override
  public String getName() {
    return "AddDuplicateToCourseParticipantClassMigrator";
  }

  @Override
  public String getDescription() {
    return "Add value duplicate to the field_status property";
  }

  /**
   * getVersion is using days since 1.1.2010 until the day of committing this
   * migration 27.03.2018 -> 3007
   * https://www.convertunits.com/dates/from/Jan+1,+2010/to/Mar+20,+2018
   */
  @Override
  public XWikiDBVersion getVersion() {
    return new XWikiDBVersion(3007);
  }

  @Override
  public void migrate(SubSystemHibernateMigrationManager manager, XWikiContext context)
      throws XWikiException {
    try {
      XWikiDocument doc = modelAccess.getDocument(courseParticipantClass.getDocRef());
      BaseClass bClass = doc.getXClass();
      ListClass status = (ListClass) bClass.get("status");
      status.setStringValue("values", Joiner.on("|").join(ParticipantStatus.values()));
      modelAccess.saveDocument(doc);
    } catch (DocumentNotExistsException exp) {
      LOGGER.debug("CourseClasses {} does not exist", courseParticipantClass.getDocRef(), exp);
    } catch (DocumentSaveException exp) {
      LOGGER.error("Document with docRef {} could not be saved", courseParticipantClass.getDocRef(),
          exp);
    }
  }
}
