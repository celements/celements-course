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

import java.util.List;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.course.classcollections.CourseClasses;
import com.celements.mailsender.IMailSenderRole;
import com.celements.model.access.ModelAccessStrategy;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.model.util.ModelUtils;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.celements.rendering.RenderCommand;
import com.celements.web.plugin.cmd.CelMailConfiguration;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
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
  public void testValidateParticipant_no_object() throws Exception {
    DocumentReference courseDocRef = new DocumentReference(getContext().getDatabase(), "Kurse",
        "Kurs2");
    expectDoc(courseDocRef);
    replayDefault();
    assertFalse(courseService.validateParticipant(courseDocRef, "test@test.com", ACTIVATION_CODE));
    verifyDefault();
  }

  @Test
  public void testValidateParticipant_wrong_initial_status() throws Exception {
    DocumentReference courseDocRef = new DocumentReference(getContext().getDatabase(), "Kurse",
        "Kurs2");
    XWikiDocument courseDoc = expectDoc(courseDocRef);
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(), "Classes",
        "CourseParticipantClass");
    BaseObject partiObj = new BaseObject();
    String emailAdr = "test@test.com";
    partiObj.setXClassReference(partiClassRef);
    partiObj.setStringValue("email", emailAdr);
    partiObj.setStringValue("validkey", ACTIVATION_HASH);
    partiObj.setStringValue("status", "some different status");
    courseDoc.setXObject(0, partiObj);
    replayDefault();
    assertFalse(courseService.validateParticipant(courseDocRef, emailAdr, ACTIVATION_CODE));
    assertEquals("some different status", partiObj.getStringValue("status"));
    verifyDefault();
  }

  @Test
  public void testValidateParticipant() throws Exception {
    DocumentReference courseDocRef = new DocumentReference(getContext().getDatabase(), "Kurse",
        "Kurs2");
    XWikiDocument courseDoc = expectDoc(courseDocRef);
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(),
        CourseClasses.COURSE_CLASSES_SPACE, CourseClasses.COURSE_PARTICIPANT_CLASS_DOC);
    BaseObject partiObj = new BaseObject();
    String emailAdr = "test@test.com";
    partiObj.setXClassReference(partiClassRef);
    partiObj.setStringValue("email", emailAdr);
    partiObj.setStringValue("validkey", ACTIVATION_HASH);
    partiObj.setStringValue("status", "unconfirmed");
    partiObj.setStringValue("eventid", "Kurse.Kurs2");
    courseDoc.addXObject(partiObj);
    getMock(ModelAccessStrategy.class).saveDocument(same(courseDoc), eq(
        "validate email addresse by link."), eq(false));
    expectLastCall().once();
    XWikiDocument emailDoc = expectDoc(courseService.getValidationEmailDocRef());
    String sender = "asdf@fdsa.ch";
    expect(getWikiMock().getXWikiPreference(eq("admin_email"), eq(
        CelMailConfiguration.MAIL_DEFAULT_ADMIN_EMAIL_KEY), eq(""), same(getContext()))).andReturn(
            sender).once();
    String content = "someContent";
    expect(courseService.injected_RenderCommand.renderCelementsDocument(same(emailDoc), eq(
        "view"))).andReturn(content).once();
    expect(getMock(IMailSenderRole.class).sendMail(eq(sender), isNull(String.class), eq(emailAdr),
        isNull(String.class), isNull(String.class), eq(""), eq(content), eq(content), isNull(
            List.class), isNull(Map.class))).andReturn(0).once();
    expect(getMock(IMailSenderRole.class).sendMail(eq(sender), isNull(String.class), eq(sender),
        isNull(String.class), isNull(String.class), eq(""), eq(content), eq(content), isNull(
            List.class), isNull(Map.class))).andReturn(0).once();
    replayDefault();
    assertTrue(courseService.validateParticipant(courseDocRef, emailAdr, ACTIVATION_CODE));
    assertEquals("confirmed", partiObj.getStringValue("status"));
    verifyDefault();
  }

  private XWikiDocument expectDoc(DocumentReference docRef) throws XWikiException {
    XWikiDocument doc = new XWikiDocument(docRef);
    expect(getMock(ModelAccessStrategy.class).exists(eq(docRef), eq(""))).andReturn(true).once();
    expect(getMock(ModelAccessStrategy.class).getDocument(eq(docRef), eq(""))).andReturn(
        doc).once();
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

}
