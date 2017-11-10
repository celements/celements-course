package com.celements.course.classes;

import java.util.List;

import javax.annotation.concurrent.Immutable;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.celements.course.service.CourseConfirmState;
import com.celements.course.service.CourseConfirmStateMarshaller;
import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.EnumListField;

@Immutable
@Singleton
@Component(CourseParticipantClass.CLASS_DEF_HINT)
public class CourseParticipantClass extends AbstractClassDefinition implements CelCourseClass {

  public static final String CLASS_NAME = "CourseParticipantClass";
  public static final String CLASS_DEF_HINT = CLASS_SPACE + "." + CLASS_NAME;

  public static final ClassField<String> FIELD_COURSE_ID = new StringField.Builder(CLASS_DEF_HINT,
      "eventid").prettyName("Course ID").size(30).build();

  public static final ClassField<String> FIELD_TITLE = new StringField.Builder(CLASS_DEF_HINT,
      "title").prettyName("Title").size(30).build();

  public static final ClassField<String> FIELD_FIRST_NAME = new StringField.Builder(CLASS_DEF_HINT,
      "firstname").prettyName("Firstname").size(30).build();

  public static final ClassField<String> FIELD_LAST_NAME = new StringField.Builder(CLASS_DEF_HINT,
      "lastname").prettyName("Lastname").size(30).build();

  public static final ClassField<String> FIELD_ADDRESS = new StringField.Builder(CLASS_DEF_HINT,
      "address").prettyName("Address").size(30).build();

  public static final ClassField<String> FIELD_ZIP = new StringField.Builder(CLASS_DEF_HINT,
      "zip").prettyName("Zip").size(30).build();

  public static final ClassField<String> FIELD_CITY = new StringField.Builder(CLASS_DEF_HINT,
      "city").prettyName("City").size(30).build();

  public static final ClassField<String> FIELD_PHONE = new StringField.Builder(CLASS_DEF_HINT,
      "phone").prettyName("Phone").size(30).build();

  public static final ClassField<String> FIELD_EMAIL = new StringField.Builder(CLASS_DEF_HINT,
      "email").prettyName("Email").size(30).build();

  public static final ClassField<List<CourseConfirmState>> FIELD_STATUS = new EnumListField.Builder<>(
      CLASS_DEF_HINT, "status", new CourseConfirmStateMarshaller(
          CourseConfirmState.class)).prettyName("Status").separator("|").build();

  // TODO ClassField definitions incomplete
  // [CELDEV-577] Refactor CourseClasses to ClassDefinitions

  @Override
  public String getName() {
    return CLASS_DEF_HINT;
  }

  @Override
  public boolean isInternalMapping() {
    return true;
  }

  @Override
  protected String getClassSpaceName() {
    return CLASS_SPACE;
  }

  @Override
  protected String getClassDocName() {
    return CLASS_NAME;
  }
}
