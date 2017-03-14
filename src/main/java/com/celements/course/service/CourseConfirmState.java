package com.celements.course.service;

import com.google.common.base.Optional;

public enum CourseConfirmState {

  CONFIRMED, PARTIALCONFIRMED, UNCONFIRMED;

  public static Optional<CourseConfirmState> convertStringToEnum(String name) {
    try {
      return Optional.fromNullable(CourseConfirmState.valueOf(name.toUpperCase()));
    } catch (IllegalArgumentException exp) {
      return Optional.absent();
    }
  }

}
