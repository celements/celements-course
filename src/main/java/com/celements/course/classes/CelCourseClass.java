package com.celements.course.classes;

import java.util.function.UnaryOperator;

import org.xwiki.component.annotation.ComponentRole;

import com.celements.model.classes.ClassDefinition;

@ComponentRole
public interface CelCourseClass extends ClassDefinition {

  String SPACE_NAME = "CourseClasses";

  UnaryOperator<String> HQL_DOC_IN_SPACE = space -> "SELECT DISTINCT doc.fullName, doc.title "
      + "FROM XWikiDocument doc WHERE doc.space='" + space + "' and doc.name <> 'WebPreferences' "
      + "ORDER BY doc.title ASC";

}
