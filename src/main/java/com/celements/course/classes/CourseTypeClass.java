package com.celements.course.classes;

import javax.annotation.concurrent.Immutable;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.LargeStringField;
import com.celements.model.classes.fields.StringField;

@Immutable
@Singleton
@Component(CourseTypeClass.CLASS_DEF_HINT)
public class CourseTypeClass extends AbstractClassDefinition implements CelCourseClass {

  public static final String DOC_NAME = "CourseTypeClass";
  public static final String CLASS_DEF_HINT = SPACE_NAME + "." + DOC_NAME;
  public static final ClassReference CLASS_REF = new ClassReference(SPACE_NAME, DOC_NAME);

  public static final ClassField<String> FIELD_NAME = new StringField.Builder(
      CLASS_REF, "typeName").prettyName("Course Type Name").validationRegExp("/.{1,255}/")
          .validationMessage("CourseClass.CourseTypeClass_typeName_notEmpty").build();

  public static final ClassField<String> FIELD_SHORT_NAME = new StringField.Builder(
      CLASS_REF, "shortName").prettyName("Course Type Short Name").build();

  public static final ClassField<String> FIELD_PREFIX = new StringField.Builder(
      CLASS_REF, "prefix").build();

  public static final ClassField<String> FIELD_IMAGE_PATH = new StringField.Builder(
      CLASS_REF, "type_img_path").prettyName("Course Type Image Path").build();

  public static final ClassField<String> FIELD_DETAILS = new LargeStringField.Builder(
      CLASS_REF, "details").build();

  public CourseTypeClass() {
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
