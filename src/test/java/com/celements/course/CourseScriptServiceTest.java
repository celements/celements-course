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
package com.celements.course;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.script.service.ScriptService;

import com.celements.common.test.AbstractComponentTest;
import com.celements.course.classcollections.CourseClasses;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

public class CourseScriptServiceTest extends AbstractComponentTest {

  private static final String ACTIVATION_CODE = "123ho123jjk5689";
  private static final String ACTIVATION_HASH = "hash:SHA-512:1a572fd8d66a08e922a0168671"
      + "fe28cd04be6b84e2fe956f3fff2d0353621108bc0834f2676fd7415542d8759e6a888a4fed9dce738"
      + "dd8c7cc0f89ab3fbce394";

  private CourseScriptService courseScriptService;
  private XWiki xwiki;
  private EntityReferenceResolver<String> stringRefResolverMock;

  @SuppressWarnings("unchecked")
  @Before
  public void setUp_CourseScriptServiceTest() throws Exception {
    xwiki = createMock(XWiki.class);
    getContext().setWiki(xwiki);
    stringRefResolverMock = registerComponentMock(EntityReferenceResolver.class);
    courseScriptService = (CourseScriptService) Utils.getComponent(ScriptService.class,
        "celcourse");
  }

  @Test
  public void testPasswordHashString() {
    assertEquals(ACTIVATION_HASH, courseScriptService.passwordHashString(ACTIVATION_CODE));
  }

  @Test
  public void testGetDocRefForFullName() {
    DocumentReference courseDocRefNew = new DocumentReference("none", "mySpace", "MyDoc");
    expect(stringRefResolverMock.resolve(eq("mySpace.MyDoc"), eq(EntityType.DOCUMENT))).andReturn(
        courseDocRefNew);
    DocumentReference courseDocRef = new DocumentReference(getContext().getDatabase(), "mySpace",
        "MyDoc");
    replayAll();
    assertEquals(courseDocRef, courseScriptService.getDocRefForFullName("mySpace.MyDoc"));
    verifyAll();
  }

  @Test
  public void testValidateParticipant_no_object() throws Exception {
    DocumentReference courseDocRefNew = new DocumentReference("none", "Kurse", "Kurs2");
    expect(stringRefResolverMock.resolve(eq("Kurse.Kurs2"), eq(EntityType.DOCUMENT))).andReturn(
        courseDocRefNew);
    DocumentReference courseDocRef = new DocumentReference(getContext().getDatabase(), "Kurse",
        "Kurs2");
    XWikiDocument courseDoc = new XWikiDocument(courseDocRef);
    expect(xwiki.getDocument(eq(courseDocRef), same(getContext()))).andReturn(courseDoc);
    expectLastCall().once();
    replayAll();
    assertFalse(courseScriptService.validateParticipant("Kurse.Kurs2", "test@test.com",
        ACTIVATION_CODE));
    verifyAll();
  }

  @Test
  public void testValidateParticipant_wrong_initial_status() throws Exception {
    DocumentReference courseDocRefNew = new DocumentReference("none", "Kurse", "Kurs2");
    expect(stringRefResolverMock.resolve(eq("Kurse.Kurs2"), eq(EntityType.DOCUMENT))).andReturn(
        courseDocRefNew);
    DocumentReference courseDocRef = new DocumentReference(getContext().getDatabase(), "Kurse",
        "Kurs2");
    XWikiDocument courseDoc = new XWikiDocument(courseDocRef);
    expect(xwiki.getDocument(eq(courseDocRef), same(getContext()))).andReturn(courseDoc);
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(), "Classes",
        "CourseParticipantClass");
    BaseObject partiObj = new BaseObject();
    String emailAdr = "test@test.com";
    partiObj.setXClassReference(partiClassRef);
    partiObj.setStringValue("email", emailAdr);
    partiObj.setStringValue("validkey", ACTIVATION_HASH);
    partiObj.setStringValue("status", "some different status");
    courseDoc.setXObject(0, partiObj);
    replayAll();
    assertFalse(courseScriptService.validateParticipant("Kurse.Kurs2", emailAdr, ACTIVATION_CODE));
    assertEquals("some different status", partiObj.getStringValue("status"));
    verifyAll();
  }

  @Test
  public void testValidateParticipant() throws Exception {
    DocumentReference courseDocRefNew = new DocumentReference("none", "Kurse", "Kurs2");
    expect(stringRefResolverMock.resolve(eq("Kurse.Kurs2"), eq(EntityType.DOCUMENT))).andReturn(
        courseDocRefNew);
    DocumentReference courseDocRef = new DocumentReference(getContext().getDatabase(), "Kurse",
        "Kurs2");
    XWikiDocument courseDoc = new XWikiDocument(courseDocRef);
    expect(xwiki.getDocument(eq(courseDocRef), same(getContext()))).andReturn(courseDoc);
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(),
        CourseClasses.COURSE_CLASSES_SPACE, CourseClasses.COURSE_PARTICIPANT_CLASS_DOC);
    BaseObject partiObj = new BaseObject();
    String emailAdr = "test@test.com";
    partiObj.setXClassReference(partiClassRef);
    partiObj.setStringValue("email", emailAdr);
    partiObj.setStringValue("validkey", ACTIVATION_HASH);
    partiObj.setStringValue("status", "unconfirmed");
    courseDoc.setXObject(0, partiObj);
    xwiki.saveDocument(same(courseDoc), eq("validate email addresse by link."), same(getContext()));
    expectLastCall().once();
    replayAll();
    assertTrue(courseScriptService.validateParticipant("Kurse.Kurs2", emailAdr, ACTIVATION_CODE));
    assertEquals("confirmed", partiObj.getStringValue("status"));
    verifyAll();
  }

  private void replayAll(Object... mocks) {
    replay(xwiki, stringRefResolverMock);
    replay(mocks);
  }

  private void verifyAll(Object... mocks) {
    verify(xwiki, stringRefResolverMock);
    verify(mocks);
  }

}
