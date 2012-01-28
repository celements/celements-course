package com.celements.course;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;

import com.celements.common.test.AbstractBridgedComponentTestCase;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class CourseScriptServiceTest extends AbstractBridgedComponentTestCase {

  private static final String ACTIVATION_CODE = "123ho123jjk5689";
  private static final String ACTIVATION_HASH = "hash:SHA-512:1a572fd8d66a08e922a0168671"
    + "fe28cd04be6b84e2fe956f3fff2d0353621108bc0834f2676fd7415542d8759e6a888a4fed9dce738"
    + "dd8c7cc0f89ab3fbce394";

  private CourseScriptService courseScriptService;
  private XWikiContext context;
  private XWiki xwiki;
  private EntityReferenceResolver<String> stringRefResolverMock;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp_CourseScriptServiceTest() throws Exception {
    context = getContext();
    xwiki = createMock(XWiki.class);
    context.setWiki(xwiki);
    courseScriptService = new CourseScriptService();
    courseScriptService.execution = getComponentManager().lookup(Execution.class);
    stringRefResolverMock = createMock(EntityReferenceResolver.class);
    courseScriptService.stringRefResolver = stringRefResolverMock;
  }

  @Test
  public void testNormalizeEmail() {
    assertEquals("fabian.pichler@synventis.com", courseScriptService.normalizeEmail(
        " Fabian.Pichler@Synventis.com "));
  }

  @Test
  public void testPasswordHashString() {
    assertEquals(ACTIVATION_HASH, courseScriptService.passwordHashString(ACTIVATION_CODE)
        );
  }

  @Test
  public void testGetDocRefForFullName() {
    DocumentReference courseDocRefNew = new DocumentReference("none", "mySpace", "MyDoc");
    expect(stringRefResolverMock.resolve(eq("mySpace.MyDoc"), eq(EntityType.DOCUMENT))
        ).andReturn(courseDocRefNew);
    DocumentReference courseDocRef = new DocumentReference(context.getDatabase(),
        "mySpace", "MyDoc");
    replayAll();
    assertEquals(courseDocRef, courseScriptService.getDocRefForFullName("mySpace.MyDoc"));
    verifyAll();
  }

  @Test
  public void testValidateParticipant_no_object() throws Exception {
    DocumentReference courseDocRefNew = new DocumentReference("none", "Kurse", "Kurs2");
    expect(stringRefResolverMock.resolve(eq("Kurse.Kurs2"), eq(EntityType.DOCUMENT))
        ).andReturn(courseDocRefNew);
    DocumentReference courseDocRef = new DocumentReference(context.getDatabase(), "Kurse",
        "Kurs2");
    XWikiDocument courseDoc = new XWikiDocument(courseDocRef);
    expect(xwiki.getDocument(eq(courseDocRef), same(context))).andReturn(courseDoc);
    expectLastCall().once();
    replayAll();
    assertFalse(courseScriptService.validateParticipant("Kurse.Kurs2", "test@test.com",
        ACTIVATION_CODE));
    verifyAll();
  }

  @Test
  public void testValidateParticipant_wrong_initial_status() throws Exception {
    DocumentReference courseDocRefNew = new DocumentReference("none", "Kurse", "Kurs2");
    expect(stringRefResolverMock.resolve(eq("Kurse.Kurs2"), eq(EntityType.DOCUMENT))
        ).andReturn(courseDocRefNew);
    DocumentReference courseDocRef = new DocumentReference(context.getDatabase(), "Kurse",
        "Kurs2");
    XWikiDocument courseDoc = new XWikiDocument(courseDocRef);
    expect(xwiki.getDocument(eq(courseDocRef), same(context))).andReturn(courseDoc);
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(), 
        "Classes", "CourseParticipantClass");
    BaseObject partiObj = new BaseObject();
    String emailAdr = "test@test.com";
    partiObj.setXClassReference(partiClassRef);
    partiObj.setStringValue("email", emailAdr);
    partiObj.setStringValue("validkey", ACTIVATION_HASH);
    partiObj.setStringValue("status", "some different status");
    courseDoc.setXObject(0, partiObj);
    replayAll();
    assertFalse(courseScriptService.validateParticipant("Kurse.Kurs2", emailAdr,
        ACTIVATION_CODE));
    assertEquals("some different status", partiObj.getStringValue("status"));
    verifyAll();
  }

  @Test
  public void testValidateParticipant() throws Exception {
    DocumentReference courseDocRefNew = new DocumentReference("none", "Kurse", "Kurs2");
    expect(stringRefResolverMock.resolve(eq("Kurse.Kurs2"), eq(EntityType.DOCUMENT))
        ).andReturn(courseDocRefNew);
    DocumentReference courseDocRef = new DocumentReference(context.getDatabase(), "Kurse",
        "Kurs2");
    XWikiDocument courseDoc = new XWikiDocument(courseDocRef);
    expect(xwiki.getDocument(eq(courseDocRef), same(context))).andReturn(courseDoc);
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(), 
        "Classes", "CourseParticipantClass");
    BaseObject partiObj = new BaseObject();
    String emailAdr = "test@test.com";
    partiObj.setXClassReference(partiClassRef);
    partiObj.setStringValue("email", emailAdr);
    partiObj.setStringValue("validkey", ACTIVATION_HASH);
    partiObj.setStringValue("status", "unconfirmed");
    courseDoc.setXObject(0, partiObj);
    xwiki.saveDocument(same(courseDoc), eq("validate email addresse by link."),
        same(context));
    expectLastCall().once();
    replayAll();
    assertTrue(courseScriptService.validateParticipant("Kurse.Kurs2", emailAdr,
        ACTIVATION_CODE));
    assertEquals("confirmed", partiObj.getStringValue("status"));
    verifyAll();
  }

  
  private void replayAll(Object ... mocks) {
    replay(xwiki, stringRefResolverMock);
    replay(mocks);
  }

  private void verifyAll(Object ... mocks) {
    verify(xwiki, stringRefResolverMock);
    verify(mocks);
  }

}
