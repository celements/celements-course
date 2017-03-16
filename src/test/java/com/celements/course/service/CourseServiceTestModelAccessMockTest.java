package com.celements.course.service;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.easymock.EasyMock.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.apache.velocity.VelocityContext;
import org.junit.Before;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.celements.common.test.AbstractComponentTest;
import com.celements.course.classcollections.CourseClasses;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.rendering.RenderCommand;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.Utils;

public class CourseServiceTestModelAccessMockTest extends AbstractComponentTest {

  private CourseService courseService;
  private IModelAccessFacade modelAccessMock;

  @Before
  public void prepareTest() throws Exception {
    getContext().put("vcontext", new VelocityContext());
    modelAccessMock = registerComponentMock(IModelAccessFacade.class);
    courseService = (CourseService) Utils.getComponent(ICourseServiceRole.class);
    courseService.injected_RenderCommand = createMockAndAddToDefault(RenderCommand.class);
  }

  @Test
  public void testGetConfirmState_confirmed() throws Exception {
    DocumentReference regDocRef = new DocumentReference("frommkurse",
        "ProgonEvent_ProgonEventFromm7", "Anmeldung36");
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(),
        CourseClasses.COURSE_CLASSES_SPACE, CourseClasses.COURSE_PARTICIPANT_CLASS_DOC);
    BaseObject partiObj1 = new BaseObject();
    partiObj1.setXClassReference(partiClassRef);
    partiObj1.setStringValue("status", "confirmed");
    BaseObject partiObj2 = new BaseObject();
    partiObj2.setXClassReference(partiClassRef);
    partiObj2.setStringValue("status", "confirmed");
    List<BaseObject> partiObjs = Arrays.<BaseObject>asList(partiObj1, partiObj2);

    expect(modelAccessMock.getXObjects(eq(regDocRef), eq(partiClassRef))).andReturn(partiObjs);

    replayDefault();
    CourseConfirmState confirmState = courseService.getConfirmState(regDocRef);
    verifyDefault();
    assertEquals(CourseConfirmState.CONFIRMED, confirmState);
  }

  @Test
  public void testGetConfirmState_unconfirmed() throws Exception {
    DocumentReference regDocRef = new DocumentReference("frommkurse",
        "ProgonEvent_ProgonEventFromm7", "Anmeldung36");
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(),
        CourseClasses.COURSE_CLASSES_SPACE, CourseClasses.COURSE_PARTICIPANT_CLASS_DOC);
    BaseObject partiObj1 = new BaseObject();
    partiObj1.setXClassReference(partiClassRef);
    partiObj1.setStringValue("status", "unconfirmed");
    BaseObject partiObj2 = new BaseObject();
    partiObj2.setXClassReference(partiClassRef);
    partiObj2.setStringValue("status", "unconfirmed");
    List<BaseObject> partiObjs = Arrays.<BaseObject>asList(partiObj1, partiObj2);

    expect(modelAccessMock.getXObjects(eq(regDocRef), eq(partiClassRef))).andReturn(partiObjs);

    replayDefault();
    CourseConfirmState confirmState = courseService.getConfirmState(regDocRef);
    verifyDefault();
    assertEquals(CourseConfirmState.UNCONFIRMED, confirmState);
  }

  @Test
  public void testGetConfirmState_partialConfirmed1() throws Exception {
    DocumentReference regDocRef = new DocumentReference("frommkurse",
        "ProgonEvent_ProgonEventFromm7", "Anmeldung36");
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(),
        CourseClasses.COURSE_CLASSES_SPACE, CourseClasses.COURSE_PARTICIPANT_CLASS_DOC);
    BaseObject partiObj1 = new BaseObject();
    partiObj1.setXClassReference(partiClassRef);
    partiObj1.setStringValue("status", "confirmed");
    BaseObject partiObj2 = new BaseObject();
    partiObj2.setXClassReference(partiClassRef);
    partiObj2.setStringValue("status", "unconfirmed");
    List<BaseObject> partiObjs = Arrays.<BaseObject>asList(partiObj1, partiObj2);

    expect(modelAccessMock.getXObjects(eq(regDocRef), eq(partiClassRef))).andReturn(partiObjs);

    replayDefault();
    CourseConfirmState confirmState = courseService.getConfirmState(regDocRef);
    verifyDefault();
    assertEquals(CourseConfirmState.PARTIALCONFIRMED, confirmState);
  }

  @Test
  public void testGetConfirmState_partialConfirmed2() throws Exception {
    DocumentReference regDocRef = new DocumentReference("frommkurse",
        "ProgonEvent_ProgonEventFromm7", "Anmeldung36");
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(),
        CourseClasses.COURSE_CLASSES_SPACE, CourseClasses.COURSE_PARTICIPANT_CLASS_DOC);
    BaseObject partiObj1 = new BaseObject();
    partiObj1.setXClassReference(partiClassRef);
    partiObj1.setStringValue("status", "unconfirmed");
    BaseObject partiObj2 = new BaseObject();
    partiObj2.setXClassReference(partiClassRef);
    partiObj2.setStringValue("status", "confirmed");
    List<BaseObject> partiObjs = Arrays.<BaseObject>asList(partiObj1, partiObj2);

    expect(modelAccessMock.getXObjects(eq(regDocRef), eq(partiClassRef))).andReturn(partiObjs);

    replayDefault();
    CourseConfirmState confirmState = courseService.getConfirmState(regDocRef);
    verifyDefault();
    assertEquals(CourseConfirmState.PARTIALCONFIRMED, confirmState);
  }

  @Test
  public void testGetConfirmState_noParticipant() throws Exception {
    DocumentReference regDocRef = new DocumentReference("frommkurse",
        "ProgonEvent_ProgonEventFromm7", "Anmeldung36");
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(),
        CourseClasses.COURSE_CLASSES_SPACE, CourseClasses.COURSE_PARTICIPANT_CLASS_DOC);

    expect(modelAccessMock.getXObjects(eq(regDocRef), eq(partiClassRef))).andThrow(
        new DocumentNotExistsException(regDocRef));

    replayDefault();
    CourseConfirmState confirmState = courseService.getConfirmState(regDocRef);
    verifyDefault();
    assertEquals(CourseConfirmState.UNDEFINED, confirmState);
  }

  @Test
  public void testGetConfirmState_stateAbsent() throws Exception {
    DocumentReference regDocRef = new DocumentReference("frommkurse",
        "ProgonEvent_ProgonEventFromm7", "Anmeldung36");
    DocumentReference partiClassRef = new DocumentReference(getContext().getDatabase(),
        CourseClasses.COURSE_CLASSES_SPACE, CourseClasses.COURSE_PARTICIPANT_CLASS_DOC);

    BaseObject partiObj1 = new BaseObject();
    partiObj1.setXClassReference(partiClassRef);
    partiObj1.setStringValue("status", "nostate");
    List<BaseObject> partiObjs = Arrays.<BaseObject>asList(partiObj1);

    expect(modelAccessMock.getXObjects(eq(regDocRef), eq(partiClassRef))).andReturn(partiObjs);

    replayDefault();
    CourseConfirmState confirmState = courseService.getConfirmState(regDocRef);
    verifyDefault();
    assertEquals(CourseConfirmState.UNDEFINED, confirmState);
  }

}
