package com.celements.course.service;


public enum CourseConfirmState {

  CONFIRMED("confirmed"),
  PARTIALCONFIRMED("partialConfirmed"),
  UNCONFIRMED("unconfirmed"),
  CANCELLED("cancelled");
  
  public final String id;

  private CourseConfirmState(String id) {
    this.id = id;
  }

}
