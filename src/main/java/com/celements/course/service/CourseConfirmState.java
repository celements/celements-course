package com.celements.course.service;

import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;

public enum CourseConfirmState {

  UNDEFINED, CONFIRMED, PARTIALCONFIRMED, UNCONFIRMED;

  @NotNull
  public static Optional<CourseConfirmState> convertStringToEnum(@NotNull String name) {
    try {
      return Optional.of(CourseConfirmState.valueOf(name.toUpperCase()));
    } catch (IllegalArgumentException exp) {
      return Optional.absent();
    }
  }

}
