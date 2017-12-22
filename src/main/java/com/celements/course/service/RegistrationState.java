package com.celements.course.service;

import javax.validation.constraints.NotNull;

import com.google.common.base.Optional;

public enum RegistrationState {

  UNDEFINED,
  CONFIRMED,
  PARTIALCONFIRMED,
  UNCONFIRMED,
  CANCELLED,
  DUPLICATE;

  @NotNull
  public static Optional<RegistrationState> convertStringToEnum(@NotNull String name) {
    try {
      return Optional.of(RegistrationState.valueOf(name.toUpperCase()));
    } catch (IllegalArgumentException exp) {
      return Optional.absent();
    }
  }

}
