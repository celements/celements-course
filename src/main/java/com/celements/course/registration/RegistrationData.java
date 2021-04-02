package com.celements.course.registration;

import static com.google.common.base.Predicates.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.concurrent.NotThreadSafe;

import org.xwiki.model.reference.DocumentReference;

import com.google.common.base.Strings;
import com.xpn.xwiki.web.XWikiRequest;

@NotThreadSafe
public class RegistrationData {

  private String eventid;
  private List<Person> persons;
  private String comment;
  private String validationKey;
  private String mainEmail;
  private String paymentMethod;
  private String participanceCategory;
  private DocumentReference regDocRef;
  private int price;

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

  public String getPaymentMethod() {
    return paymentMethod;
  }

  public void setPaymentMethod(String paymentMethod) {
    this.paymentMethod = paymentMethod;
  }

  public String getParticipanceCategory() {
    return participanceCategory;
  }

  public void setParticipanceCategory(String participanceCategory) {
    this.participanceCategory = participanceCategory;
  }

  public int getPrice() {
    return price;
  }

  public void setPrice(int price) {
    this.price = price;
  }

  public Optional<String> getValidationKey() {
    return Optional.ofNullable(validationKey).filter(not(String::isEmpty));
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
    setPaymentMethod(req.get("payment_method"));
    setParticipanceCategory(req.get("participance_category"));
    persons = new ArrayList<>();
    for (int i = 0; getEndCondition(req.getParameterValues("title"), i); i++) {
      getSetPerson(persons, i).setTitle(req.getParameterValues("title")[i]);
    }
    for (int i = 0; getEndCondition(req.getParameterValues("givenName"), i); i++) {
      getSetPerson(persons, i).setGivenName(req.getParameterValues("givenName")[i]);
    }
    for (int i = 0; getEndCondition(req.getParameterValues("surname"), i); i++) {
      getSetPerson(persons, i).setSurname(req.getParameterValues("surname")[i]);
    }
    for (int i = 0; getEndCondition(req.getParameterValues("email"), i); i++) {
      getSetPerson(persons, i).setEmail(req.getParameterValues("email")[i]);
    }
    for (int i = 0; getEndCondition(req.getParameterValues("addressEqualsMain"), i); i++) {
      int idx = i;
      if ((i > 0) && "1".equals(req.getParameterValues("addressEqualsMain"))) {
        idx = 0;
      }
      if (req.getParameterValues("street").length > idx) {
        getSetPerson(persons, i).setAddress(req.getParameterValues("street")[idx]);
      }
      if (req.getParameterValues("zip").length > idx) {
        getSetPerson(persons, i).setZip(req.getParameterValues("zip")[idx]);
      }
      if (req.getParameterValues("city").length > idx) {
        getSetPerson(persons, i).setCity(req.getParameterValues("city")[idx]);
      }
    }
    for (int i = 0; getEndCondition(req.getParameterValues("phone"), i); i++) {
      getSetPerson(persons, i).setPhone(req.getParameterValues("phone")[i]);
    }
    for (int i = 0; getEndCondition(req.getParameterValues("dob"), i); i++) {
      getSetPerson(persons, i).setDateOfBirth(req.getParameterValues("dob")[i]);
    }
    for (int i = 0; getEndCondition(req.getParameterValues("status"), i); i++) {
      getSetPerson(persons, i).setStatus(req.getParameterValues("status")[i]);
    }
  }

  boolean getEndCondition(String[] strs, int i) {
    return (strs != null) && (i < strs.length);
  }

  Person getSetPerson(List<Person> personList, int i) {
    if (personList.size() <= i) {
      personList.add(new Person());
    }
    Person person = personList.get(i);
    return person;
  }

  @Override
  public String toString() {
    return "RegistrationData [eventid=" + eventid + ", comment=" + comment + ", mainEmail="
        + getMainEmail() + ", regDocRef=" + regDocRef + ", persons=" + persons + ", paymentMethod="
        + paymentMethod + ", participanceCategory=" + participanceCategory + "]";
  }

}
