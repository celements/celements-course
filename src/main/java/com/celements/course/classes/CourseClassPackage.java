package com.celements.course.classes;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.model.classes.AbstractLegacyClassPackage;
import com.celements.model.classes.ClassDefinition;

@Component(CourseClassPackage.NAME)
public class CourseClassPackage extends AbstractLegacyClassPackage {

  public static final String NAME = "course";

  @Requirement
  private List<CelCourseClass> classDefs;

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public List<? extends ClassDefinition> getClassDefinitions() {
    return new ArrayList<>(classDefs);
  }

  @Override
  public String getLegacyName() {
    return "celCourse";
  }

}
