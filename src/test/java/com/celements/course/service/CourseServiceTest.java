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

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.model.access.ModelAccessStrategy;
import com.celements.model.access.exception.DocumentLoadException;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

public class CourseServiceTest extends AbstractComponentTest {

  private CourseService courseService;
  private String db;
  private DocumentReference docRef;
  private XWikiDocument doc;

  @Before
  public void prepareTest() throws Exception {
    registerComponentMock(INextFreeDocRole.class);
    registerComponentMock(ModelAccessStrategy.class);
    courseService = (CourseService) Utils.getComponent(ICourseServiceRole.class);
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
    obj.setStringValue("type", courseService.webUtilsService.serializeRef(typeDocRef));
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
  public void testGetSpaceForEventId_doNothing() {
    String str = "TestString_WithoutAny-Separators";
    SpaceReference repl = new SpaceReference(str, new WikiReference(getContext().getDatabase()));
    assertEquals(repl, courseService.getSpaceForEventId(str));
  }

  @Test
  public void testGetSpaceForEventId_replaceSpaceDocSeparator() {
    String str = "TestString_With.Separator";
    SpaceReference repl = new SpaceReference("TestString_With_Separator", new WikiReference(
        getContext().getDatabase()));
    assertEquals(repl, courseService.getSpaceForEventId(str));
  }

  @Test
  public void testGetSpaceForEventId_replaceWikiSpaceSeparator() {
    String str = "TestString:With-Separators";
    SpaceReference repl = new SpaceReference("TestString_With-Separators", new WikiReference(
        getContext().getDatabase()));
    assertEquals(repl, courseService.getSpaceForEventId(str));
  }

  @Test
  public void testGetSpaceForEventId_replaceWikiSpaceAndDocSeparator() {
    String str = "TestString:With.Separators";
    SpaceReference repl = new SpaceReference("TestString_With_Separators", new WikiReference(
        getContext().getDatabase()));
    assertEquals(repl, courseService.getSpaceForEventId(str));
  }

  @Test
  public void test_createParticipantDocRef_nullSpaceRef() {
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
    expect(getMock(INextFreeDocRole.class).getNextUntitledPageDocRef(eq(
        docRef.getLastSpaceReference()))).andReturn(docRef).once();
    replayDefault();
    assertSame(docRef, courseService.createParticipantDocRef(docRef.getLastSpaceReference()));
    verifyDefault();
  }

  @Test
  public void test_createParticipantDocRef_titled() {
    String name = "asdf";
    getConfigurationSource().setProperty(CourseService.CFGSRC_PARTICIPANT_DOC_NAME_PREFIX, name);
    expect(getMock(INextFreeDocRole.class).getNextTitledPageDocRef(eq(
        docRef.getLastSpaceReference()), eq(name))).andReturn(docRef).once();
    replayDefault();
    assertSame(docRef, courseService.createParticipantDocRef(docRef.getLastSpaceReference()));
    verifyDefault();
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

}
