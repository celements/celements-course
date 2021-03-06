package com.celements.course.registration;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.annotation.concurrent.NotThreadSafe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.celements.course.classes.CourseParticipantClass.ParticipantStatus;
import com.celements.course.service.ICourseServiceRole;
import com.google.common.base.Enums;
import com.google.common.base.Strings;
import com.xpn.xwiki.web.Utils;

@NotThreadSafe
public class Person {

  private static final Logger LOGGER = LoggerFactory.getLogger(Person.class);

  private String title;
  private String givenName;
  private String surname;
  private String address;
  private String zip;
  private String city;
  private String phone;
  private String email;
  private Date dateOfBirth;
  private String status;

  public boolean isEmpty() {
    return Strings.isNullOrEmpty(title) && Strings.isNullOrEmpty(givenName)
        && Strings.isNullOrEmpty(surname) && Strings.isNullOrEmpty(address)
        && Strings.isNullOrEmpty(zip) && Strings.isNullOrEmpty(city) && Strings.isNullOrEmpty(phone)
        && Strings.isNullOrEmpty(email) && (dateOfBirth == null) && Strings.isNullOrEmpty(status);
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getSurname() {
    return surname;
  }

  public void setSurname(String surname) {
    this.surname = surname;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getZip() {
    return zip;
  }

  public void setZip(String zip) {
    this.zip = zip;
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getEmail() {
    return getCourseService().normalizeEmail(email);
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Date getDateOfBirth() {
    return dateOfBirth;
  }

  /**
   * @param dateOfBirth
   *          expects as format dd.MM.yyyy
   */
  public void setDateOfBirth(String dateOfBirth) {
    String expectedFormat = "dd.MM.yyyy";
    SimpleDateFormat sdf = new SimpleDateFormat(expectedFormat);
    try {
      this.dateOfBirth = sdf.parse(dateOfBirth);
    } catch (ParseException pe) {
      LOGGER.warn("date of birth input did not match expected format [{}], but was [{}]",
          expectedFormat, dateOfBirth);
    }
  }

  public void setDateOfBirth(Date dateOfBirth) {
    this.dateOfBirth = dateOfBirth;
  }

  public ParticipantStatus getStatus() {
    return Enums.getIfPresent(ParticipantStatus.class, Strings.nullToEmpty(status))
        .or(ParticipantStatus.unconfirmed);
  }

  public void setStatus(String status) {
    this.status = status;
  }

  private ICourseServiceRole getCourseService() {
    return Utils.getComponent(ICourseServiceRole.class);
  }

  @Override
  public String toString() {
    return "Person [title=" + title + ", givenName=" + givenName + ", surname=" + surname
        + ", address=" + address + ", zip=" + zip + ", city=" + city + ", phone=" + phone
        + ", email=" + getEmail() + ", dateOfBirth=" + dateOfBirth + ", status=" + getStatus()
        + "]";
  }

}
