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
package com.celements.course.service;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.course.classes.CourseParticipantClass;
import com.celements.course.classes.CourseParticipantClass.ParticipantStatus;
import com.celements.mailsender.IMailSenderRole;
import com.celements.model.access.ModelAccessStrategy;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.util.ModelUtils;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.celements.rendering.RenderCommand;
import com.celements.search.lucene.query.LuceneDocType;
import com.celements.search.lucene.query.LuceneQuery;
import com.celements.web.plugin.cmd.CelMailConfiguration;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.plugin.lucene.LucenePlugin;
import com.xpn.xwiki.plugin.lucene.SearchResult;
import com.xpn.xwiki.plugin.lucene.SearchResults;
import com.xpn.xwiki.web.Utils;

public class CourseServiceTest extends AbstractComponentTest {

  private static final String ACTIVATION_CODE = "123ho123jjk5689";
  private static final String ACTIVATION_HASH = "hash:SHA-512:1a572fd8d66a08e922a0168671"
      + "fe28cd04be6b84e2fe956f3fff2d0353621108bc0834f2676fd7415542d8759e6a888a4fed9dce738"
      + "dd8c7cc0f89ab3fbce394";

  private CourseService courseService;
  private String db;
  private DocumentReference docRef;
  private XWikiDocument doc;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMocks(INextFreeDocRole.class, ModelAccessStrategy.class,
        IMailSenderRole.class);
    getContext().put("vcontext", new VelocityContext());
    courseService = (CourseService) Utils.getComponent(ICourseServiceRole.class);
    courseService.injected_RenderCommand = createMockAndAddToDefault(RenderCommand.class);
    db = "db";
    docRef = new DocumentReference(db, "CourseSpace", "CourseX");
    doc = new XWikiDocument(docRef);
    expectClass(Utils.getComponent(ClassDefinition.class, CourseParticipantClass.CLASS_DEF_HINT));
  }

  @Test
  public void testNormalizeEmail() {
    assertEquals("fabian.pichler@synventis.com", courseService.normalizeEmail(
        " Fabian.Pichler@Synventis.com "));
  }

  @Test
  public void testGetCourseTypeForCourse() throws Exception {
    DocumentReference typeDocRef = new DocumentReference(db, "TypeSpace", "TypeX");
    expectDoc(false);
    BaseObject obj = new BaseObject();
    obj.setXClassReference(courseService.getCourseClasses().getCourseClassRef(db));
    obj.setStringValue("type", getModelUtils().serializeRef(typeDocRef));
    doc.addXObject(obj);

    replayDefault();
    DocumentReference ret = courseService.getCourseTypeForCourse(docRef);
    verifyDefault();
    assertEquals(typeDocRef, ret);
  }

  @Test
  public void testGetCourseTypeForCourse_noType() throws Exception {
    expectDoc(false);
    BaseObject obj = new BaseObject();
    obj.setXClassReference(courseService.getCourseClasses().getCourseClassRef(db));
    doc.addXObject(obj);

    replayDefault();
    DocumentReference ret = courseService.getCourseTypeForCourse(docRef);
    verifyDefault();
    assertNull(ret);
  }

  @Test
  public void testGetCourseTypeForCourse_noObj() throws Exception {
    expectDoc(false);
    replayDefault();
    DocumentReference ret = courseService.getCourseTypeForCourse(docRef);
    verifyDefault();
    assertNull(ret);
  }

  @Test
  public void testGetCourseTypeForCourse_XWE() throws Exception {
    expectDoc(true);
    replayDefault();
    try {
      courseService.getCourseTypeForCourse(docRef);
      fail("expecting XWE");
    } catch (DocumentLoadException exc) {
      // expected outcome
    }
    verifyDefault();
  }

  @Test
  public void testGetCourseTypeName() throws Exception {
    String name = "asdf";
    expectDoc(false);
    BaseObject typeObj = new BaseObject();
    typeObj.setXClassReference(courseService.getCourseClasses().getCourseTypeClassRef(db));
    typeObj.setStringValue("typeName", name);
    doc.addXObject(typeObj);

    replayDefault();
    String ret = courseService.getCourseTypeName(docRef);
    verifyDefault();
    assertEquals(name, ret);
  }

  @Test
  public void testGetCourseTypeName_noName() throws Exception {
    expectDoc(false);
    BaseObject typeObj = new BaseObject();
    typeObj.setXClassReference(courseService.getCourseClasses().getCourseTypeClassRef(db));
    doc.addXObject(typeObj);

    replayDefault();
    String ret = courseService.getCourseTypeName(docRef);
    verifyDefault();
    assertEquals("", ret);
  }

  @Test
  public void testGetCourseTypeName_noObj() throws Exception {
    expectDoc(false);
    replayDefault();
    String ret = courseService.getCourseTypeName(docRef);
    verifyDefault();
    assertEquals("", ret);
  }

  @Test
  public void testGetCourseTypeName_XWE() throws Exception {
    expectDoc(true);
    replayDefault();
    try {
      courseService.getCourseTypeName(docRef);
      fail("expecting XWE");
    } catch (DocumentLoadException exc) {
    }
    verifyDefault();
  }

  @Test
  public void test_getRegistrationSpace_null() {
    replayDefault();
    try {
      courseService.getRegistrationSpace(null);
      fail("expecting NPE");
    } catch (NullPointerException npe) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_getRegistrationSpace() {
    SpaceReference repl = new SpaceReference(docRef.getParent().getName() + "_" + docRef.getName(),
        docRef.getWikiReference());
    assertEquals(repl, courseService.getRegistrationSpace(docRef));
  }

  @Test
  public void test_createParticipantDocRef_null() {
    replayDefault();
    try {
      courseService.createParticipantDocRef(null);
      fail("expecting NPE");
    } catch (NullPointerException npe) {
      // expected
    }
    verifyDefault();
  }

  @Test
  public void test_createParticipantDocRef_untitled() {
    SpaceReference spaceRef = courseService.getRegistrationSpace(docRef);
    DocumentReference regDocRef = new DocumentReference("untitled123", spaceRef);
    expect(getMock(INextFreeDocRole.class).getNextUntitledPageDocRef(eq(spaceRef))).andReturn(
        regDocRef).once();
    replayDefault();
    assertSame(regDocRef, courseService.createParticipantDocRef(docRef));
    verifyDefault();
  }

  @Test
  public void test_createParticipantDocRef_titled() {
    SpaceReference spaceRef = courseService.getRegistrationSpace(docRef);
    String name = "asdf";
    DocumentReference regDocRef = new DocumentReference(name + "123", spaceRef);
    getConfigurationSource().setProperty(CourseService.CFGSRC_PARTICIPANT_DOC_NAME_PREFIX, name);
    expect(getMock(INextFreeDocRole.class).getNextTitledPageDocRef(eq(spaceRef), eq(
        name))).andReturn(regDocRef).once();
    replayDefault();
    assertSame(regDocRef, courseService.createParticipantDocRef(docRef));
    verifyDefault();
  }

  @Test
  public void testPasswordHashString() {
    assertEquals(ACTIVATION_HASH, courseService.passwordHashString(ACTIVATION_CODE));
  }

  @Test
  public void test_validateParticipant_no_object() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    String email = "test@test.com";

    replayDefault();
    assertFalse(courseService.validateParticipant(regDoc.getDocumentReference(), email,
        ACTIVATION_CODE));
    verifyDefault();
  }

  @Test
  public void test_validateParticipant_wrong_key() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    String email = "test@test.com";
    addParticipant(regDoc, email, false);

    replayDefault();
    assertFalse(courseService.validateParticipant(regDoc.getDocumentReference(), email,
        ACTIVATION_CODE));
    assertSame(RegistrationState.UNCONFIRMED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_validateParticipant() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    getMock(ModelAccessStrategy.class).saveDocument(same(regDoc), anyObject(String.class), eq(
        false));
    expectLastCall().once();
    expectEmail(email, 1);

    replayDefault();
    assertTrue(courseService.validateParticipant(regDoc.getDocumentReference(), email,
        ACTIVATION_CODE));
    assertSame(RegistrationState.CONFIRMED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_validateParticipant_multiple_sameEmail() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    addParticipant(regDoc, email, true);
    getMock(ModelAccessStrategy.class).saveDocument(same(regDoc), anyObject(String.class), eq(
        false));
    expectLastCall().once();
    expectEmail(email, 2);

    replayDefault();
    assertTrue(courseService.validateParticipant(regDoc.getDocumentReference(), email,
        ACTIVATION_CODE));
    assertSame(RegistrationState.CONFIRMED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_validateParticipant_multiple_otherEmail() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    addParticipant(regDoc, "test2@test.com", true);
    getMock(ModelAccessStrategy.class).saveDocument(same(regDoc), anyObject(String.class), eq(
        false));
    expectLastCall().once();
    expectEmail(email, 1);

    replayDefault();
    assertTrue(courseService.validateParticipant(regDoc.getDocumentReference(), email,
        ACTIVATION_CODE));
    assertSame(RegistrationState.PARTIALCONFIRMED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_validateParticipant_multiple_withNoEmail() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    addParticipant(regDoc, null, true);
    getMock(ModelAccessStrategy.class).saveDocument(same(regDoc), anyObject(String.class), eq(
        false));
    expectLastCall().once();
    expectEmail(email, 2);

    replayDefault();
    assertTrue(courseService.validateParticipant(regDoc.getDocumentReference(), email,
        ACTIVATION_CODE));
    assertSame(RegistrationState.CONFIRMED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @SuppressWarnings("unchecked")
  private void expectEmail(String email, int count) throws Exception {
    XWikiDocument emailDoc = expectDoc(courseService.getConfirmationEmailDocRef());
    String sender = "asdf@fdsa.ch";
    expect(getWikiMock().getXWikiPreference(eq("admin_email"), eq(
        CelMailConfiguration.MAIL_DEFAULT_ADMIN_EMAIL_KEY), eq(""), same(getContext()))).andReturn(
            sender).times(count);
    String content = "someContent";
    expect(courseService.injected_RenderCommand.renderCelementsDocument(same(emailDoc), eq(
        "view"))).andReturn(content).times(count);
    expect(getMock(IMailSenderRole.class).sendMail(eq(sender), isNull(String.class), eq(email),
        isNull(String.class), isNull(String.class), eq(""), eq(content), eq(content), isNull(
            List.class), isNull(Map.class))).andReturn(0).times(count);
    expect(getMock(IMailSenderRole.class).sendMail(eq(sender), isNull(String.class), eq(sender),
        isNull(String.class), isNull(String.class), eq(""), eq(content), eq(content), isNull(
            List.class), isNull(Map.class))).andReturn(0).times(count);
  }

  @Test
  public void test_getConfirmState_confirmed() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    addParticipant(regDoc, ParticipantStatus.confirmed);
    addParticipant(regDoc, ParticipantStatus.confirmed);
    addParticipant(regDoc, ParticipantStatus.cancelled);
    addParticipant(regDoc, ParticipantStatus.duplicate);

    replayDefault();
    assertSame(RegistrationState.CONFIRMED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_unconfirmed() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));

    addParticipant(regDoc, ParticipantStatus.unconfirmed);
    addParticipant(regDoc, ParticipantStatus.unconfirmed);
    addParticipant(regDoc, ParticipantStatus.cancelled);
    addParticipant(regDoc, ParticipantStatus.duplicate);

    replayDefault();
    assertSame(RegistrationState.UNCONFIRMED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_partialConfirmed1() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    addParticipant(regDoc, ParticipantStatus.confirmed);
    addParticipant(regDoc, ParticipantStatus.unconfirmed);
    addParticipant(regDoc, ParticipantStatus.cancelled);
    addParticipant(regDoc, ParticipantStatus.duplicate);

    replayDefault();
    assertSame(RegistrationState.PARTIALCONFIRMED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_partialConfirmed2() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    addParticipant(regDoc, ParticipantStatus.unconfirmed);
    addParticipant(regDoc, ParticipantStatus.confirmed);
    addParticipant(regDoc, ParticipantStatus.cancelled);
    addParticipant(regDoc, ParticipantStatus.duplicate);

    replayDefault();
    assertSame(RegistrationState.PARTIALCONFIRMED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_cancelled() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    addParticipant(regDoc, ParticipantStatus.cancelled);
    addParticipant(regDoc, ParticipantStatus.duplicate);

    replayDefault();
    assertSame(RegistrationState.CANCELLED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_duplicate() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    addParticipant(regDoc, ParticipantStatus.duplicate);
    addParticipant(regDoc, ParticipantStatus.duplicate);

    replayDefault();
    assertSame(RegistrationState.DUPLICATE, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_noParticipant() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    replayDefault();
    assertSame(RegistrationState.UNDEFINED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_stateAbsent() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference("db", "Kurse", "Kurs2"));
    addParticipant(regDoc, null);

    replayDefault();
    assertSame(RegistrationState.UNDEFINED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_getRegistrationCount_allConfirmed() throws Exception {
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference("xwikidb", "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg3"));
    addParticipant(regDoc3, ParticipantStatus.confirmed);
    addParticipant(regDoc3, ParticipantStatus.confirmed);
    regDocs.add(regDoc3);
    fillRegistrationList(regDocs);

    replayDefault();
    long expRes = 6;
    assertSame(expRes, courseService.getRegistrationCount(courseDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_getRegistrationCount_withDuplicate() throws Exception {
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference("xwikidb", "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.duplicate);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg3"));
    addParticipant(regDoc3, ParticipantStatus.confirmed);
    addParticipant(regDoc3, ParticipantStatus.confirmed);
    regDocs.add(regDoc3);
    fillRegistrationList(regDocs);

    replayDefault();
    long expRes = 4;
    assertSame(expRes, courseService.getRegistrationCount(courseDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_getRegistrationCount_withCancelled() throws Exception {
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference("xwikidb", "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.cancelled);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.cancelled);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg3"));
    addParticipant(regDoc3, ParticipantStatus.confirmed);
    addParticipant(regDoc3, ParticipantStatus.cancelled);
    regDocs.add(regDoc3);
    fillRegistrationList(regDocs);

    replayDefault();
    long expRes = 3;
    assertSame(expRes, courseService.getRegistrationCount(courseDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_getRegistrationCount_allWithCancelledAndDuplicate()
      throws Exception {
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference("xwikidb", "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.duplicate);
    addParticipant(regDoc1, ParticipantStatus.cancelled);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.cancelled);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg3"));
    addParticipant(regDoc3, ParticipantStatus.confirmed);
    addParticipant(regDoc3, ParticipantStatus.confirmed);
    addParticipant(regDoc3, ParticipantStatus.duplicate);
    addParticipant(regDoc3, ParticipantStatus.cancelled);
    regDocs.add(regDoc3);
    fillRegistrationList(regDocs);

    replayDefault();
    long expRes = 5;
    assertSame(expRes, courseService.getRegistrationCount(courseDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_getRegistrationCount_withUnconfirmed() throws Exception {
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference("xwikidb", "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.unconfirmed);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg3"));
    addParticipant(regDoc3, ParticipantStatus.unconfirmed);
    addParticipant(regDoc3, ParticipantStatus.unconfirmed);
    regDocs.add(regDoc3);
    fillRegistrationList(regDocs);

    replayDefault();
    long expRes = 6;
    assertSame(expRes, courseService.getRegistrationCount(courseDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_getRegistrationCount_withAllStates() throws Exception {
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference("xwikidb", "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg3"));
    addParticipant(regDoc3, ParticipantStatus.unconfirmed);
    addParticipant(regDoc3, ParticipantStatus.cancelled);
    regDocs.add(regDoc3);
    fillRegistrationList(regDocs);

    replayDefault();
    assertSame(4l, courseService.getRegistrationCount(courseDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_getRegistrationCount_withParticipantState() throws Exception {
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference("xwikidb", "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    addParticipant(regDoc1, ParticipantStatus.duplicate);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg3"));
    addParticipant(regDoc3, ParticipantStatus.unconfirmed);
    addParticipant(regDoc3, ParticipantStatus.cancelled);
    addParticipant(regDoc3, ParticipantStatus.cancelled);
    regDocs.add(regDoc3);
    fillRegistrationList(regDocs);

    replayDefault();
    assertSame(2l, courseService.getRegistrationCount(courseDoc.getDocumentReference(),
        ParticipantStatus.cancelled));
    assertSame(3l, courseService.getRegistrationCount(courseDoc.getDocumentReference(),
        ParticipantStatus.confirmed));
    assertSame(4l, courseService.getRegistrationCount(courseDoc.getDocumentReference(),
        ParticipantStatus.duplicate));
    assertSame(3l, courseService.getRegistrationCount(courseDoc.getDocumentReference(),
        ParticipantStatus.unconfirmed));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_getRegistrationCount_ignoreParticipantStates() throws Exception {
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference("xwikidb", "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    addParticipant(regDoc1, ParticipantStatus.duplicate);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference("xwikidb", "Registrations", "Reg3"));
    addParticipant(regDoc3, ParticipantStatus.unconfirmed);
    addParticipant(regDoc3, ParticipantStatus.cancelled);
    addParticipant(regDoc3, ParticipantStatus.cancelled);
    addParticipant(regDoc3, ParticipantStatus.cancelled);
    addParticipant(regDoc3, ParticipantStatus.cancelled);
    regDocs.add(regDoc3);
    fillRegistrationList(regDocs);

    replayDefault();
    assertSame(8l, courseService.getRegistrationCount(courseDoc.getDocumentReference(),
        Arrays.asList(ParticipantStatus.unconfirmed, ParticipantStatus.confirmed)));
    assertSame(6l, courseService.getRegistrationCount(courseDoc.getDocumentReference(),
        Arrays.asList(ParticipantStatus.duplicate, ParticipantStatus.cancelled)));
    assertSame(10l, courseService.getRegistrationCount(courseDoc.getDocumentReference(),
        Arrays.asList(ParticipantStatus.cancelled)));
    assertSame(10l, courseService.getRegistrationCount(courseDoc.getDocumentReference(),
        Arrays.asList(ParticipantStatus.duplicate)));
    verifyDefault();
  }

  private void fillRegistrationList(List<XWikiDocument> docs) throws Exception {
    XWiki xwiki = getWikiMock();
    LuceneQuery query = new LuceneQuery();
    List<LuceneDocType> docTypes = new ArrayList<>();
    docTypes.add(LuceneDocType.DOC);
    query.setDocTypes(docTypes);
    SearchResults sResultsMock = createMockAndAddToDefault(SearchResults.class);
    List<SearchResult> list = new ArrayList<>();
    int index = 0;
    for (XWikiDocument regDoc : docs) {
      list.add(createMockAndAddToDefault(SearchResult.class));
      expect(list.get(index).getReference()).andReturn(regDoc.getDocumentReference()).atLeastOnce();
      index++;
    }

    LucenePlugin lucenePluginMock = createMockAndAddToDefault(LucenePlugin.class);
    expect(xwiki.getPlugin(eq("lucene"), same(getContext()))).andReturn(
        lucenePluginMock).atLeastOnce();
    expect(lucenePluginMock.getSearchResults((String) anyObject(), (String[]) anyObject(),
        (String) isNull(), eq(""), same(getContext()))).andReturn(sResultsMock).atLeastOnce();
    expect(sResultsMock.getHitcount()).andReturn(1234).atLeastOnce();
    expect(sResultsMock.getResults(eq(1), eq(1234))).andReturn(list).atLeastOnce();
  }

  private BaseObject addParticipant(XWikiDocument regDoc, ParticipantStatus state) {
    return addParticipant(regDoc, state, null, false);
  }

  private BaseObject addParticipant(XWikiDocument regDoc, String email, boolean valid) {
    return addParticipant(regDoc, ParticipantStatus.unconfirmed, email, valid);
  }

  private BaseObject addParticipant(XWikiDocument regDoc, ParticipantStatus state, String email,
      boolean valid) {
    BaseObject partiObj = new BaseObject();
    partiObj.setXClassReference(getParticipantClassDef().getClassReference().getDocRef(
        regDoc.getDocumentReference().getWikiReference()));
    partiObj.setStringValue("status", (state != null ? state.name().toLowerCase() : null));
    partiObj.setStringValue("email", email);
    partiObj.setStringValue("validkey", valid ? ACTIVATION_HASH : "x");
    partiObj.setStringValue("eventid", getModelUtils().serializeRefLocal(
        regDoc.getDocumentReference()));
    regDoc.addXObject(partiObj);
    return partiObj;
  }

  private ClassDefinition getParticipantClassDef() {
    return Utils.getComponent(ClassDefinition.class, CourseParticipantClass.CLASS_DEF_HINT);
  }

  private XWikiDocument expectDoc(DocumentReference docRef) throws XWikiException {
    XWikiDocument doc = new XWikiDocument(docRef);
    expect(getMock(ModelAccessStrategy.class).exists(eq(docRef), eq(""))).andReturn(
        true).atLeastOnce();
    expect(getMock(ModelAccessStrategy.class).getDocument(eq(docRef), eq(""))).andReturn(
        doc).atLeastOnce();
    return doc;
  }

  private void expectDoc(boolean throwExc) throws XWikiException {
    expect(getMock(ModelAccessStrategy.class).exists(eq(docRef), eq(""))).andReturn(true).once();
    if (throwExc) {
      expect(getMock(ModelAccessStrategy.class).getDocument(eq(docRef), eq(""))).andThrow(
          new DocumentLoadException(docRef)).once();
    } else {
      expect(getMock(ModelAccessStrategy.class).getDocument(eq(docRef), eq(""))).andReturn(
          doc).once();
    }
  }

  private ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

  private static BaseClass expectClass(ClassDefinition classDef) throws XWikiException {
    BaseClass bClass = createBaseClassMock(classDef.getDocRef());
    for (ClassField<?> field : classDef.getFields()) {
      expect(bClass.get(field.getName())).andReturn(field.getXField()).anyTimes();
    }
    return bClass;
  }

}
