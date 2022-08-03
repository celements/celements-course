package com.celements.course.service;

import static com.celements.common.lambda.LambdaExceptionUtil.*;
import static com.celements.course.classes.CourseParticipantClass.*;
import static com.celements.model.util.ReferenceSerializationMode.*;
import static com.celements.rights.access.EAccessLevel.*;
import static com.google.common.base.Preconditions.*;
import static com.google.common.base.Predicates.*;
import static com.google.common.collect.ImmutableList.*;
import static com.google.common.collect.Maps.*;
import static java.util.stream.Collectors.*;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.model.reference.ClassReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ImmutableObjectReference;
import org.xwiki.model.reference.SpaceReference;

import com.celements.auth.user.User;
import com.celements.copydoc.ICopyDocumentRole;
import com.celements.course.classes.CourseClass;
import com.celements.course.classes.CourseParticipantClass;
import com.celements.course.classes.CourseParticipantClass.Attendance;
import com.celements.course.classes.CourseParticipantClass.ParticipantStatus;
import com.celements.course.classes.CourseParticipantClass.PaymentStatus;
import com.celements.course.classes.CourseTypeClass;
import com.celements.course.registration.Person;
import com.celements.course.registration.RegistrationData;
import com.celements.mailsender.IMailSenderRole;
import com.celements.model.access.IModelAccessFacade;
import com.celements.model.access.exception.DocumentAccessException;
import com.celements.model.access.exception.DocumentNotExistsException;
import com.celements.model.access.exception.DocumentSaveException;
import com.celements.model.context.ModelContext;
import com.celements.model.field.FieldAccessor;
import com.celements.model.field.XObjectFieldAccessor;
import com.celements.model.object.xwiki.XWikiObjectEditor;
import com.celements.model.object.xwiki.XWikiObjectFetcher;
import com.celements.model.reference.RefBuilder;
import com.celements.model.util.ModelUtils;
import com.celements.nextfreedoc.INextFreeDocRole;
import com.celements.rendering.RenderCommand;
import com.celements.search.lucene.ILuceneSearchService;
import com.celements.search.lucene.LuceneSearchException;
import com.celements.search.lucene.LuceneSearchResult;
import com.celements.search.lucene.query.LuceneQuery;
import com.celements.web.classes.oldcore.XWikiGlobalRightsClass;
import com.celements.web.plugin.cmd.CelMailConfiguration;
import com.celements.web.plugin.cmd.ConvertToPlainTextException;
import com.celements.web.plugin.cmd.PlainTextCommand;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.classes.PasswordClass;
import com.xpn.xwiki.plugin.rightsmanager.RightsManager;
import com.xpn.xwiki.user.api.XWikiUser;
import com.xpn.xwiki.web.XWikiRequest;

import one.util.streamex.StreamEx;

