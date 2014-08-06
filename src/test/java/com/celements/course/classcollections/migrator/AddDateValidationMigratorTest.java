package com.celements.course.classcollections.migrator;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractBridgedComponentTestCase;
import com.celements.migrations.celSubSystem.ICelementsMigrator;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.objects.classes.DateClass;
import com.xpn.xwiki.web.Utils;

public class AddDateValidationMigratorTest extends AbstractBridgedComponentTestCase {
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
    XWikiDocument docMock = createMockAndAddToDefault(XWikiDocument.class);
    DocumentReference docRef = new DocumentReference(context.getDatabase(),
        "CourseClasses", "CourseClass");
    expect(xwiki.getDocument(eq(docRef), same(context))).andReturn(docMock).once();
    
    BaseClass bClass = new BaseClass();
    bClass.addDateField("startTimeStamp", "Start Timestamp", "dd.MM.yyyy", 0);
    bClass.addDateField("endTimeStamp", "End Timestamp", "dd.MM.yyyy", 0);
    expect(docMock.getXClass()).andReturn(bClass).once();
    
//    DateClass startDateElement = new DateClass();
//    startDateElement.setValidationRegExp(
//        "/^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[012])\\.([0-9]{4})$/");
//    startDateElement.setValidationMessage("cel_course_validation_startTimeStamp");
////    expect(bClass.get("startTimeStamp")).andReturn(startDateElement).once();
//    
//    DateClass endDateElement   = new DateClass();
//    endDateElement.setValidationRegExp(
//        "/^(0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[012])\\.([0-9]{4})$/");
//    endDateElement.setValidationMessage("cel_course_validation_endTimeStamp");
//    expect(bClass.get("endTimeStamp")).andReturn(endDateElement).once();
    
    xwiki.saveDocument(docMock, context);
    expectLastCall();
    
    replayDefault();
    migrator.migrate(null, context);
    verifyDefault();
    
    assertTrue(((DateClass) bClass.get("startTimeStamp")).getValidationRegExp().length(
        ) > 0);
    assertTrue(((DateClass) bClass.get("endTimeStamp")).getValidationRegExp().length(
        ) > 0);
    
    assertEquals("cel_course_validation_startTimeStamp", ((DateClass) bClass.get("startTimeStamp")).getValidationMessage());
    assertEquals("cel_course_validation_endTimeStamp", ((DateClass) bClass.get("endTimeStamp")).getValidationMessage());
  }
}
