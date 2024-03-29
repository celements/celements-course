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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.configuration.CelementsFromWikiConfigurationSource;
import com.celements.course.classes.CourseClass;
import com.celements.course.classes.CourseParticipantClass;
import com.celements.course.classes.CourseParticipantClass.ParticipantStatus;
import com.celements.course.classes.CourseTypeClass;
import com.celements.mailsender.IMailSenderRole;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.classes.ClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.util.ModelUtils;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.celements.rendering.RenderCommand;
import com.celements.search.lucene.query.LuceneDocType;
import com.celements.search.lucene.query.LuceneQuery;
import com.celements.web.plugin.cmd.CelMailConfiguration;
import com.google.common.collect.ImmutableList;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
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
  private LucenePlugin lucenePluginMock;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(ConfigurationSource.class, "all", getConfigurationSource());
    registerComponentMock(ConfigurationSource.class, CelementsFromWikiConfigurationSource.NAME,
        getConfigurationSource());
    registerComponentMocks(INextFreeDocRole.class, IModelAccessFacade.class, IMailSenderRole.class);
    getContext().put("vcontext", new VelocityContext());
    courseService = (CourseService) Utils.getComponent(ICourseServiceRole.class);
    courseService.injected_RenderCommand = createDefaultMock(RenderCommand.class);
    db = "db";
    docRef = new DocumentReference(db, "CourseSpace", "CourseX");
    doc = new XWikiDocument(docRef);
    expectClass(Utils.getComponent(ClassDefinition.class, CourseParticipantClass.CLASS_DEF_HINT),
        new WikiReference(db));
    lucenePluginMock = createDefaultMock(LucenePlugin.class);
    expect(getWikiMock().getPlugin(eq("lucene"), same(getContext())))
        .andReturn(lucenePluginMock).anyTimes();
    expect(lucenePluginMock.getAnalyzer()).andReturn(null).anyTimes();
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
    obj.setXClassReference(CourseClass.CLASS_REF);
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
    obj.setXClassReference(CourseClass.CLASS_REF);
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
    typeObj.setXClassReference(CourseTypeClass.CLASS_REF);
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
    typeObj.setXClassReference(CourseTypeClass.CLASS_REF);
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
    } catch (DocumentLoadException exc) {}
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
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
    String email = "test@test.com";

    replayDefault();
    assertFalse(courseService.validateParticipant(regDoc.getDocumentReference(), email,
        ACTIVATION_CODE));
    verifyDefault();
  }

  @Test
  public void test_validateParticipant_wrong_key() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
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
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    getMock(IModelAccessFacade.class).saveDocument(same(regDoc), anyObject(String.class));
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
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    addParticipant(regDoc, email, true);
    getMock(IModelAccessFacade.class).saveDocument(same(regDoc), anyObject(String.class));
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
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    addParticipant(regDoc, "test2@test.com", true);
    getMock(IModelAccessFacade.class).saveDocument(same(regDoc), anyObject(String.class));
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
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    addParticipant(regDoc, null, true);
    getMock(IModelAccessFacade.class).saveDocument(same(regDoc), anyObject(String.class));
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
  public void test_sendConfirmationMail_failed_noemail() throws Exception {
    XWikiDocument regDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    expect(getMock(IModelAccessFacade.class).getOrCreateDocument(eq(regDoc.getDocumentReference())))
        .andReturn(regDoc).once();
    addParticipant(regDoc, null, true);
    addParticipant(regDoc, "test@test.com", true);
    expectDoc(courseService.getConfirmationEmailDocRef());

    replayDefault();
    assertFalse(courseService.sendConfirmationMail(regDoc.getDocumentReference(), 0));
    verifyDefault();
  }

  @Test
  public void test_sendConfirmationMail_success() throws Exception {
    XWikiDocument regDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    expect(getMock(IModelAccessFacade.class).getOrCreateDocument(eq(regDoc.getDocumentReference())))
        .andReturn(regDoc).once();
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    expectEmail(email, 1);

    replayDefault();
    assertTrue(courseService.sendConfirmationMail(regDoc.getDocumentReference(), 0));
    verifyDefault();
  }

  @Test
  public void test_sendConfirmationMail_success_withAttachment() throws Exception {
    XWikiDocument regDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    expect(getMock(IModelAccessFacade.class).getOrCreateDocument(eq(regDoc.getDocumentReference())))
        .andReturn(regDoc).once();
    String email = "test@test.com";
    addParticipant(regDoc, email, true);
    List<Attachment> attachments = ImmutableList.of(createDefaultMock(Attachment.class));
    expectEmail(email, 1, attachments);

    replayDefault();
    assertTrue(courseService.sendConfirmationMail(regDoc.getDocumentReference(), 0));
    verifyDefault();
  }

  @Test
  public void test_setStatusConfirmedFromUnconfirmed() throws Exception {
    XWikiDocument regDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    expect(getMock(IModelAccessFacade.class).getOrCreateDocument(eq(regDoc.getDocumentReference())))
        .andReturn(regDoc).once();
    addParticipant(regDoc, null, true);
    addParticipant(regDoc, "test@test.com", true);
    getMock(IModelAccessFacade.class).saveDocument(same(regDoc), anyObject(String.class));

    replayDefault();
    assertTrue(courseService.setStatusConfirmedFromUnconfirmed(regDoc.getDocumentReference(), 1));
    verifyDefault();
    assertEquals(ImmutableList.of(ParticipantStatus.unconfirmed, ParticipantStatus.confirmed),
        XWikiObjectFetcher.on(regDoc).fetchField(CourseParticipantClass.FIELD_STATUS).list());
  }

  @Test
  public void test_setStatusConfirmedFromUnconfirmed_failWrongInitialStatus() throws Exception {
    XWikiDocument regDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    expect(getMock(IModelAccessFacade.class).getOrCreateDocument(eq(regDoc.getDocumentReference())))
        .andReturn(regDoc).once();
    addParticipant(regDoc, "test@test.com", true).setStringValue(
        CourseParticipantClass.FIELD_STATUS.getName(), ParticipantStatus.duplicate.toString());
    replayDefault();
    assertFalse(courseService.setStatusConfirmedFromUnconfirmed(regDoc.getDocumentReference(), 0));
    verifyDefault();
    assertEquals(ParticipantStatus.duplicate, XWikiObjectFetcher.on(regDoc)
        .fetchField(CourseParticipantClass.FIELD_STATUS).stream().findFirst().orElse(null));
  }

  private XWikiDocument expectEmail(String email, int count) throws Exception {
    return expectEmail(email, count, Collections.emptyList());
  }

  @SuppressWarnings("unchecked")
  private XWikiDocument expectEmail(String email, int count, List<Attachment> attachments)
      throws Exception {
    DocumentReference emailDocRef = courseService.getConfirmationEmailDocRef();
    XWikiDocument emailDoc = createDefaultMock(XWikiDocument.class);
    expect(getMock(IModelAccessFacade.class).getDocument(eq(emailDocRef)))
        .andReturn(emailDoc).atLeastOnce();
    expect(emailDoc.getTitle()).andReturn("subject").atLeastOnce();
    Document emailDocApi = createDefaultMock(Document.class);
    expect(emailDoc.newDocument(getContext())).andReturn(emailDocApi).atLeastOnce();
    expect(emailDocApi.getAttachmentList()).andReturn(attachments).atLeastOnce();
    String sender = "asdf@fdsa.ch";
    expect(getWikiMock().getXWikiPreference(eq("admin_email"), eq(
        CelMailConfiguration.MAIL_DEFAULT_ADMIN_EMAIL_KEY), eq(""), same(getContext()))).andReturn(
            sender).times(count);
    String content = "someContent";
    expect(courseService.injected_RenderCommand.renderCelementsDocument(same(emailDoc), eq(
        "view"))).andReturn(content).times(count);
    expect(getMock(IMailSenderRole.class).sendMail(eq(sender), isNull(String.class), eq(email),
        isNull(String.class), isNull(String.class), eq("subject"), eq(content), eq(content),
        eq(attachments), isNull(Map.class)))
            .andReturn(0).times(count);
    expect(getMock(IMailSenderRole.class).sendMail(eq(sender), isNull(String.class), eq(sender),
        isNull(String.class), isNull(String.class), eq("subject"), eq(content), eq(content),
        eq(attachments), isNull(Map.class)))
            .andReturn(0).times(count);
    return emailDoc;
  }

  @Test
  public void test_getConfirmState_confirmed() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
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
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));

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
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
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
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
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
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
    addParticipant(regDoc, ParticipantStatus.cancelled);
    addParticipant(regDoc, ParticipantStatus.duplicate);

    replayDefault();
    assertSame(RegistrationState.CANCELLED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_duplicate() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
    addParticipant(regDoc, ParticipantStatus.duplicate);
    addParticipant(regDoc, ParticipantStatus.duplicate);

    replayDefault();
    assertSame(RegistrationState.DUPLICATE, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_noParticipant() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
    replayDefault();
    assertSame(RegistrationState.UNDEFINED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_stateAbsent() throws Exception {
    XWikiDocument regDoc = expectDoc(new DocumentReference(db, "Kurse", "Kurs2"));
    addParticipant(regDoc, null);

    replayDefault();
    assertSame(RegistrationState.UNDEFINED, courseService.getConfirmState(
        regDoc.getDocumentReference()));
    verifyDefault();
  }

  @Test
  public void test_getConfirmState_getRegistrationCount_allConfirmed() throws Exception {
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference(db, "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference(db, "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference(db, "Registrations", "Reg3"));
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
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference(db, "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.duplicate);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference(db, "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference(db, "Registrations", "Reg3"));
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
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference(db, "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.cancelled);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference(db, "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.cancelled);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference(db, "Registrations", "Reg3"));
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
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference(db, "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.duplicate);
    addParticipant(regDoc1, ParticipantStatus.cancelled);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference(db, "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.cancelled);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference(db, "Registrations", "Reg3"));
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
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference(db, "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference(db, "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.unconfirmed);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference(db, "Registrations", "Reg3"));
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
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference(db, "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference(db, "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference(db, "Registrations", "Reg3"));
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
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference(db, "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    addParticipant(regDoc1, ParticipantStatus.duplicate);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference(db, "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference(db, "Registrations", "Reg3"));
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
    XWikiDocument courseDoc = new XWikiDocument(new DocumentReference(db, "Kurse", "Kurs2"));
    List<XWikiDocument> regDocs = new ArrayList<>();
    XWikiDocument regDoc1 = expectDoc(new DocumentReference(db, "Registrations", "Reg1"));
    addParticipant(regDoc1, ParticipantStatus.confirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    addParticipant(regDoc1, ParticipantStatus.unconfirmed);
    addParticipant(regDoc1, ParticipantStatus.duplicate);
    regDocs.add(regDoc1);
    XWikiDocument regDoc2 = expectDoc(new DocumentReference(db, "Registrations", "Reg2"));
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.confirmed);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    addParticipant(regDoc2, ParticipantStatus.duplicate);
    regDocs.add(regDoc2);
    XWikiDocument regDoc3 = expectDoc(new DocumentReference(db, "Registrations", "Reg3"));
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
    SearchResults sResultsMock = createDefaultMock(SearchResults.class);
    List<SearchResult> list = new ArrayList<>();
    int index = 0;
    for (XWikiDocument regDoc : docs) {
      list.add(createDefaultMock(SearchResult.class));
      expect(list.get(index).getReference()).andReturn(regDoc.getDocumentReference()).atLeastOnce();
      index++;
    }

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
    partiObj.setXClassReference(CourseParticipantClass.CLASS_REF.getDocRef(
        regDoc.getDocumentReference().getWikiReference()));
    partiObj.setStringValue("status", (state != null ? state.name().toLowerCase() : null));
    partiObj.setStringValue("email", email);
    partiObj.setStringValue("validkey", valid ? ACTIVATION_HASH : "x");
    partiObj.setStringValue("eventid", getModelUtils().serializeRefLocal(
        regDoc.getDocumentReference()));
    regDoc.addXObject(partiObj);
    return partiObj;
  }

  private XWikiDocument expectDoc(DocumentReference docRef) throws DocumentNotExistsException {
    XWikiDocument doc = new XWikiDocument(docRef);
    expect(getMock(IModelAccessFacade.class).getDocument(eq(docRef))).andReturn(doc)
        .atLeastOnce();
    return doc;
  }

  private void expectDoc(boolean throwExc) throws DocumentNotExistsException {
    if (throwExc) {
      expect(getMock(IModelAccessFacade.class).getDocument(eq(docRef)))
          .andThrow(new DocumentLoadException(docRef)).once();
    } else {
      expect(getMock(IModelAccessFacade.class).getDocument(eq(docRef)))
          .andReturn(doc).once();
    }
  }

  private ModelUtils getModelUtils() {
    return Utils.getComponent(ModelUtils.class);
  }

  private static BaseClass expectClass(ClassDefinition classDef, WikiReference wikiRef)
      throws XWikiException {
    BaseClass bClass = createBaseClassMock(classDef.getDocRef(wikiRef));
    for (ClassField<?> field : classDef.getFields()) {
      expect(bClass.get(field.getName())).andReturn(field.getXField()).anyTimes();
    }
    return bClass;
  }

}
