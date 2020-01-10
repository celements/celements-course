package com.celements.course.classes;

import java.util.List;
import java.util.function.UnaryOperator;

import javax.annotation.concurrent.Immutable;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.EnumListField;
import com.celements.model.classes.fields.list.single.DBSingleListField;

@Immutable
@Singleton
@Component(CourseParticipantClass.CLASS_DEF_HINT)
public class CourseParticipantClass extends AbstractClassDefinition implements CelCourseClass {

  public static final String CLASS_NAME = "CourseParticipantClass";
  public static final String CLASS_DEF_HINT = CLASS_SPACE + "." + CLASS_NAME;

  public enum ParticipantStatus {
    unconfirmed, confirmed, cancelled, duplicate;
  }

  public static final UnaryOperator<String> DOC_IN_SPACE_DB_LIST_HQL = space -> "SELECT "
      + "doc.fullName, doc.title FROM XWikiDocument doc WHERE doc.space='" + space
      + "' ORDER BY doc.title ASC";

  public static final ClassField<String> FIELD_COURSE_ID = new StringField.Builder(CLASS_DEF_HINT,
      "eventid").prettyName("Course ID").size(30).build();

  public static final ClassField<String> FIELD_TITLE = new StringField.Builder(CLASS_DEF_HINT,
      "title").prettyName("Title").size(30).validationRegExp("/^.{0,8}$/").validationMessage(
          "cel_course_validation_titleToLong").build();

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

  public static final ClassField<List<ParticipantStatus>> FIELD_STATUS = new EnumListField.Builder<>(
      CLASS_DEF_HINT, "status", ParticipantStatus.class).prettyName("Status").separator(
          "|").build();

  public static final ClassField<String> FIELD_PAYMENT_METHOD = new DBSingleListField.Builder(
      CLASS_DEF_HINT, "payment_method").sql(DOC_IN_SPACE_DB_LIST_HQL.apply("PaymentMethods"))
          .build();

  public static final ClassField<String> FIELD_PARTICIPANCE_CATEGORY = new DBSingleListField.Builder(
      CLASS_DEF_HINT, "participance_category").sql(DOC_IN_SPACE_DB_LIST_HQL.apply(
          "ParticipanceCategories")).build();

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