@Component
public class CourseService implements ICourseServiceRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(CourseService.class);

  public static final String GROUP_SPACE = RightsManager.DEFAULT_USERORGROUP_SPACE;

  static final String CFGSRC_PARTICIPANT_DOC_NAME_PREFIX = "celements.course.participant.docNamePrefix";

  private static final Set<ParticipantStatus> DEFAULT_IGNORE_STATES = ImmutableSet.of(
      ParticipantStatus.cancelled, ParticipantStatus.duplicate);

  @Requirement
  private IModelAccessFacade modelAccess;

  @Requirement(XObjectFieldAccessor.NAME)
  private FieldAccessor<BaseObject> xObjFieldAccessor;

  @Requirement
  private ModelContext context;

  @Requirement
  private ModelUtils modelUtils;

  @Requirement
  private INextFreeDocRole nextFreeDoc;

  @Requirement
  private IMailSenderRole mailSender;

  @Requirement
  private ConfigurationSource cfgSrc;

  @Requirement
  private ILuceneSearchService searchService;

  @Requirement
  private ICopyDocumentRole copyDocService;

  @Override
  public DocumentReference getCourseTypeForCourse(DocumentReference courseDocRef) {
    DocumentReference typeDocRef = null;
    try {
      typeDocRef = XWikiObjectFetcher.on(modelAccess.getDocument(courseDocRef))
          .fetchField(CourseClass.FIELD_TYPE)
          .stream().findFirst()
          .map(typeFN -> modelUtils.resolveRef(typeFN, DocumentReference.class, courseDocRef))
          .orElse(null);
    } catch (DocumentNotExistsException dnee) {
      LOGGER.error("getCourseTypeForCourse: Course document {} does not exist", courseDocRef, dnee);
    }
    return typeDocRef;
  }

  @Override
  public String getCourseTypeName(DocumentReference courseTypeDocRef) {
    String typeName = "";
    try {
      typeName = XWikiObjectFetcher.on(modelAccess.getDocument(courseTypeDocRef))
          .fetchField(CourseTypeClass.FIELD_NAME)
          .stream().findFirst()
          .orElse("");
    } catch (DocumentNotExistsException dnee) {
      LOGGER.error("getCourseTypeName: Course document {} does not exist", courseTypeDocRef, dnee);
    }
    return typeName;
  }

  @Override
  public boolean registerParticipantFromRequest(boolean sendValidationMail) {
    XWikiRequest req = context.getRequest().get();
    LOGGER.trace("registerParticipantFromRequest: request '{}': {}", req.hashCode(), req);
    RegistrationData data = new RegistrationData();
    data.setData(req);
    if (!Strings.isNullOrEmpty(data.getEventid()) && !Strings.isNullOrEmpty(data.getMainEmail())) {
      DocumentReference courseDocRef = modelUtils.resolveRef(data.getEventid(),
          DocumentReference.class);
      try {
        data.setPrice(XWikiObjectFetcher.on(modelAccess.getDocument(courseDocRef)).fetchField(
            CourseClass.FIELD_PRICE).stream().findFirst().orElse(0));
        XWikiDocument regDoc = createRegistrationDoc(courseDocRef);
        data.setRegDocRef(regDoc.getDocumentReference());
        if (createParticipantObjects(regDoc, data)) {
          prepareRegistrationSpace(regDoc.getDocumentReference().getLastSpaceReference());
          modelAccess.saveDocument(regDoc, "created new registration");
          if (sendValidationMail) {
            sendValidationMails(data);
          }
        } else {
          LOGGER.warn("registerParticipantFromRequest: incomplete person data '{}'", data);
        }
        return true;
      } catch (DocumentAccessException | XWikiException | IllegalArgumentException excp) {
        LOGGER.error("exception while creating new registration", excp);
      }
    } else {
      LOGGER.info("registerParticipantFromRequest: request '{}' no eventid or email",
          req.hashCode());
    }
    return false;
  }

  private XWikiDocument createRegistrationDoc(DocumentReference courseDocRef)
      throws DocumentAccessException {
    modelAccess.getDocument(courseDocRef);
    XWikiDocument regDoc = modelAccess.createDocument(createParticipantDocRef(courseDocRef));
    context.getRequestParameter("template").toJavaUtil()
        .map(template -> modelUtils.resolveRef(template, DocumentReference.class))
        .filter(modelAccess::exists)
        .ifPresent(rethrowConsumer(templateDocRef -> {
          try {
            regDoc.readFromTemplate(templateDocRef, context.getXWikiContext());
          } catch (XWikiException exc) {
            throw new DocumentAccessException(templateDocRef, exc);
          }
        }));
    return regDoc;
  }

  @Override
  public boolean prepareRegistrationSpace(SpaceReference regSpaceRef,
      ClassReference... additionalGroups) {
    boolean success = true;
    DocumentReference webPrefRef = new DocumentReference("WebPreferences", regSpaceRef);
    if (!modelAccess.exists(webPrefRef)) {
      XWikiDocument webPrefDoc = modelAccess.getOrCreateDocument(webPrefRef);
      StreamEx.of(
          new ClassReference(GROUP_SPACE, "XWikiAdminGroup"),
          new ClassReference(GROUP_SPACE, "CourseEditorGroup"))
          .append(additionalGroups)
          .filter(Objects::nonNull)
          .distinct()
          .filter(ref -> modelAccess.exists(ref.getDocRef(webPrefRef.getWikiReference())))
          .forEach(ref -> addAccessRightsEdit(webPrefDoc, ref.serialize()));
      try {
        modelAccess.saveDocument(webPrefDoc, "prepareRegistrationSpace");
      } catch (DocumentSaveException dse) {
        LOGGER.error("Exception saving registration space WebPreferences document.", dse);
        success = false;
      }
    }
    DocumentReference webHomeRef = new DocumentReference("WebHome", regSpaceRef);
    if (!modelAccess.exists(webHomeRef)) {
      XWikiDocument webHomeDoc = modelAccess.getOrCreateDocument(webHomeRef);
      webHomeDoc.setContent("#parse('celMacros/getRegistrationListing.vm')");
      try {
        modelAccess.saveDocument(webHomeDoc, "prepareRegistrationSpace");
      } catch (DocumentSaveException dse) {
        LOGGER.error("Exception saving registration space WebHome document.", dse);
        success = false;
      }
    }
    return success;
  }

  BaseObject addAccessRightsEdit(XWikiDocument webPrefDoc, String groupName) {
    return XWikiObjectEditor.on(webPrefDoc)
        .filter(XWikiGlobalRightsClass.FIELD_GROUPS, ImmutableList.of(groupName))
        .filter(XWikiGlobalRightsClass.FIELD_LEVELS, ImmutableList.of(VIEW, EDIT, DELETE, UNDELETE))
        .filter(XWikiGlobalRightsClass.FIELD_USERS, ImmutableList.<XWikiUser>of())
        .filter(XWikiGlobalRightsClass.FIELD_ALLOW, true)
        .createFirstIfNotExists();
  }

  private boolean createParticipantObjects(XWikiDocument regDoc, RegistrationData data) {
    boolean participantAdded = false;
    List<Person> persons = data.getPersons();
    for (int nb = 0; nb < persons.size(); nb++) {
      Person person = persons.get(nb);
      if (!person.isEmpty()) {
        BaseObject obj = XWikiObjectEditor.on(regDoc)
            .filter(CourseParticipantClass.CLASS_REF).filter(nb)
            .createFirstIfNotExists();
        xObjFieldAccessor.set(obj, FIELD_COURSE_ID, data.getEventid());
        xObjFieldAccessor.set(obj, FIELD_TITLE, person.getTitle());
        xObjFieldAccessor.set(obj, FIELD_FIRST_NAME, person.getGivenName());
        xObjFieldAccessor.set(obj, FIELD_LAST_NAME, person.getSurname());
        xObjFieldAccessor.set(obj, FIELD_ADDRESS, person.getAddress());
        xObjFieldAccessor.set(obj, FIELD_ZIP, person.getZip());
        xObjFieldAccessor.set(obj, FIELD_CITY, person.getCity());
        xObjFieldAccessor.set(obj, FIELD_PHONE, person.getPhone());
        xObjFieldAccessor.set(obj, FIELD_EMAIL, person.getEmail());
        xObjFieldAccessor.set(obj, FIELD_DOB, person.getDateOfBirth());
        xObjFieldAccessor.set(obj, FIELD_ATTENDANCE, Attendance.yes);
        xObjFieldAccessor.set(obj, FIELD_STATUS, person.getStatus());
        xObjFieldAccessor.set(obj, FIELD_PAYED, PaymentStatus.unpayed);
        xObjFieldAccessor.set(obj, FIELD_PAYED_AMOUNT, data.getPrice());
        xObjFieldAccessor.set(obj, FIELD_COMMENT, data.getComment());
        // using set since there is no setPassword method
        xObjFieldAccessor.set(obj, FIELD_VALIDATION_KEY, data.getValidationKey());
        xObjFieldAccessor.set(obj, FIELD_TIMESTAMP, new Date());
        xObjFieldAccessor.set(obj, FIELD_CLIENT, getClientInfo());
        participantAdded = true;
      } else {
        LOGGER.info("createParticipantObjects: incomplete person '{}'", person);
      }
    }
    return participantAdded;
  }

  private void sendValidationMails(RegistrationData data) throws DocumentNotExistsException,
      XWikiException {
    HashSet<String> sentEmails = new HashSet<>();
    getVeloContext().put("registrationData", data);
    XWikiDocument emailContentDoc = modelAccess.getDocument(new DocumentReference(
        context.getWikiRef().getName(), "MailContent", "NeueAnmeldung"));
    for (Person person : data.getPersons()) {
      if (!person.isEmpty() && sentEmails.add(person.getEmail())) {
        sendMail(null, person, emailContentDoc, false);
      }
    }
  }

  @Override
  public String normalizeEmail(String emailAdr) {
    return Strings.nullToEmpty(emailAdr).toLowerCase().trim();
  }

  String getClientInfo() {
    StringBuilder clientInfo = new StringBuilder();
    clientInfo.append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
    clientInfo.append(" @ ");
    clientInfo.append(context.getRequest().get().getHttpServletRequest()
        .getHeader("user-agent"));
    return clientInfo.toString();
  }

  @Override
  public DocumentReference createParticipantDocRef(DocumentReference courseDocRef) {
    DocumentReference ret;
    SpaceReference spaceRef = getRegistrationSpace(checkNotNull(courseDocRef));
    String name = cfgSrc.getProperty(CFGSRC_PARTICIPANT_DOC_NAME_PREFIX, "");
    if (name.isEmpty()) {
      ret = nextFreeDoc.getNextUntitledPageDocRef(spaceRef);
    } else {
      ret = nextFreeDoc.getNextTitledPageDocRef(spaceRef, name);
    }
    return ret;
  }

  @Override
  public DocumentReference createParticipantDocRef(DocumentReference courseDocRef, User user) {
    checkNotNull(user);
    return RefBuilder.from(getRegistrationSpace(courseDocRef))
        .doc(user.getDocRef().getName())
        .build(DocumentReference.class);
  }

  @Override
  public SpaceReference getRegistrationSpace(DocumentReference courseDocRef) {
    checkNotNull(courseDocRef);
    String spaceName = Joiner.on("_").join(courseDocRef.getParent().getName(),
        courseDocRef.getName());
    return new SpaceReference(spaceName, courseDocRef.getWikiReference());
  }

  @Override
  public String passwordHashString(String str) {
    return new PasswordClass().getEquivalentPassword("hash:SHA-512:", str);
  }

  @Override
  public boolean validateParticipant(DocumentReference regDocRef, String emailAdr,
      String activationCode) {
    boolean isValidated;
    LOGGER.debug("validateParticipant: with regDoc [{}], email [{}] and activation code [{}]",
        regDocRef, emailAdr, activationCode);
    try {
      XWikiDocument regDoc = modelAccess.getDocument(regDocRef);
      isValidated = isParticipantInState(regDoc, emailAdr, ParticipantStatus.confirmed)
          || validateParticipant(regDoc, emailAdr, activationCode);
    } catch (DocumentAccessException | XWikiException exp) {
      LOGGER.error("validateParticipant: failed for [{}], [{}], [{}]", regDocRef, emailAdr,
          activationCode, exp);
      isValidated = false;
    }
    return isValidated;
  }

  private boolean validateParticipant(XWikiDocument regDoc, String emailAdr, String activationCode)
      throws DocumentSaveException, DocumentNotExistsException, XWikiException {
    List<BaseObject> editablePartiObjs = Stream.concat(
        XWikiObjectEditor.on(regDoc).filter(FIELD_EMAIL, emailAdr).fetch().stream(),
        XWikiObjectEditor.on(regDoc).filterAbsent(FIELD_EMAIL).fetch().stream())
        .collect(toList());
    boolean isValidated = validateOrRemoveParticipants(editablePartiObjs, activationCode);
    if (!editablePartiObjs.isEmpty()) {
      modelAccess.saveDocument(regDoc, "email address validated by link");
      for (BaseObject partiObj : editablePartiObjs) {
        sendConfirmationMail(partiObj, emailAdr);
      }
    }
    return isValidated;
  }

  private boolean validateOrRemoveParticipants(Iterable<BaseObject> partiObjs,
      String activationCode) {
    boolean isValidated = false;
    Iterator<BaseObject> iter = partiObjs.iterator();
    while (iter.hasNext()) {
      BaseObject obj = iter.next();
      if (validateParticipant(activationCode, obj)) {
        isValidated = true;
      } else {
        LOGGER.debug("activationCode does not match object key for obj [{}] on doc [{}]",
            obj.getNumber(), obj.getDocumentReference());
        iter.remove();
      }
    }
    return isValidated;
  }

  @Override
  public RegistrationState getConfirmState(DocumentReference regDocRef) {
    RegistrationState regState = null;
    try {
      XWikiDocument regDoc = modelAccess.getDocument(regDocRef);
      Set<ParticipantStatus> status = XWikiObjectFetcher.on(regDoc)
          .fetchField(CourseParticipantClass.FIELD_STATUS)
          .set();
      if (status.contains(ParticipantStatus.confirmed)) {
        if (!status.contains(ParticipantStatus.unconfirmed)) {
          regState = RegistrationState.CONFIRMED;
        } else {
          regState = RegistrationState.PARTIALCONFIRMED;
        }
      } else if (status.contains(ParticipantStatus.unconfirmed)) {
        regState = RegistrationState.UNCONFIRMED;
      } else if (status.contains(ParticipantStatus.cancelled)) {
        regState = RegistrationState.CANCELLED;
      } else if (status.contains(ParticipantStatus.duplicate)) {
        regState = RegistrationState.DUPLICATE;
      }
    } catch (DocumentNotExistsException exp) {
      LOGGER.info("Failed to get participants for docRef '{}' ", regDocRef);
    }
    return regState != null ? regState : RegistrationState.UNDEFINED;
  }

  @Override
  public List<DocumentReference> getRegistrationsForCourse(SpaceReference regSpaceRef,
      List<String> sortFields) throws LuceneSearchException {
    LuceneQuery query = searchService.createQuery();
    query.add(searchService.createSpaceRestriction(regSpaceRef));
    query.add(searchService.createObjectRestriction(CourseParticipantClass.CLASS_REF));
    LuceneSearchResult result = searchService.search(query, sortFields, null);
    return result.getResults(DocumentReference.class);
  }

  @Override
  public List<DocumentReference> getRegistrationsForCourse(DocumentReference courseDocRef)
      throws LuceneSearchException {
    return getRegistrationsForCourse(getRegistrationSpace(courseDocRef), null);
  }

  @Override
  public long getRegistrationCount(DocumentReference courseDocRef) throws LuceneSearchException {
    return getRegistrationCount(courseDocRef, null, null);
  }

  @Override
  public long getRegistrationCount(DocumentReference courseDocRef, ParticipantStatus state)
      throws LuceneSearchException {
    return getRegistrationCount(courseDocRef, state, null);
  }

  @Override
  public long getRegistrationCount(DocumentReference courseDocRef,
      List<ParticipantStatus> ignorList) throws LuceneSearchException {
    return getRegistrationCount(courseDocRef, null, ignorList);
  }

  private long getRegistrationCount(DocumentReference courseDocRef, ParticipantStatus state,
      Collection<ParticipantStatus> ignoreParticipantStates) throws LuceneSearchException {
    long retVal = 0;
    if (ignoreParticipantStates == null) {
      ignoreParticipantStates = DEFAULT_IGNORE_STATES;
    }
    for (DocumentReference registrationDocRef : getRegistrationsForCourse(courseDocRef)) {
      try {
        List<ParticipantStatus> values;
        if (state == null) {
          values = Stream.of(ParticipantStatus.values())
              .filter(not(ignoreParticipantStates::contains))
              .collect(ImmutableList.toImmutableList());
        } else {
          values = ImmutableList.of(state);
        }
        retVal += XWikiObjectFetcher.on(modelAccess.getDocument(registrationDocRef)).filter(
            FIELD_STATUS, values).count();
      } catch (DocumentNotExistsException exp) {
        LOGGER.info("Failed to get registrationDocRef '{}'", registrationDocRef, exp);
      }
    }
    return retVal;
  }

  private boolean validateParticipant(String activationCode, BaseObject partiObj) {
    String hashedCode = passwordHashString(activationCode);
    String savedHash = partiObj.getStringValue("validkey");
    if (hashedCode.equals(savedHash)) {
      xObjFieldAccessor.get(partiObj, FIELD_STATUS)
          .filter(not(DEFAULT_IGNORE_STATES::contains))
          .ifPresent(state -> xObjFieldAccessor.set(partiObj, FIELD_STATUS,
              ParticipantStatus.confirmed));
      return true;
    }
    return false;
  }

  private boolean isParticipantInState(XWikiDocument regDoc, String emailAdr,
      ParticipantStatus state) {
    return XWikiObjectFetcher.on(regDoc)
        .filter(FIELD_EMAIL, emailAdr)
        .filter(FIELD_STATUS, Arrays.asList(state))
        .exists();
  }

  @Override
  public Optional<Attendance> getAttendance(DocumentReference participantDocRef) {
    return XWikiObjectFetcher.on(modelAccess.getOrCreateDocument(participantDocRef))
        .fetchField(CourseParticipantClass.FIELD_ATTENDANCE)
        .stream().findAny();
  }

  @Override
  public List<ImmutableObjectReference> copyParticipants(DocumentReference targetCourseDocRef,
      Stream<ImmutableObjectReference> objRefs) {
    Map<DocumentReference, Set<Integer>> groupedByDoc = objRefs.filter(
        objRef -> objRef.getClassReference().equals(CourseParticipantClass.CLASS_REF)).collect(
            groupingBy(ImmutableObjectReference::getDocumentReference, LinkedHashMap::new, mapping(
                ImmutableObjectReference::getNumber, toSet())));
    List<BaseObject> copiedObjs = transformEntries(groupedByDoc,
        this::fetchParticipantObjs).values().stream().flatMap(
            participantObjs -> copyParticipantObjs(targetCourseDocRef, participantObjs)).collect(
                toImmutableList());
    if (!copiedObjs.isEmpty()) {
      prepareRegistrationSpace(getRegistrationSpace(targetCourseDocRef));
    }
    return copiedObjs.stream().map(ImmutableObjectReference::from).collect(toImmutableList());
  }

  private List<BaseObject> fetchParticipantObjs(DocumentReference participantDocRef,
      Set<Integer> objNbs) {
    XWikiDocument participantDoc = modelAccess.getOrCreateDocument(participantDocRef);
    return objNbs.stream().sorted()
        .flatMap(nb -> XWikiObjectFetcher.on(participantDoc)
            .filter(CourseParticipantClass.CLASS_REF).filter(nb).stream())
        .collect(toImmutableList());
  }

  private Stream<BaseObject> copyParticipantObjs(DocumentReference courseDocRef,
      List<BaseObject> participantObjsToCopy) {
    if (!participantObjsToCopy.isEmpty() && modelAccess.exists(courseDocRef)) {
      try {
        XWikiDocument regDoc = createRegistrationDoc(courseDocRef);
        List<BaseObject> copied = participantObjsToCopy.stream()
            .map(obj -> Pair.of(obj, XWikiObjectEditor.on(regDoc)
                .filter(CourseParticipantClass.CLASS_REF)
                .filter(obj.getNumber())
                .createFirstIfNotExists()))
            .filter(pair -> copyDocService.copyObject(pair.getLeft(), pair.getRight()))
            .map(Pair::getRight)
            .map(restoreParticipantObj(courseDocRef))
            .collect(toImmutableList());
        if (!copied.isEmpty()) {
          modelAccess.saveDocument(regDoc);
          LOGGER.info("copyParticipantObjs - created registration [{}] from [{}]",
              regDoc.getDocumentReference(), courseDocRef);
        }
        return copied.stream();
      } catch (DocumentAccessException dae) {
        LOGGER.error("failed to copy participants to course: " + courseDocRef, dae);
      }
    }
    return Stream.empty();
  }

  private Function<BaseObject, BaseObject> restoreParticipantObj(DocumentReference courseDocRef)
      throws DocumentNotExistsException {
    XWikiDocument courseDoc = modelAccess.getDocument(courseDocRef);
    return participantObj -> {
      xObjFieldAccessor.set(participantObj, FIELD_COURSE_ID,
          modelUtils.serializeRef(courseDocRef, COMPACT_WIKI));
      xObjFieldAccessor.set(participantObj, FIELD_TIMESTAMP, new Date());
      xObjFieldAccessor.set(participantObj, FIELD_STATUS, ParticipantStatus.confirmed);
      xObjFieldAccessor.set(participantObj, FIELD_PAYED, PaymentStatus.unpayed);
      xObjFieldAccessor.set(participantObj, FIELD_PARTIAL_PAYED_REASON, null);
      xObjFieldAccessor.set(participantObj, FIELD_PAYED_DATE, null);
      xObjFieldAccessor.set(participantObj, FIELD_PAYED_AMOUNT, XWikiObjectFetcher
          .on(courseDoc).fetchField(CourseClass.FIELD_PRICE).stream().findFirst().orElse(0));
      xObjFieldAccessor.set(participantObj, FIELD_VALIDATION_KEY,
          CourseParticipantClass.generateNewValidationKey());
      return participantObj;
    };
  }

  @Override
  public boolean setStatusConfirmedFromUnconfirmed(DocumentReference regDocRef,
      int participantObjNb) {
    try {
      XWikiDocument regDoc = modelAccess.getOrCreateDocument(regDocRef);
      if (XWikiObjectFetcher.on(regDoc).filter(participantObjNb).filter(
          CourseParticipantClass.FIELD_STATUS, ParticipantStatus.unconfirmed).exists()) {
        boolean changed = XWikiObjectEditor.on(regDoc).filter(participantObjNb).editField(
            CourseParticipantClass.FIELD_STATUS).first(ParticipantStatus.confirmed);
        if (changed) {
          modelAccess.saveDocument(regDoc, "set confirmed after confirmation mail was sent");
          return true;
        }
      }
    } catch (DocumentSaveException sde) {
      LOGGER.warn("setting status to confirmed after sending confirmation failed for doc {}",
          regDocRef);
    }
    return false;
  }

  @Override
  public boolean sendConfirmationMail(DocumentReference regDocRef, int participantObjNb) {
    return XWikiObjectFetcher.on(modelAccess.getOrCreateDocument(regDocRef))
        .filter(CourseParticipantClass.CLASS_REF)
        .filter(participantObjNb)
        .stream().findFirst()
        .map(this::sendConfirmationMail)
        .orElse(false);
  }

  private boolean sendConfirmationMail(BaseObject partiObj) {
    try {
      return sendConfirmationMail(partiObj, "");
    } catch (DocumentNotExistsException | XWikiException exp) {
      LOGGER.warn("sendConfirmationMail: failed for [{}], [{}], [{}]",
          partiObj.getDocumentReference(), partiObj.getNumber(), exp);
      return false;
    }
  }

  private boolean sendConfirmationMail(BaseObject partiObj, String fallbackEmail)
      throws DocumentNotExistsException, XWikiException {
    getVeloContext().put("courseDocRef", modelUtils.resolveRef(partiObj.getStringValue("eventid"),
        DocumentReference.class));
    Person person = createPersonFromParticipant(partiObj, fallbackEmail);
    XWikiDocument emailContentDoc = modelAccess.getDocument(getConfirmationEmailDocRef());
    return sendMail(null, person, emailContentDoc, true);
  }

  DocumentReference getConfirmationEmailDocRef() {
    return new DocumentReference(context.getWikiRef().getName(), "MailContent",
        "AnmeldungBestaetigt");
  }

  private Person createPersonFromParticipant(BaseObject bObj, String fallbackEmail) {
    Person person = new Person();
    person.setTitle(bObj.getStringValue("title"));
    person.setGivenName(bObj.getStringValue("firstname"));
    person.setSurname(bObj.getStringValue("lastname"));
    person.setAddress(bObj.getStringValue("address"));
    person.setZip(bObj.getStringValue("zip"));
    person.setCity(bObj.getStringValue("city"));
    person.setPhone(bObj.getStringValue("phone"));
    person.setEmail(xObjFieldAccessor.get(bObj, FIELD_EMAIL).orElse(fallbackEmail));
    person.setDateOfBirth(bObj.getDateValue("dob"));
    person.setStatus(bObj.getStringValue("status"));
    return person;
  }

  private boolean sendMail(String sender, Person person, XWikiDocument emailContentDoc,
      boolean sendToSender) throws XWikiException {
    boolean success = false;
    if (!person.getEmail().isEmpty()) {
      sender = MoreObjects.firstNonNull(Strings.emptyToNull(sender),
          new CelMailConfiguration().getDefaultAdminSenderAddress());
      getVeloContext().put("registrationPerson", person);
      String htmlContent = getRenderCommand().renderCelementsDocument(emailContentDoc, "view");
      String textContent = "-";
      try {
        textContent = new PlainTextCommand().convertHtmlToPlainText(htmlContent);
      } catch (ConvertToPlainTextException ctpte) {
        LOGGER.error("could not convert mail html content to plain text", ctpte);
      }
      Document emailContentDocApi = emailContentDoc.newDocument(context.getXWikiContext());
      List<Attachment> attachmentApiList = Optional.ofNullable(emailContentDoc.getAttachmentList())
          .orElseGet(ImmutableList::of).stream()
          .map(att -> new Attachment(emailContentDocApi, att, context.getXWikiContext()))
          .collect(Collectors.toList());
      LOGGER.debug("mailing with [{}] attachments", attachmentApiList.size());
      success = mailSender.sendMail(sender, null, person.getEmail(), null, null,
          emailContentDoc.getTitle(), htmlContent, textContent, attachmentApiList, null) >= 0;
      if (sendToSender) {
        success = mailSender.sendMail(sender, null, sender, null, null, emailContentDoc.getTitle(),
            htmlContent, textContent, attachmentApiList, null) >= 0;
      }
    }
    return success;
  }

  RenderCommand injected_RenderCommand;

  private RenderCommand getRenderCommand() {
    if (injected_RenderCommand != null) {
      return injected_RenderCommand;
    }
    return new RenderCommand();
  }

  private VelocityContext getVeloContext() {
    return (VelocityContext) context.getXWikiContext().get("vcontext");
  }

}
