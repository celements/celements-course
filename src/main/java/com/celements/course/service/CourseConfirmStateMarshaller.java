package com.celements.course.service;

import com.celements.marshalling.EnumMarshaller;
import com.google.common.base.Optional;

public class CourseConfirmStateMarshaller extends EnumMarshaller<CourseConfirmState> {

  public CourseConfirmStateMarshaller(Class<CourseConfirmState> token) {
    super(token);
  }

  @Override
  public String serialize(CourseConfirmState val) {
    return super.serialize(val).toLowerCase();
  }

  @Override
  public Optional<CourseConfirmState> resolve(String val) {
    return super.resolve(val.toUpperCase());
  }

}
