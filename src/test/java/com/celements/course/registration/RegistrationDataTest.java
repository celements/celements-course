package com.celements.course.registration;

import static org.junit.Assert.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.celements.common.test.AbstractComponentTest;

public class RegistrationDataTest extends AbstractComponentTest {

  private RegistrationData regData;

  @Before
  public void setUp_RegistrationDataTest() throws Exception {
    regData = new RegistrationData();
  }

  @Test
  public void testGetValidationKey() {
    Optional<String> validKey = regData.getValidationKey();
    assertTrue(validKey.isPresent());
    assertEquals(24, validKey.get().length());
    assertEquals("remains the same", validKey.get(), regData.getValidationKey().get());
  }

}
