package com.celements.course.classes;

import java.util.Date;

import javax.annotation.concurrent.Immutable;
import javax.inject.Singleton;

import org.apache.commons.lang.RandomStringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.marshalling.EnumMarshaller;
import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.DateField;
import com.celements.model.classes.fields.LargeStringField;
import com.celements.model.classes.fields.PasswordField;
import com.celements.model.classes.fields.PasswordField.StorageType;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.DisplayType;
import com.celements.model.classes.fields.list.single.DBSingleListField;
import com.celements.model.classes.fields.list.single.EnumSingleListField;
import com.celements.model.classes.fields.number.IntField;

@Immutable
@Singleton
@Component(CourseParticipantClass.CLASS_DEF_HINT)
public class CourseParticipantClass extends AbstractClassDefinition implements CelCourseClass {

  public static final String DOC_NAME = "CourseParticipantClass";
  public static final String CLASS_DEF_HINT = SPACE_NAME + "." + DOC_NAME;
  public static final ClassReference CLASS_REF = new ClassReference(SPACE_NAME, DOC_NAME);

  public enum ParticipantStatus {

    unconfirmed, confirmed, cancelled, duplicate;

  }

  public enum PaymentStatus {

    unpayed("unpayed"), payed("payed"), partial("partially paid");

    public final String id;

    private PaymentStatus(String id) {
      this.id = id;
    }
  }

  public static final ClassField<String> FIELD_COURSE_ID = new StringField.Builder(
      CLASS_REF, "eventid").prettyName("Course ID").build();

  public static final ClassField<String> FIELD_TITLE = new StringField.Builder(
      CLASS_REF, "title").validationRegExp("/^.{0,8}$/")
          .validationMessage("cel_course_validation_titleToLong").build();

  public static final ClassField<String> FIELD_FIRST_NAME = new StringField.Builder(
      CLASS_REF, "firstname").build();

  public static final ClassField<String> FIELD_LAST_NAME = new StringField.Builder(
      CLASS_REF, "lastname").build();

  public static final ClassField<String> FIELD_ADDRESS = new StringField.Builder(
      CLASS_REF, "address").build();

  public static final ClassField<String> FIELD_ZIP = new StringField.Builder(
      CLASS_REF, "zip").build();

  public static final ClassField<String> FIELD_CITY = new StringField.Builder(
      CLASS_REF, "city").build();

  public static final ClassField<String> FIELD_PHONE = new StringField.Builder(
      CLASS_REF, "phone").build();

  public static final ClassField<String> FIELD_EMAIL = new StringField.Builder(
      CLASS_REF, "email").build();

  public static final ClassField<Date> FIELD_DOB = new DateField.Builder(
      CLASS_REF, "dob").prettyName("Date of birth (dd.MM.yyyy)").dateFormat("dd.MM.yyyy").build();

  public static final ClassField<ParticipantStatus> FIELD_STATUS = new EnumSingleListField.Builder<>(
      CLASS_REF, "status", ParticipantStatus.class).displayType(DisplayType.select).build();

  public static final ClassField<PaymentStatus> FIELD_PAYED = new EnumSingleListField.Builder<>(
      CLASS_REF, "payed", new EnumMarshaller<>(PaymentStatus.class, enm -> enm.id))
          .displayType(DisplayType.select).build();

  public static final ClassField<String> FIELD_PARTIAL_PAYED_REASON = new StringField.Builder(
      CLASS_REF, "partial_payed_reason").build();

  public static final ClassField<Integer> FIELD_PAYED_AMOUNT = new IntField.Builder(
      CLASS_REF, "payed_amount").build();

  public static final ClassField<Date> FIELD_PAYED_DATE = new DateField.Builder(
      CLASS_REF, "payedDate").prettyName("Payed Date (dd.MM.yyyy)").dateFormat("dd.MM.yyyy")
          .build();

  public static final String SPACE_PAYMENT_METHOD = "PaymentMethods";

  public static final ClassField<String> FIELD_PAYMENT_METHOD = new DBSingleListField.Builder(
      CLASS_REF, "payment_method").sql(HQL_DOC_IN_SPACE.apply(SPACE_PAYMENT_METHOD)).build();

  public static final ClassField<String> FIELD_PARTICIPANCE_CATEGORY = new DBSingleListField.Builder(
      CLASS_REF, "participance_category").sql(HQL_DOC_IN_SPACE.apply("ParticipanceCategories"))
          .build();

  public static final ClassField<String> FIELD_COMMENT = new LargeStringField.Builder(
      CLASS_REF, "comment").build();

  public static final ClassField<String> FIELD_VALIDATION_KEY = new PasswordField.Builder(
      CLASS_REF, "validkey").storageType(StorageType.Hash).prettyName("Validation Key").build();

  public static final ClassField<Date> FIELD_TIMESTAMP = new DateField.Builder(
      CLASS_REF, "timestamp").emptyIsToday(1).build();

  public static final ClassField<String> FIELD_CLIENT = new StringField.Builder(
      CLASS_REF, "client").prettyName("Client Info").build();

  public CourseParticipantClass() {
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

  public static final String generateNewValidationKey() {
    return RandomStringUtils.randomAlphanumeric(24);
  }
}
