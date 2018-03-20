package com.celements.course.classcollections.migrator;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.celements.model.access.IModelAccessFacade;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.ListClass;
import com.xpn.xwiki.web.Utils;

public class AddDuplicateToCourseParticipantClassMigratorTest extends AbstractComponentTest {

  private XWikiContext context;
  private XWiki xwiki;
  private IModelAccessFacade modelAccess;

  private AddDuplicateToCourseParticipantClassMigrator migrator;

  @Before
  public void setUp_DocumentMetaDataMigratorTest() throws Exception {
    modelAccess = registerComponentMock(IModelAccessFacade.class);
    context = getContext();
    xwiki = getWikiMock();
    context.setWiki(xwiki);
    migrator = (AddDuplicateToCourseParticipantClassMigrator) Utils.getComponent(
        ICelementsMigrator.class, "AddDuplicateToCourseParticipantClassMigrator");
  }

  @Test
  public void testGetName() {
    assertEquals("AddDuplicateToCourseParticipantClassMigrator", migrator.getName());
  }

  @Test
  public void testMigrate() throws Exception {
    XWikiDocument docMock = createMockAndAddToDefault(XWikiDocument.class);
    DocumentReference docRef = new DocumentReference(context.getDatabase(), "CourseClasses",
        "CourseParticipantClass");
    expect(modelAccess.getDocument(eq(docRef))).andReturn(docMock);
    expect(docMock.getDocumentReference()).andReturn(docRef);
    expect(xwiki.exists(eq(docRef), same(context))).andReturn(true);

    BaseClass bClass = new BaseClass();
    bClass.addStaticListField("status", "Status", "unconfirmed|confirmed|cancelled");

    expect(docMock.getXClass()).andReturn(bClass).once();
    modelAccess.saveDocument(docMock);
    expectLastCall();

    replayDefault();
    migrator.migrate(null, context);
    verifyDefault();

    ListClass status = (ListClass) bClass.get("status");
    assertEquals("unconfirmed|confirmed|cancelled|duplicate", status.getStringValue("values"));
  }
}
