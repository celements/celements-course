package com.celements.course.registration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.xwiki.model.reference.DocumentReference;

import com.google.common.base.Strings;
import com.xpn.xwiki.web.XWikiRequest;

public class RegistrationData {

  private String eventid;
  private List<Person> persons;
  private String comment;
  private String validationKey;
  private String mainEmail;
  private DocumentReference regDocRef;

  public RegistrationData() {
  }

  public String getEventid() {
    return eventid;
  }

  public RegistrationData(XWikiRequest req) {
    setData(req);
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getMainEmail() {
    if (Strings.isNullOrEmpty(mainEmail) && (getPersons().size() > 0)) {
      mainEmail = getPersons().get(0).getEmail();
    }
    return mainEmail;
  }

  public void setMainEmail(String email) {
    this.mainEmail = email;
  }

  public String getValidationKey() {
    if (Strings.isNullOrEmpty(validationKey)) {
      validationKey = RandomStringUtils.randomAlphanumeric(24);
    }
    return validationKey;
  }

  public void setValidationKey(String validationKey) {
    this.validationKey = validationKey;
  }

  public DocumentReference getRegDocRef() {
    return regDocRef;
  }

  public void setRegDocRef(DocumentReference regDocRef) {
    this.regDocRef = regDocRef;
  }

  public void setEventid(String eventid) {
    this.eventid = eventid;
  }

  public List<Person> getPersons() {
    if ((persons != null)) {
      return persons;
    }
    return Collections.emptyList();
  }

  public void setData(XWikiRequest req) {
    setEventid(req.get("eventid"));
    setComment(req.get("comment"));
    persons = new ArrayList<>();
    for (int i = 0; i < req.getParameterValues("title").length; i++) {
      getSetPerson(persons, i).setTitle(req.getParameterValues("title")[i]);
    }
    for (int i = 0; i < req.getParameterValues("givenName").length; i++) {
      getSetPerson(persons, i).setGivenName(req.getParameterValues("givenName")[i]);
    }
    for (int i = 0; i < req.getParameterValues("title").length; i++) {
      getSetPerson(persons, i).setSurname(req.getParameterValues("title")[i]);
    }
    for (int i = 0; i < req.getParameterValues("email").length; i++) {
      getSetPerson(persons, i).setEmail(req.getParameterValues("email")[i]);
    }
    for (int i = 0; i < req.getParameterValues("addressEqualsMain").length; i++) {
      int idx = i;
      if ((i > 0) && "1".equals(req.getParameterValues("addressEqualsMain"))) {
        idx = 0;
      }
      getSetPerson(persons, i).setAddress(req.getParameterValues("street")[idx]);
      getSetPerson(persons, i).setZip(req.getParameterValues("zip")[idx]);
      getSetPerson(persons, i).setCity(req.getParameterValues("city")[idx]);
    }
    for (int i = 0; i < req.getParameterValues("phone").length; i++) {
      getSetPerson(persons, i).setPhone(req.getParameterValues("phone")[i]);
    }
    for (int i = 0; i < req.getParameterValues("dob").length; i++) {
      getSetPerson(persons, i).setDateOfBirth(req.getParameterValues("dob")[i]);
    }
    for (int i = 0; i < req.getParameterValues("status").length; i++) {
      getSetPerson(persons, i).setStatus(req.getParameterValues("status")[i]);
    }
  }

  Person getSetPerson(List<Person> personList, int i) {
    if (personList.size() <= i) {
      personList.add(new Person());
    }
    Person person = personList.get(i);
    return person;
  }

}