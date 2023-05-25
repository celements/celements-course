package com.celements.course.classes;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

import javax.annotation.concurrent.Immutable;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.ClassReference;

import com.celements.model.classes.AbstractClassDefinition;
import com.celements.model.classes.fields.ClassField;
import com.celements.model.classes.fields.DateField;
import com.celements.model.classes.fields.LargeStringField;
import com.celements.model.classes.fields.StringField;
import com.celements.model.classes.fields.list.DBListField;
import com.celements.model.classes.fields.list.single.DBSingleListField;
import com.celements.model.classes.fields.number.IntField;

@Immutable
@Singleton
@Component(CourseClass.CLASS_DEF_HINT)
public class CourseClass extends AbstractClassDefinition implements CelCourseClass {

  public static final String DOC_NAME = "CourseClass";
  public static final String CLASS_DEF_HINT = CelCourseClass.SPACE_NAME + "." + DOC_NAME;
  public static final ClassReference CLASS_REF = new ClassReference(SPACE_NAME, DOC_NAME);

  private static final String REGEX_DATE_NON_EMPTY = "/^((0[1-9]|[12][0-9]|3[01])\\.(0[1-9]|1[012])\\.([0-9]{4}))$/";

  private static final Function<ClassReference, String> HQL_DOC_BY_OBJ = classRef -> {
    String className = classRef.getName().replace("Class", "");
    String classId = className.replace("Course", "").toLowerCase(); // level / type
    return "select distinct doc.fullName, map." + classId + "Name "
        + "from XWikiDocument as doc, BaseObject as obj, " + classRef.serialize() + " as map "
        + "where doc.translation=0 and doc.space='" + className + "' and doc.fullName=obj.name "
        + "and obj.id=map.id and obj.className='" + classRef.serialize() + "' "
        + "order by map." + classId + (classId.equals("level") ? "Pos" : "Name");
  };

  public static final ClassField<String> FIELD_TYPE = new DBSingleListField.Builder(
      CLASS_REF, "type").sql(HQL_DOC_BY_OBJ.apply(CourseTypeClass.CLASS_REF)).build();

  public static final ClassField<String> FIELD_LEVEL = new DBSingleListField.Builder(
      CLASS_REF, "level").sql(HQL_DOC_BY_OBJ.apply(CourseLevelClass.CLASS_REF)).build();

  public static final ClassField<String> FIELD_NUMBER = new StringField.Builder(
      CLASS_REF, "number").build();

  public static final ClassField<Integer> FIELD_SEATS = new IntField.Builder(
      CLASS_REF, "seats").build();

  public static final ClassField<String> FIELD_INFO = new LargeStringField.Builder(
      CLASS_REF, "info").build();

  public static final ClassField<Integer> FIELD_PRICE = new IntField.Builder(
      CLASS_REF, "price")
          .validationRegExp("/^0*[0-9]{1,9}$/")
          .validationMessage("cel_course_validation_price")
          .build();

  public static final ClassField<String> FIELD_PRICE_INFO = new StringField.Builder(
      CLASS_REF, "priceInfo").build();

  public static final ClassField<List<String>> FIELD_TEACHER = new DBListField.Builder(
      CLASS_REF, "teacher").multiSelect(true).sql(HQL_DOC_IN_SPACE.apply("Teachers")).build();

  public static final ClassField<Date> FIELD_START_TIMESTAMP = new DateField.Builder(
      CLASS_REF, "startTimeStamp").prettyName("Start Timestamp (dd.MM.yyyy)")
          .dateFormat("dd.MM.yyyy")
          .validationRegExp(REGEX_DATE_NON_EMPTY)
          .validationMessage("cel_course_validation_startTimeStamp")
          .build();

  public static final ClassField<Date> FIELD_END_TIMESTAMP = new DateField.Builder(
      CLASS_REF, "endTimeStamp").prettyName("End Timestamp (dd.MM.yyyy)")
          .dateFormat("dd.MM.yyyy")
          .validationRegExp(REGEX_DATE_NON_EMPTY)
          .validationMessage("cel_course_validation_endTimeStamp")
          .build();

  public CourseClass() {
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
