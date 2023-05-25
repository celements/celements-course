package com.celements.course.classes;

import javax.annotation.concurrent.Immutable;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.LargeStringField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.number.IntField;

@Immutable
@Singleton
@Component(CourseLevelClass.CLASS_DEF_HINT)
public class CourseLevelClass extends AbstractClassDefinition implements CelCourseClass {

  public static final String DOC_NAME = "CourseLevelClass";
  public static final String CLASS_DEF_HINT = CelCourseClass.SPACE_NAME + "." + DOC_NAME;
  public static final ClassReference CLASS_REF = new ClassReference(SPACE_NAME, DOC_NAME);

  public static final ClassField<String> FIELD_NAME = new StringField.Builder(
      CLASS_REF, "levelName").prettyName("Course Type Name").build();

  public static final ClassField<String> FIELD_SHORT_NAME = new StringField.Builder(
      CLASS_REF, "shortName").prettyName("Course Type Short Name").build();

  public static final ClassField<String> FIELD_IMAGE_PATH = new StringField.Builder(
      CLASS_REF, "level_img_path").prettyName("Course Type Image Path").build();

  public static final ClassField<Integer> FIELD_POSITION = new IntField.Builder(
      CLASS_REF, "levelPos").prettyName("order position").build();

  public static final ClassField<String> FIELD_DETAILS = new LargeStringField.Builder(
      CLASS_REF, "details").build();

  public CourseLevelClass() {
    super(CLASS_REF);
  }

  @Override
  public String getName() {
    return CLASS_DEF_HINT;
  }

  @Override
  public boolean isInternalMapping() {
    return true;
  }
}
