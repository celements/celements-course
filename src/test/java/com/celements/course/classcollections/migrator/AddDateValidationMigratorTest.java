package com.celements.course.classcollections.migrator;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.web.Utils;

public class AddDateValidationMigratorTest extends AbstractComponentTest {
  private XWikiContext context;
  private XWiki xwiki;
  
  private AddDateValidationMigrator migrator;
  
  @Before
  public void setUp_DocumentMetaDataMigratorTest() {
    context = getContext();
    xwiki = getWikiMock();
    context.setWiki(xwiki);
    migrator = (AddDateValidationMigrator) Utils.getComponent(ICelementsMigrator.class,
        "AddDateValidationMigrator");
  }
  
  @Test
  public void testGetName() {
    assertEquals("AddDateValidationMigrator", migrator.getName());
  }
  
  @Test
  public void testMigrate() throws Exception {
    XWikiDocument docMock = createDefaultMock(XWikiDocument.class);
    DocumentReference docRef = new DocumentReference(context.getDatabase(),
        "CourseClasses", "CourseClass");
    expect(xwiki.getDocument(eq(docRef), same(context))).andReturn(docMock).once();
    
    BaseClass bClass = new BaseClass();
    bClass.addDateField("startTimeStamp", "Start Timestamp", "dd.MM.yyyy", 0);
    bClass.addDateField("endTimeStamp", "End Timestamp", "dd.MM.yyyy", 0);
    expect(docMock.getXClass()).andReturn(bClass).once();
    
    xwiki.saveDocument(docMock, context);
    expectLastCall();
    
    replayDefault();
    migrator.migrate(null, context);
    verifyDefault();
    
    assertTrue(((DateClass) bClass.get("startTimeStamp")).getValidationRegExp().length(
        ) > 0);
    assertTrue(((DateClass) bClass.get("endTimeStamp")).getValidationRegExp().length(
        ) > 0);
    assertEquals("cel_course_validation_startTimeStamp", ((DateClass) bClass.get(
        "startTimeStamp")).getValidationMessage());
    assertEquals("cel_course_validation_endTimeStamp", ((DateClass) bClass.get(
        "endTimeStamp")).getValidationMessage());
  }
}
