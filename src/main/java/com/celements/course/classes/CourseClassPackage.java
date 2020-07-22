package com.celements.course.classes;

import java.util.List;
import java.util.function.Supplier;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import com.celements.model.classes.AbstractLegacyClassPackage;
import com.celements.model.classes.ClassDefinition;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;

@Component(CourseClassPackage.NAME)
public class CourseClassPackage extends AbstractLegacyClassPackage {

  public static final String NAME = "course";

  @Requirement
  private List<CelCourseClass> classDefsMutable;

  private final Supplier<ImmutableList<CelCourseClass>> classDefs = Suppliers
      .memoize(() -> ImmutableList.copyOf(classDefsMutable));

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public String getLegacyName() {
    return "celCourse";
  }

  @Override
  public List<? extends ClassDefinition> getClassDefinitions() {
    return classDefs.get();
  }

}
