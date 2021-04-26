package com.celements.course.registration;

import static com.celements.common.test.CelementsTestUtils.*;
import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;

public class PersonTest extends AbstractComponentTest {

  private Person person;

  @Before
  public void setUp_PersonTest() throws Exception {
    person = new Person();
  }

  @Test
  public void testGetStatus_null_Status() {
    replayDefault();
    assertEquals(Optional.empty(), person.getStatus());
    verifyDefault();
  }

}
