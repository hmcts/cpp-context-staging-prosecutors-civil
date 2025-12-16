//package uk.gov.moj.cpp.cps.casemanagement.aggregate;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.junit.jupiter.MockitoExtension;
//import uk.gov.justice.cps.casemanagement.event.AccusedOrganisationAdded;
//import uk.gov.justice.cps.casemanagement.event.AccusedOrganisationRemoved;
//import uk.gov.justice.cps.casemanagement.event.AccusedOrganisationUpdated;
//import uk.gov.justice.cps.casemanagement.event.AccusedPersonAdded;
//import uk.gov.justice.cps.casemanagement.event.AccusedPersonRemoved;
//import uk.gov.justice.cps.casemanagement.event.AccusedPersonUpdated;
//import uk.gov.justice.cps.casemanagement.event.CasesLinked;
//import uk.gov.justice.cps.casemanagement.event.ChargedCaseUpdated;
//import uk.gov.justice.cps.casemanagement.event.HearingRemoved;
//import uk.gov.justice.cps.casemanagement.event.HearingUpdated;
//import uk.gov.justice.cps.casemanagement.event.LinkedCaseIds;
//import uk.gov.justice.cps.casemanagement.event.OffenceRemoved;
//import uk.gov.justice.cps.casemanagement.event.OffenceUpdateReceived;
//import uk.gov.justice.cps.casemanagement.event.OrganisationDefendantAdded;
//import uk.gov.justice.cps.casemanagement.event.OrganisationDefendantRemoved;
//import uk.gov.justice.cps.casemanagement.event.OrganisationDefendantUpdated;
//import uk.gov.justice.cps.casemanagement.event.PersonDefendantAdded;
//import uk.gov.justice.cps.casemanagement.event.PersonDefendantRemoved;
//import uk.gov.justice.cps.casemanagement.event.PersonDefendantUpdated;
//import uk.gov.justice.cps.casemanagement.event.PersonVictimAdded;
//import uk.gov.justice.cps.casemanagement.event.PersonVictimRemoved;
//import uk.gov.justice.cps.casemanagement.event.PersonVictimUpdated;
//import uk.gov.justice.cps.casemanagement.event.PersonVictimWitnessAdded;
//import uk.gov.justice.cps.casemanagement.event.PersonVictimWitnessRemoved;
//import uk.gov.justice.cps.casemanagement.event.PersonVictimWitnessUpdated;
//import uk.gov.justice.cps.casemanagement.event.PersonWitnessAdded;
//import uk.gov.justice.cps.casemanagement.event.PersonWitnessRemoved;
//import uk.gov.justice.cps.casemanagement.event.PersonWitnessUpdated;
//import uk.gov.justice.cps.casemanagement.event.PetDefendants;
//import uk.gov.justice.cps.casemanagement.event.PetDetailUpdated;
//import uk.gov.justice.cps.casemanagement.event.PetFormCreated;
//import uk.gov.justice.cps.casemanagement.event.PetFormDrafted;
//import uk.gov.justice.cps.casemanagement.event.PetFormPublished;
//import uk.gov.justice.cps.casemanagement.event.PetFormUpdated;
//import uk.gov.justice.cps.casemanagement.event.PetOperationFailed;
//import uk.gov.justice.cps.casemanagement.event.PrechargeCaseAdded;
//import uk.gov.justice.cps.casemanagement.event.PrechargeCaseUpdated;
//import uk.gov.justice.cps.casemanagement.event.ProposedChargeRemoved;
//import uk.gov.justice.cps.casemanagement.event.ProposedChargeUpdateReceived;
//import uk.gov.justice.cps.casemanagement.event.SuspectCharged;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.CaseDetails;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.Channel;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.Defendant;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.Name;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.NextHearing;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.Offence;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.Person;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.ProposedCharge;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.Suspect;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.UserDetails;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.Victim;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.VictimAndWitness;
//import uk.gov.moj.cpp.cps.core.domain.json.schemas.Witness;
//
//import java.time.LocalDate;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
//import java.util.UUID;
//import java.util.stream.Stream;
//
//import static com.google.common.collect.Lists.newArrayList;
//import static java.util.Arrays.asList;
//import static java.util.UUID.randomUUID;
//import static java.util.stream.Collectors.toList;
//import static org.hamcrest.CoreMatchers.equalTo;
//import static org.hamcrest.CoreMatchers.instanceOf;
//import static org.hamcrest.CoreMatchers.is;
//import static org.hamcrest.CoreMatchers.nullValue;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.empty;
//import static org.hamcrest.Matchers.hasSize;
//import static uk.gov.justice.cps.casemanagement.event.PrechargeCaseAdded.prechargeCaseAdded;
//import static uk.gov.justice.services.test.utils.core.random.RandomGenerator.STRING;
//import static uk.gov.justice.services.test.utils.core.reflection.ReflectionUtil.setField;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.CaseDetails.caseDetails;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.Channel.MCC;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.CpsUnit.cpsUnit;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.Defendant.defendant;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.Name.name;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.NextHearing.nextHearing;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.Offence.offence;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.Organisation.organisation;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.Person.person;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.ProposedCharge.proposedCharge;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.Suspect.suspect;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.UserDetails.userDetails;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.Victim.victim;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.VictimAndWitness.victimAndWitness;
//import static uk.gov.moj.cpp.cps.core.domain.json.schemas.Witness.witness;
//
//@ExtendWith(MockitoExtension.class)
//public class CaseAggregateTest {
//
//    private static final String CASE_URN = "35MP6785687";
//    private static final UserDetails USER_DETAILS = userDetails().build();
//    public static final String NOT_REQUIRED = "Not Required";
//
//    @InjectMocks
//    private CaseAggregate caseAggregate;
//
//    @BeforeEach
//    public void setUp() {
//        caseAggregate = new CaseAggregate();
//    }
//
//    @Test
//    public void shouldGeneratePetFormDrafted() {
//        final String caseUrn = STRING.next();
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final UUID formId = randomUUID();
//        final UUID offenceId = randomUUID();
//        final String formData = "test data";
//        final UUID petId = randomUUID();
//        final UUID userId = randomUUID();
//        Map<UUID, List<UUID>> defendantOffenceIds = new HashMap();
//        defendantOffenceIds.put(defendantId, asList(offenceId));
//        setField(caseAggregate, "caseDetails", CaseDetails.caseDetails().withUrn(caseUrn).build());
//
//        final List<Object> eventStream = caseAggregate.draftPetForm(caseId, defendantOffenceIds, formId, formData, petId, userId).collect(toList());
//        assertThat(eventStream.size(), is(1));
//        final Object object = eventStream.get(0);
//        assertThat(object.getClass(), is(equalTo(PetFormDrafted.class)));
//    }
//
//    @Test
//    public void shouldNotGeneratePetFormCreatedWhenDefendantAssociatedWithOffenceAlreadyHavePetCreated() {
//        final String caseUrn = STRING.next();
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final UUID formId = randomUUID();
//        final UUID offenceId = randomUUID();
//        final String formData = "test data";
//        final UUID petId = randomUUID();
//        final UUID userId = randomUUID();
//        Map<UUID, List<UUID>> defendantOffenceIds = new HashMap();
//        defendantOffenceIds.put(defendantId, asList(offenceId));
//
//
//        HashMap< UUID,PetFormPublished> fieldValue = new HashMap<>();
//        fieldValue.put( petId, new PetFormPublished(caseId, caseUrn, formId,Arrays.asList(new PetDefendants(defendantId,Arrays.asList(offenceId))),petId,userId ));
//        setField(caseAggregate, "petFormToBePublishedMap", fieldValue);
//
//        List<Offence> offences = new ArrayList<>();
//        offences.add(Offence.offence().withId(offenceId).build());
//        List<Defendant> defendants = new ArrayList<>();
//        defendants.add(Defendant.defendant().withId(defendantId).withOffences(offences).build());
//        CaseDetails caseDetails = CaseDetails.caseDetails().withCaseId(randomUUID()).withDefendants(defendants).build();
//        setField(caseAggregate, "caseDetails", caseDetails);
//
//
//        final List<Object> eventStream = caseAggregate.draftPetForm(caseId, defendantOffenceIds, formId, formData, petId, userId).collect(toList());
//        assertThat(eventStream.size(), is(1));
//        assertThat(eventStream.get(0).getClass(), is(equalTo(PetOperationFailed.class)));
//        final PetOperationFailed petOperationFailed = (PetOperationFailed) eventStream.get(0);
//        assertThat(petOperationFailed.getCaseId(), is(equalTo(caseId)));
//        assertThat(petOperationFailed.getPetId(), is(equalTo(petId)));
//    }
//
//    @Test
//    public void shouldGeneratePetFormUpdated() {
//        final String caseUrn = STRING.next();
//        final UUID caseId = randomUUID();
//        final String formData = "test data";
//        final UUID petId = randomUUID();
//        final UUID userId = randomUUID();
//        Map<UUID, List<UUID>> defendantOffenceIds = new HashMap();
//        defendantOffenceIds.put(randomUUID(), asList(randomUUID()));
//
//        setField(caseAggregate, "caseDetails", CaseDetails.caseDetails().withUrn(caseUrn).build());
//
//        caseAggregate.draftPetForm(caseId, defendantOffenceIds, randomUUID(), formData, petId, userId).collect(toList());
//        final List<Object> eventStream = caseAggregate.updatePetForm(caseId, formData, petId, userId).collect(toList());
//        assertThat(eventStream.size(), is(1));
//        final Object object = eventStream.get(0);
//        assertThat(object.getClass(), is(equalTo(PetFormUpdated.class)));
//    }
//
//    @Test
//    public void shouldNotGeneratePetFormUpdated() {
//        final UUID caseId = randomUUID();
//        final String formData = "test data";
//        final UUID petId = randomUUID();
//        final UUID userId = randomUUID();
//
//        final List<Object> eventStream = caseAggregate.updatePetForm(caseId, formData, petId, userId).collect(toList());
//        assertThat(eventStream.size(), is(1));
//        assertThat(eventStream.get(0).getClass(), is(equalTo(PetOperationFailed.class)));
//        final PetOperationFailed petOperationFailed = (PetOperationFailed) eventStream.get(0);
//        assertThat(petOperationFailed.getCaseId(), is(equalTo(caseId)));
//        assertThat(petOperationFailed.getPetId(), is(equalTo(petId)));
//
//    }
//
//    @Test
//    public void shouldGeneratePetFormPublished() {
//        final String caseUrn = STRING.next();
//        final UUID caseId = randomUUID();
//        final UUID defendantOrSuspectId = randomUUID();
//        final UUID formId = randomUUID();
//        final UUID offenceOrProposedChargeId = randomUUID();
//        final UUID petId = randomUUID();
//        final UUID userId = randomUUID();
//
//        HashMap< UUID,PetFormPublished> fieldValue = new HashMap<>();
//        fieldValue.put( petId, new PetFormPublished(caseId, caseUrn, formId,Arrays.asList(new PetDefendants(defendantOrSuspectId,Arrays.asList(offenceOrProposedChargeId))),petId,userId ));
//        setField(caseAggregate, "petFormToBePublishedMap", fieldValue);
//
//        final List<Object> eventStream = caseAggregate.publishPetForm(caseId,petId).collect(toList());
//
//        assertThat(eventStream.size(), is(1));
//        final Object object = eventStream.get(0);
//        final PetFormPublished petFormPublished = (PetFormPublished) eventStream.get(0);
//        assertThat(object.getClass(), is(equalTo(PetFormPublished.class)));
//        assertThat(petFormPublished.getCaseUrn(), is(equalTo(caseUrn)));
//        assertThat(petFormPublished.getFormId(), is(equalTo(formId)));
//        assertThat(petFormPublished.getPetId(), is(equalTo(petId)));
//        assertThat(petFormPublished.getUserId(), is(equalTo(userId)));
//        assertThat(petFormPublished.getPetDefendants().get(0).getDefendantOrSuspectId(), is(equalTo(defendantOrSuspectId)));
//        assertThat(petFormPublished.getPetDefendants().get(0).getOffenceOrProposedChargeIds().get(0), is(equalTo(offenceOrProposedChargeId)));
//    }
//
//    @Test
//    public void shouldGeneratePetDetailUpdated() {
//        final UUID caseId = randomUUID();
//        final UUID offenceOrProposedChargeId = randomUUID();
//        final UUID petId = randomUUID();
//        final UUID userId = randomUUID();
//
//        Map<UUID, List<UUID>> defendantOffenceIds = new HashMap();
//        final UUID newDefendantOrSuspectid = randomUUID();
//        defendantOffenceIds.put(newDefendantOrSuspectid, asList(offenceOrProposedChargeId));
//
//        final List<Object> eventStream = caseAggregate.updatePetDetail(caseId,petId,defendantOffenceIds,userId).collect(toList());
//
//        assertThat(eventStream.size(), is(1));
//        assertThat(eventStream.get(0).getClass(), is(equalTo(PetOperationFailed.class)));
//        final PetOperationFailed petOperationFailed = (PetOperationFailed) eventStream.get(0);
//        assertThat(petOperationFailed.getCaseId(), is(equalTo(caseId)));
//        assertThat(petOperationFailed.getPetId(), is(equalTo(petId)));
//    }
//
//    @Test
//    public void shouldNotGeneratePetDetailUpdated() {
//        final String caseUrn = STRING.next();
//        final UUID caseId = randomUUID();
//        final UUID defendantOrSuspectId = randomUUID();
//        final UUID formId = randomUUID();
//        final UUID offenceOrProposedChargeId = randomUUID();
//        final UUID petId = randomUUID();
//        final UUID userId = randomUUID();
//
//        HashMap< UUID,PetFormPublished> fieldValue = new HashMap<>();
//        fieldValue.put( petId, new PetFormPublished(caseId, caseUrn, formId,Arrays.asList(new PetDefendants(defendantOrSuspectId,Arrays.asList(offenceOrProposedChargeId))),petId,userId ));
//        setField(caseAggregate, "caseDetails", CaseDetails.caseDetails().withUrn(caseUrn).build());
//        setField(caseAggregate, "petFormToBePublishedMap", fieldValue);
//
//        Map<UUID, List<UUID>> defendantOffenceIds = new HashMap();
//        final UUID newDefendantOrSuspectid = randomUUID();
//        defendantOffenceIds.put(newDefendantOrSuspectid, asList(offenceOrProposedChargeId));
//
//        final List<Object> eventStream = caseAggregate.updatePetDetail(caseId,petId,defendantOffenceIds,userId).collect(toList());
//
//        assertThat(eventStream.size(), is(1));
//        final Object object = eventStream.get(0);
//        final PetDetailUpdated petDetailUpdated = (PetDetailUpdated) eventStream.get(0);
//        assertThat(object.getClass(), is(equalTo(PetDetailUpdated.class)));
//        assertThat(petDetailUpdated.getCaseUrn(), is(equalTo(caseUrn)));
//        assertThat(petDetailUpdated.getPetId(), is(equalTo(petId)));
//        assertThat(petDetailUpdated.getUserId(), is(equalTo(userId)));
//        assertThat(petDetailUpdated.getPetDefendants().get(0).getDefendantOrSuspectId(), is(equalTo(newDefendantOrSuspectid)));
//        assertThat(petDetailUpdated.getPetDefendants().get(0).getOffenceOrProposedChargeIds().get(0), is(equalTo(offenceOrProposedChargeId)));
//    }
//
//    @Test
//    public void shouldGeneratePetFormCreated() {
//        final String caseUrn = STRING.next();
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final UUID formId = randomUUID();
//        final UUID offenceId = randomUUID();
//        final String formData = "test data";
//        final UUID petId = randomUUID();
//        final UUID userId = randomUUID();
//        Map<UUID, List<UUID>> defendantOffenceIds = new HashMap();
//        defendantOffenceIds.put(defendantId, asList(offenceId));
//        setField(caseAggregate, "caseDetails", CaseDetails.caseDetails().withUrn(caseUrn).build());
//
//        final List<Object> eventStream = caseAggregate.createPetForm(caseId, defendantOffenceIds, formId, formData, petId, userId).collect(toList());
//        assertThat(eventStream.size(), is(1));
//        final Object object = eventStream.get(0);
//        assertThat(object.getClass(), is(equalTo(PetFormCreated.class)));
//    }
//
//    @Test
//    public void shouldAddVictimReceivedWhenNewVictimAdded() {
//        List<Victim> existingWitnesses = newArrayList();
//        existingWitnesses.add(victim().withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("first")
//                                .withLastName("last name")
//                                .build())
//                        .build())
//                .build());
//
//
//        final PrechargeCaseAdded prechargeCaseReceived = prechargeCaseAdded()
//                .withCaseDetails(caseDetails()
//                        .withCaseId(randomUUID())
//                        .withUrn(CASE_URN)
//                        .withVictims(existingWitnesses)
//                        .build())
//                .build();
//        caseAggregate.apply(prechargeCaseReceived);
//        assertThat(caseAggregate.getCaseDetails().getVictims(), hasSize(1));
//        final Victim addVictim = victim().withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("Second")
//                                .withLastName("another name")
//                                .build())
//                        .build())
//                .build();
//
//        final Stream<Object> eventStream = caseAggregate.addPersonVictim(prechargeCaseReceived.getCaseDetails().getCaseId(), CASE_URN, addVictim, MCC, USER_DETAILS);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimAdded.class));
//        final PersonVictimAdded personVictimReceived = (PersonVictimAdded) eventList.get(0);
//        assertThat(personVictimReceived.getVictim().getId(), is(addVictim.getId()));
//        assertThat(caseAggregate.getCaseDetails().getVictims(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getVictims().stream().anyMatch(v -> v.getId().equals(addVictim.getId())), is(true));
//    }
//
//
//    @Test
//    public void shouldAddVictimReceivedWhenNewVictimAddedBeforeAddingPrechargeCase() {
//        final UUID caseId = randomUUID();
//
//        final Victim addVictim = victim().withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("Second")
//                                .withLastName("another name")
//                                .build())
//                        .build())
//                .build();
//
//        final Stream<Object> eventStream = caseAggregate.addPersonVictim(caseId, CASE_URN, addVictim, MCC, USER_DETAILS);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimAdded.class));
//        final PersonVictimAdded personVictimReceived = (PersonVictimAdded) eventList.get(0);
//        assertThat(personVictimReceived.getVictim().getId(), is(addVictim.getId()));
//        assertThat(caseAggregate.getCaseDetails().getVictims(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getVictims().stream().anyMatch(v -> v.getId().equals(addVictim.getId())), is(true));
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .build();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addPrechargeCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getVictims(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getUrn(), is(CASE_URN));
//
//    }
//
//
//    @Test
//    public void shouldAddWitnessReceivedWhenNewWitnessAdded() {
//        List<Witness> existingWitnesses = newArrayList();
//        existingWitnesses.add(witness()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("first")
//                                .withLastName("last name")
//                                .build())
//                        .build())
//                .build());
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(randomUUID())
//                .withUrn(CASE_URN)
//                .withWitnesses(existingWitnesses)
//                .build();
//        caseAggregate.addPrechargeCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//        assertThat(caseAggregate.getCaseDetails().getWitnesses(), hasSize(1));
//
//
//        final Witness addWitness = witness()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("Second")
//                                .withLastName("another name")
//                                .build())
//                        .build())
//                .build();
//        final Stream<Object> eventStream = caseAggregate.addPersonWitness(caseDetails.getCaseId(), CASE_URN, addWitness, MCC, USER_DETAILS);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonWitnessAdded.class));
//        final PersonWitnessAdded personWitnessAdded = (PersonWitnessAdded) eventList.get(0);
//        assertThat(personWitnessAdded.getWitness().getId(), is(addWitness.getId()));
//        assertThat(caseAggregate.getCaseDetails().getWitnesses(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getWitnesses().stream().anyMatch(v -> v.getId().equals(addWitness.getId())), is(true));
//    }
//
//    @Test
//    public void shouldAddWitnessReceivedWhenNewWitnessAddedBeforeAddingPreChargeCase() {
//        final UUID caseId = randomUUID();
//
//        final Witness addWitness = witness()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("Second")
//                                .withLastName("another name")
//                                .build())
//                        .build())
//                .build();
//        final Stream<Object> eventStream = caseAggregate.addPersonWitness(caseId, CASE_URN, addWitness, MCC, USER_DETAILS);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonWitnessAdded.class));
//        final PersonWitnessAdded personWitnessAdded = (PersonWitnessAdded) eventList.get(0);
//        assertThat(personWitnessAdded.getWitness().getId(), is(addWitness.getId()));
//        assertThat(caseAggregate.getCaseDetails().getWitnesses(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getWitnesses().stream().anyMatch(v -> v.getId().equals(addWitness.getId())), is(true));
//
//
//        List<Witness> existingWitnesses = newArrayList();
//        existingWitnesses.add(witness()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("first")
//                                .withLastName("last name")
//                                .build())
//                        .build())
//                .build());
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .build();
//        caseAggregate.addPrechargeCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//        assertThat(caseAggregate.getCaseDetails().getWitnesses(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getUrn(), is(CASE_URN));
//    }
//
//    @Test
//    public void shouldAddVictimWitnessReceivedWhenNewVictimAWitnessAdded() {
//        List<VictimAndWitness> existingWitnesses = newArrayList();
//        existingWitnesses.add(victimAndWitness().withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("first")
//                                .withLastName("last name")
//                                .build())
//                        .build())
//                .build());
//        final PrechargeCaseAdded prechargeCaseReceived = prechargeCaseAdded()
//                .withCaseDetails(caseDetails()
//                        .withCaseId(randomUUID())
//                        .withUrn(CASE_URN)
//                        .withVictimsAndWitnesses(existingWitnesses)
//                        .build())
//                .build();
//        caseAggregate.apply(prechargeCaseReceived);
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses(), hasSize(1));
//        final VictimAndWitness addVictimWitness = victimAndWitness().withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("Second")
//                                .withLastName("another name")
//                                .build())
//                        .build())
//                .build();
//
//        final Stream<Object> eventStream = caseAggregate.addPersonVictimAndWitness(prechargeCaseReceived.getCaseDetails().getCaseId(), CASE_URN, addVictimWitness, MCC, USER_DETAILS);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimWitnessAdded.class));
//        final PersonVictimWitnessAdded personVictimWitnessReceived = (PersonVictimWitnessAdded) eventList.get(0);
//        assertThat(personVictimWitnessReceived.getVictimAndWitness().getId(), is(addVictimWitness.getId()));
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses().stream().anyMatch(v -> v.getId().equals(addVictimWitness.getId())), is(true));
//    }
//
//    @Test
//    public void shouldAddVictimWitnessReceivedWhenNewVictimAWitnessAddedBeforeAddingPrechargeCase() {
//        final UUID caseId = randomUUID();
//        final VictimAndWitness addVictimWitness = victimAndWitness().withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("Second")
//                                .withLastName("another name")
//                                .build())
//                        .build())
//                .build();
//
//        final Stream<Object> eventStream = caseAggregate.addPersonVictimAndWitness(caseId, CASE_URN, addVictimWitness, MCC, USER_DETAILS);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimWitnessAdded.class));
//        final PersonVictimWitnessAdded personVictimWitnessReceived = (PersonVictimWitnessAdded) eventList.get(0);
//        assertThat(personVictimWitnessReceived.getVictimAndWitness().getId(), is(addVictimWitness.getId()));
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses().stream().anyMatch(v -> v.getId().equals(addVictimWitness.getId())), is(true));
//
//        final PrechargeCaseAdded prechargeCaseReceived = prechargeCaseAdded()
//                .withCaseDetails(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(CASE_URN)
//                        .build())
//                .build();
//        caseAggregate.apply(prechargeCaseReceived);
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses(), hasSize(1));
//    }
//
//
//    @Test
//    public void shouldUpdateChargeCaseWhenUpdateForChargeCaseCommandReceived() {
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(randomUUID())
//                .withUrn(CASE_URN)
//                .withCaseUnit(cpsUnit()
//                        .withId(randomUUID())
//                        .withCode("Unit 1")
//                        .build())
//                .withDefendants(asList(defendant()
//                        .withId(randomUUID())
//                        .withPerson(person()
//                                .withName(name()
//                                        .withForenames("First|Second|Third|Fourth")
//                                        .build())
//                                .withAliases(asList(name().withForenames("A|B|C|D").build()))
//                                .build())
//                        .build()))
//                .build();
//
//        caseAggregate.addChargedCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//        assertThat(caseAggregate.getCaseDetails().getCaseUnit().getId(), is(caseDetails.getCaseUnit().getId()));
//
//        final CaseDetails updatedCaseDetails = caseDetails()
//                .withCaseId(caseDetails.getCaseId())
//                .withUrn(CASE_URN)
//                .withCaseUnit(cpsUnit()
//                        .withId(randomUUID())
//                        .withCode("Unit 2")
//                        .build())
//                .withOperationName("Operation Name")
//                .build();
//
//        final Stream<Object> eventStream = caseAggregate.updateChargedCase(updatedCaseDetails, MCC, USER_DETAILS);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(ChargedCaseUpdated.class));
//        final ChargedCaseUpdated chargedCaseUpdated = (ChargedCaseUpdated) eventList.get(0);
//        assertThat(chargedCaseUpdated.getCaseId(), is(caseDetails.getCaseId()));
//        assertThat(caseAggregate.getCaseDetails().getCaseUnit().getCode(), is(updatedCaseDetails.getCaseUnit().getCode()));
//        assertThat(caseAggregate.getCaseDetails().getOperationName(), is(updatedCaseDetails.getOperationName()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//    }
//
//
//    @Test
//    public void shouldUpdatePreChargeCaseReceivedWhenGivenUpdateCaseReceived() {
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(randomUUID())
//                .withUrn(CASE_URN)
//                .withCaseUnit(cpsUnit()
//                        .withId(randomUUID())
//                        .withCode("Unit 1")
//                        .build())
//                .build();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addPrechargeCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//        assertThat(caseAggregate.getCaseDetails().getCaseUnit().getId(), is(caseDetails.getCaseUnit().getId()));
//
//        final CaseDetails updatedCaseDetails = caseDetails()
//                .withCaseId(caseDetails.getCaseId())
//                .withUrn(CASE_URN)
//                .withCaseUnit(cpsUnit()
//                        .withId(randomUUID())
//                        .withCode("Unit 2")
//                        .build())
//                .withOperationName("Operation Name")
//                .build();
//
//        final Stream<Object> eventStream = caseAggregate.updatePrechargeCase(updatedCaseDetails, MCC, USER_DETAILS);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PrechargeCaseUpdated.class));
//        final PrechargeCaseUpdated prechargeCaseUpdateReceived = (PrechargeCaseUpdated) eventList.get(0);
//        assertThat(prechargeCaseUpdateReceived.getCaseId(), is(caseDetails.getCaseId()));
//        assertThat(caseAggregate.getCaseDetails().getCaseUnit().getCode(), is(updatedCaseDetails.getCaseUnit().getCode()));
//        assertThat(caseAggregate.getCaseDetails().getOperationName(), is(updatedCaseDetails.getOperationName()));
//    }
//
//    @Test
//    public void shouldReturnAccusedOrganisationAdded() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        caseAggregate.addChargedCase(
//                caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Suspect suspectOrganisation = suspect()
//                .withId(suspectId)
//                .withOrganisation(organisation()
//                        .withName("XYZ Test LTD")
//                        .build())
//                .build();
//
//        final AccusedOrganisationAdded result = (AccusedOrganisationAdded) caseAggregate.addAccusedOrganisation(caseId, caseUrn, suspectOrganisation, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//
//        final int suspectCount = caseAggregate.getCaseDetails().getSuspects().size();
//        final CaseDetails updatedCaseDetails = caseAggregate.getCaseDetails();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getSuspect().getOrganisation().getName(), is("XYZ Test LTD"));
//        assertThat(suspectCount, is(1));
//        assertThat(updatedCaseDetails.getSuspects().get(0).getOrganisation().getName(), is("XYZ Test LTD"));
//    }
//
//
//    @Test
//    public void shouldReturnAccusedOrganisationAddedBeforeAddingChargedCase() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final Suspect suspectOrganisation = suspect()
//                .withId(suspectId)
//                .withOrganisation(organisation()
//                        .withName("XYZ Test LTD")
//                        .build())
//                .build();
//
//        final AccusedOrganisationAdded result = (AccusedOrganisationAdded) caseAggregate.addAccusedOrganisation(caseId, caseUrn, suspectOrganisation, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//
//        final int suspectCount = caseAggregate.getCaseDetails().getSuspects().size();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getSuspect().getOrganisation().getName(), is("XYZ Test LTD"));
//        assertThat(suspectCount, is(1));
//
//        caseAggregate.addChargedCase(
//                caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//        final CaseDetails finalCaseDetails = caseAggregate.getCaseDetails();
//        assertThat(finalCaseDetails.getSuspects().get(0).getOrganisation().getName(), is("XYZ Test LTD"));
//    }
//
//    @Test
//    public void shouldReturnAccusedPersonAdded() {
//
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(
//                caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final LocalDate dateOfBirth = LocalDate.of(2000, 1, 1);
//
//        final Suspect cpsSuspectPerson = suspect()
//                .withId(suspectId)
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(dateOfBirth)
//                        .build())
//                .build();
//
//        final AccusedPersonAdded result = (AccusedPersonAdded) caseAggregate.addAccusedPerson(caseId, caseUrn, cpsSuspectPerson, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//        final int finalSize = caseAggregate.getCaseDetails().getSuspects().size();
//        final CaseDetails finalCaseDetails = caseAggregate.getCaseDetails();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getSuspect().getPerson().getName().getForenames(), is("John"));
//        assertThat(result.getSuspect().getPerson().getName().getLastName(), is("Brown"));
//        assertThat(result.getSuspect().getPerson().getDateOfBirth(), is(dateOfBirth));
//        assertThat(finalSize, is(1));
//        assertThat(finalCaseDetails.getSuspects().get(0).getPerson().getName().getForenames(), is("John"));
//        assertThat(finalCaseDetails.getSuspects().get(0).getPerson().getName().getLastName(), is("Brown"));
//        assertThat(finalCaseDetails.getSuspects().get(0).getPerson().getDateOfBirth(), is(dateOfBirth));
//    }
//
//    @Test
//    public void shouldReturnAccusedPersonAddedBeforeAddingChargedCase() {
//
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final LocalDate dateOfBirth = LocalDate.of(2000, 1, 1);
//
//        final Suspect cpsSuspectPerson = suspect()
//                .withId(suspectId)
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(dateOfBirth)
//                        .build())
//                .build();
//
//        final AccusedPersonAdded result = (AccusedPersonAdded) caseAggregate.addAccusedPerson(caseId, caseUrn, cpsSuspectPerson, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//        final int finalSize = caseAggregate.getCaseDetails().getSuspects().size();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getSuspect().getPerson().getName().getForenames(), is("John"));
//        assertThat(result.getSuspect().getPerson().getName().getLastName(), is("Brown"));
//        assertThat(result.getSuspect().getPerson().getDateOfBirth(), is(dateOfBirth));
//        assertThat(finalSize, is(1));
//
//        caseAggregate.addChargedCase(
//                caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final CaseDetails finalCaseDetails = caseAggregate.getCaseDetails();
//
//        assertThat(finalCaseDetails.getSuspects().get(0).getPerson().getName().getForenames(), is("John"));
//        assertThat(finalCaseDetails.getSuspects().get(0).getPerson().getName().getLastName(), is("Brown"));
//        assertThat(finalCaseDetails.getSuspects().get(0).getPerson().getDateOfBirth(), is(dateOfBirth));
//    }
//
//
//    @Test
//    public void shouldReturnAccusedDefendantAdded() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        caseAggregate.addChargedCase(
//                caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Defendant organisationDefendant = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation()
//                        .withName("XYZ Test LTD")
//                        .build())
//                .build();
//
//        final OrganisationDefendantAdded result = (OrganisationDefendantAdded) caseAggregate.addOrganisationDefendant(caseId, caseUrn, organisationDefendant, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//
//        final int defedantCount = caseAggregate.getCaseDetails().getDefendants().size();
//        final CaseDetails updatedCaseDetails = caseAggregate.getCaseDetails();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getDefendant().getOrganisation().getName(), is("XYZ Test LTD"));
//        assertThat(defedantCount, is(1));
//        assertThat(updatedCaseDetails.getDefendants().get(0).getOrganisation().getName(), is("XYZ Test LTD"));
//    }
//
//    @Test
//    public void shouldReturnAccusedDefendantAddedBeforeAddingChargedCase() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String caseUrn = STRING.next();
//
//        final Defendant organisationDefendant = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation()
//                        .withName("XYZ Test LTD")
//                        .build())
//                .build();
//
//        final OrganisationDefendantAdded result = (OrganisationDefendantAdded) caseAggregate.addOrganisationDefendant(caseId, caseUrn, organisationDefendant, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//
//        final int defedantCount = caseAggregate.getCaseDetails().getDefendants().size();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getDefendant().getOrganisation().getName(), is("XYZ Test LTD"));
//        assertThat(defedantCount, is(1));
//
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        caseAggregate.addChargedCase(
//                caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final CaseDetails finalCaseDetail = caseAggregate.getCaseDetails();
//        assertThat(finalCaseDetail.getDefendants().get(0).getOrganisation().getName(), is("XYZ Test LTD"));
//    }
//
//    @Test
//    public void shouldReturnPersonDefendantAdded() {
//
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(
//                caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final LocalDate dateOfBirth = LocalDate.of(2000, 1, 1);
//
//        final Defendant cpsSuspectPerson = defendant()
//                .withId(defendantId)
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(dateOfBirth)
//                        .build())
//                .build();
//
//        final PersonDefendantAdded result = (PersonDefendantAdded) caseAggregate.addPersonDefendant(caseId, caseUrn, cpsSuspectPerson, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//        final int finalSize = caseAggregate.getCaseDetails().getDefendants().size();
//        final CaseDetails finalCaseDetails = caseAggregate.getCaseDetails();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getDefendant().getPerson().getName().getForenames(), is("John"));
//        assertThat(result.getDefendant().getPerson().getName().getLastName(), is("Brown"));
//        assertThat(result.getDefendant().getPerson().getDateOfBirth(), is(dateOfBirth));
//        assertThat(finalSize, is(1));
//        assertThat(finalCaseDetails.getDefendants().get(0).getPerson().getName().getForenames(), is("John"));
//        assertThat(finalCaseDetails.getDefendants().get(0).getPerson().getName().getLastName(), is("Brown"));
//        assertThat(finalCaseDetails.getDefendants().get(0).getPerson().getDateOfBirth(), is(dateOfBirth));
//    }
//
//
//    @Test
//    public void shouldReturnPersonDefendantAddedBeforeChargedCase() {
//
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String caseUrn = STRING.next();
//
//        final LocalDate dateOfBirth = LocalDate.of(2000, 1, 1);
//
//        final Defendant cpsSuspectPerson = defendant()
//                .withId(defendantId)
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(dateOfBirth)
//                        .build())
//                .build();
//
//        final PersonDefendantAdded result = (PersonDefendantAdded) caseAggregate.addPersonDefendant(caseId, caseUrn, cpsSuspectPerson, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//        final int finalSize = caseAggregate.getCaseDetails().getDefendants().size();
//        final CaseDetails afterDefendentAdded = caseAggregate.getCaseDetails();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getDefendant().getPerson().getName().getForenames(), is("John"));
//        assertThat(result.getDefendant().getPerson().getName().getLastName(), is("Brown"));
//        assertThat(result.getDefendant().getPerson().getDateOfBirth(), is(dateOfBirth));
//        assertThat(finalSize, is(1));
//        assertThat(afterDefendentAdded.getDefendants().get(0).getPerson().getName().getForenames(), is("John"));
//        assertThat(afterDefendentAdded.getDefendants().get(0).getPerson().getName().getLastName(), is("Brown"));
//        assertThat(afterDefendentAdded.getDefendants().get(0).getPerson().getDateOfBirth(), is(dateOfBirth));
//
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(
//                caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final CaseDetails finalCaseDetails = caseAggregate.getCaseDetails();
//        assertThat(finalCaseDetails.getDefendants().get(0).getPerson().getName().getForenames(), is("John"));
//        assertThat(finalCaseDetails.getDefendants().get(0).getPerson().getName().getLastName(), is("Brown"));
//        assertThat(finalCaseDetails.getDefendants().get(0).getPerson().getDateOfBirth(), is(dateOfBirth));
//
//
//    }
//
//    @Test
//    public void shouldReturnAccusedOrganisationUpdated() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String caseUrn = STRING.next();
//
//
//        registerCaseForSuspect(caseId, suspectId, caseUrn, MCC);
//
//        final int initialSize = caseAggregate.getCaseDetails().getSuspects().size();
//        final CaseDetails initialCaseDetails = caseAggregate.getCaseDetails();
//
//        final Suspect suspectOrganisation = suspect()
//                .withId(suspectId)
//                .withOrganisation(organisation()
//                        .withName("XYZ Test Updated LTD")
//                        .build())
//                .build();
//
//        final AccusedOrganisationUpdated result = (AccusedOrganisationUpdated) caseAggregate.updateAccusedOrganisation(caseId, caseUrn, suspectOrganisation, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//
//        final int finalSize = caseAggregate.getCaseDetails().getSuspects().size();
//        final CaseDetails updatedCaseDetails = caseAggregate.getCaseDetails();
//
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getSuspect().getOrganisation().getName(), is("XYZ Test Updated LTD"));
//        assertThat(initialSize, is(finalSize));
//        assertThat(initialCaseDetails.getSuspects().get(0).getOrganisation().getName(), is("XYZ Test LTD"));
//        assertThat(updatedCaseDetails.getSuspects().get(0).getOrganisation().getName(), is("XYZ Test Updated LTD"));
//    }
//
//    @Test
//    public void shouldReturnAccusedPersonUpdated() {
//
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String caseUrn = STRING.next();
//
//
//        registerCaseForSuspect(caseId, suspectId, caseUrn, MCC);
//        final int initialSize = caseAggregate.getCaseDetails().getSuspects().size();
//        final CaseDetails initialCaseDetails = caseAggregate.getCaseDetails();
//
//        final LocalDate dateOfBirth = LocalDate.of(2000, 1, 1);
//
//        final Suspect cpsSuspectPerson = suspect()
//                .withId(suspectId)
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("Updated John").withLastName("Updated Brown").build())
//                        .withDateOfBirth(dateOfBirth)
//                        .build())
//                .build();
//
//        final AccusedPersonUpdated result = (AccusedPersonUpdated) caseAggregate.updateAccusedPerson(caseId, caseUrn, cpsSuspectPerson, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//        final int finalSize = caseAggregate.getCaseDetails().getSuspects().size();
//        final CaseDetails finalCaseDetails = caseAggregate.getCaseDetails();
//
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getSuspect().getPerson().getName().getForenames(), is("Updated John"));
//        assertThat(result.getSuspect().getPerson().getName().getLastName(), is("Updated Brown"));
//        assertThat(result.getSuspect().getPerson().getDateOfBirth(), is(dateOfBirth));
//        assertThat(initialSize, is(finalSize));
//        assertThat(initialCaseDetails.getSuspects().get(0).getPerson().getName().getForenames(), is("John"));
//        assertThat(initialCaseDetails.getSuspects().get(0).getPerson().getName().getLastName(), is("Brown"));
//        assertThat(initialCaseDetails.getSuspects().get(0).getPerson().getDateOfBirth(), is(dateOfBirth));
//        assertThat(finalCaseDetails.getSuspects().get(0).getPerson().getName().getForenames(), is("Updated John"));
//        assertThat(finalCaseDetails.getSuspects().get(0).getPerson().getName().getLastName(), is("Updated Brown"));
//        assertThat(finalCaseDetails.getSuspects().get(0).getPerson().getDateOfBirth(), is(dateOfBirth));
//
//    }
//
//    @Test
//    public void shouldReturnOrganisationDefendantUpdated() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String caseUrn = STRING.next();
//
//
//        registerCaseForDefendant(caseId, defendantId, caseUrn, MCC);
//
//        final int initialSize = caseAggregate.getCaseDetails().getDefendants().size();
//        final CaseDetails initialCaseDetails = caseAggregate.getCaseDetails();
//
//        final Defendant suspectOrganisation = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation()
//                        .withName("XYZ Test Updated LTD")
//                        .build())
//                .build();
//
//        final OrganisationDefendantUpdated result = (OrganisationDefendantUpdated) caseAggregate.updateOrganisationDefendant(caseId, caseUrn, suspectOrganisation, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//
//        final int finalSize = caseAggregate.getCaseDetails().getDefendants().size();
//        final CaseDetails updatedCaseDetails = caseAggregate.getCaseDetails();
//
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getDefendant().getOrganisation().getName(), is("XYZ Test Updated LTD"));
//        assertThat(initialSize, is(finalSize));
//        assertThat(initialCaseDetails.getDefendants().get(0).getOrganisation().getName(), is("XYZ Test LTD"));
//        assertThat(updatedCaseDetails.getDefendants().get(0).getOrganisation().getName(), is("XYZ Test Updated LTD"));
//    }
//
//    @Test
//    public void shouldReturnPersonDefendantUpdated() {
//
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String caseUrn = STRING.next();
//
//
//        registerCaseForDefendant(caseId, defendantId, caseUrn, MCC);
//        final int initialSize = caseAggregate.getCaseDetails().getDefendants().size();
//        final CaseDetails initialCaseDetails = caseAggregate.getCaseDetails();
//
//        final LocalDate dateOfBirth = LocalDate.of(2000, 1, 1);
//
//        final Defendant cpsSuspectPerson = defendant()
//                .withId(defendantId)
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("Updated John").withLastName("Updated Brown").build())
//                        .withDateOfBirth(dateOfBirth)
//                        .build())
//                .build();
//
//        final PersonDefendantUpdated result = (PersonDefendantUpdated) caseAggregate.updatePersonDefendant(caseId, caseUrn, cpsSuspectPerson, MCC, USER_DETAILS)
//                .collect(toList()).get(0);
//        final int finalSize = caseAggregate.getCaseDetails().getDefendants().size();
//        final CaseDetails finalCaseDetails = caseAggregate.getCaseDetails();
//
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getDefendant().getPerson().getName().getForenames(), is("Updated John"));
//        assertThat(result.getDefendant().getPerson().getName().getLastName(), is("Updated Brown"));
//        assertThat(result.getDefendant().getPerson().getDateOfBirth(), is(dateOfBirth));
//        assertThat(initialSize, is(finalSize));
//        assertThat(initialCaseDetails.getDefendants().get(0).getPerson().getName().getForenames(), is("John"));
//        assertThat(initialCaseDetails.getDefendants().get(0).getPerson().getName().getLastName(), is("Brown"));
//        assertThat(initialCaseDetails.getDefendants().get(0).getPerson().getDateOfBirth(), is(dateOfBirth));
//        assertThat(finalCaseDetails.getDefendants().get(0).getPerson().getName().getForenames(), is("Updated John"));
//        assertThat(finalCaseDetails.getDefendants().get(0).getPerson().getName().getLastName(), is("Updated Brown"));
//        assertThat(finalCaseDetails.getDefendants().get(0).getPerson().getDateOfBirth(), is(dateOfBirth));
//
//    }
//
//    private void registerCaseForSuspect(final UUID caseId, final UUID suspectId, final String caseUrn, final Channel channel) {
//        final List<Suspect> suspects = new ArrayList<>();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .build();
//        suspects.add(suspect);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withSuspects(suspects)
//                        .build(),
//                channel, USER_DETAILS, linkedCaseIds, linkedReasonId);
//    }
//
//    private void registerCaseForDefendant(final UUID caseId, final UUID defendantId, final String caseUrn, final Channel channel) {
//        final List<Defendant> defendants = new ArrayList<>();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .build();
//        defendants.add(defendant);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withDefendants(defendants)
//                        .build(),
//                channel, USER_DETAILS, linkedCaseIds, linkedReasonId);
//    }
//
//    @Test
//    public void shouldRemoveHearing() {
//        final UUID defendantId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withDefendants(asList(defendant()
//                        .withId(defendantId)
//                        .withNextHearing(nextHearing().build())
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Stream<Object> eventStream = caseAggregate.removeHearing("12345", caseId,
//                randomUUID(), defendantId, userDetails().withName("John Wick").build());
//        List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(HearingRemoved.class));
//        final HearingRemoved hearingRemoved = (HearingRemoved) eventList.get(0);
//        assertThat(hearingRemoved.getUrn(), is("12345"));
//        assertThat(hearingRemoved.getCaseId(), is(caseId));
//        assertThat(hearingRemoved.getUserDetails().getName(), is("John Wick"));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getNextHearing(), is(nullValue()));
//    }
//
//    @Test
//    public void shouldRemoveVictim() {
//        final UUID victimId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withVictims(asList(victim()
//                        .withId(victimId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Stream<Object> eventStream = caseAggregate.removeVictim(caseId,
//                "12345", victimId, "a reason", MCC, null);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimRemoved.class));
//        PersonVictimRemoved personVictimRemoved = (PersonVictimRemoved) eventList.get(0);
//        assertThat(personVictimRemoved.getUrn(), is("12345"));
//        assertThat(personVictimRemoved.getCaseId(), is(caseId));
//        assertThat(personVictimRemoved.getReason(), is("a reason"));
//        assertThat(caseAggregate.getCaseDetails().getVictims(), empty());
//    }
//
//    @Test
//    public void shouldRemoveWitness() {
//        final UUID victimId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withWitnesses(asList(witness()
//                        .withId(victimId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Stream<Object> eventStream = caseAggregate.removeWitness(caseId,
//                "12345", victimId, "a reason", MCC, null);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonWitnessRemoved.class));
//        PersonWitnessRemoved personWitnessRemoved = (PersonWitnessRemoved) eventList.get(0);
//        assertThat(personWitnessRemoved.getUrn(), is("12345"));
//        assertThat(personWitnessRemoved.getCaseId(), is(caseId));
//        assertThat(personWitnessRemoved.getReason(), is("a reason"));
//        assertThat(caseAggregate.getCaseDetails().getWitnesses(), empty());
//    }
//
//    @Test
//    public void shouldRemoveVictimWitness() {
//        final UUID victimId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withVictimsAndWitnesses(asList(victimAndWitness()
//                        .withId(victimId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Stream<Object> eventStream = caseAggregate.removeVictimWitness(caseId,
//                "12345", victimId, "a reason", MCC, null);
//        final List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimWitnessRemoved.class));
//        PersonVictimWitnessRemoved personVictimWitnessRemoved = (PersonVictimWitnessRemoved) eventList.get(0);
//        assertThat(personVictimWitnessRemoved.getUrn(), is("12345"));
//        assertThat(personVictimWitnessRemoved.getCaseId(), is(caseId));
//        assertThat(personVictimWitnessRemoved.getReason(), is("a reason"));
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses(), empty());
//    }
//
//    @Test
//    public void shouldReturnAccusedOrganisationRemoved() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String caseUrn = STRING.next();
//
//        registerCaseForSuspect(caseId, suspectId, caseUrn, MCC);
//
//        final int initialSize = caseAggregate.getCaseDetails().getSuspects().size();
//
//        final UserDetails userDetails = buldUserDetail();
//
//        final AccusedOrganisationRemoved result = (AccusedOrganisationRemoved) caseAggregate
//                .removeAccusedOrganisation(caseId, caseUrn, suspectId, MCC, userDetails)
//                .collect(toList()).get(0);
//
//        final int finalSize = caseAggregate.getCaseDetails().getSuspects().size();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getSuspectId(), is(suspectId));
//        assertThat(initialSize, is(1));
//        assertThat(finalSize, is(0));
//    }
//
//    @Test
//    public void shouldReturnAccusedPersonRemoved() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String caseUrn = STRING.next();
//
//        registerCaseForSuspect(caseId, suspectId, caseUrn, MCC);
//
//        final int initialSize = caseAggregate.getCaseDetails().getSuspects().size();
//
//        final UserDetails userDetails = buldUserDetail();
//
//        final AccusedPersonRemoved result = (AccusedPersonRemoved) caseAggregate
//                .removeAccusedPerson(caseId, caseUrn, suspectId, MCC, userDetails)
//                .collect(toList()).get(0);
//
//        final int finalSize = caseAggregate.getCaseDetails().getSuspects().size();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getSuspectId(), is(suspectId));
//        assertThat(initialSize, is(1));
//        assertThat(finalSize, is(0));
//    }
//
//    @Test
//    public void shouldReturnOrganisationDefendantRemoved() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String caseUrn = STRING.next();
//        final String reason = "No longer defendant";
//
//        registerCaseForDefendant(caseId, defendantId, caseUrn, MCC);
//
//        final int initialSize = caseAggregate.getCaseDetails().getDefendants().size();
//
//        final UserDetails userDetails = buldUserDetail();
//
//        final OrganisationDefendantRemoved result = (OrganisationDefendantRemoved) caseAggregate
//                .removeOrganisationDefendant(caseId, caseUrn, defendantId, reason, MCC, userDetails)
//                .collect(toList()).get(0);
//
//        final int finalSize = caseAggregate.getCaseDetails().getDefendants().size();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getDefendantId(), is(defendantId));
//        assertThat(initialSize, is(1));
//        assertThat(finalSize, is(0));
//    }
//
//    @Test
//    public void shouldReturnPersonDefendantRemoved() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String caseUrn = STRING.next();
//        final String reason = "No longer defendant";
//
//        registerCaseForDefendant(caseId, defendantId, caseUrn, MCC);
//
//        final int initialSize = caseAggregate.getCaseDetails().getDefendants().size();
//
//        final UserDetails userDetails = buldUserDetail();
//
//        final PersonDefendantRemoved result = (PersonDefendantRemoved) caseAggregate
//                .removePersonDefendant(caseId, caseUrn, defendantId, reason, MCC, userDetails)
//                .collect(toList()).get(0);
//
//        final int finalSize = caseAggregate.getCaseDetails().getDefendants().size();
//
//        assertThat(result.getCaseId(), is(caseId));
//        assertThat(result.getDefendantId(), is(defendantId));
//        assertThat(initialSize, is(1));
//        assertThat(finalSize, is(0));
//    }
//
//    @Test
//    public void shouldAddOffenceForDefendantWithNoOffence() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String defendantExternalId = STRING.next();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final List<Defendant> defendants = new ArrayList<>();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .build();
//        defendants.add(defendant);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withDefendants(defendants)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), nullValue());
//
//        final Offence offence = offence().withId(randomUUID()).build();
//
//        caseAggregate.addOffences(caseId, caseUrn, defendantId, defendantExternalId, asList(offence), MCC, null);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getId(), is(offence.getId()));
//    }
//
//    @Test
//    public void shouldAddNewOffenceForDefendantWithOffence() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String defendantExternalId = STRING.next();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final List<Defendant> defendants = new ArrayList<>();
//        final UUID offenceId = randomUUID();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .withOffences(asList(offence().withId(offenceId).build()))
//                .build();
//        defendants.add(defendant);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withDefendants(defendants)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//
//        final Offence offence = offence().withId(randomUUID()).build();
//
//        caseAggregate.addOffences(caseId, caseUrn, defendantId, defendantExternalId, asList(offence), MCC, null);
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(1).getId(), is(offence.getId()));
//    }
//
//    @Test
//    public void shouldAddNewAndIgnoreExistingOffencesForDefendantWithSomeMatchingOffence() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String defendantExternalId = STRING.next();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final List<Defendant> defendants = new ArrayList<>();
//        final UUID offenceId = randomUUID();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .withOffences(asList(offence().withId(offenceId).build()))
//                .build();
//        defendants.add(defendant);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withDefendants(defendants)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//
//        final Offence offence = offence().withId(randomUUID()).build();
//
//        caseAggregate.addOffences(caseId, caseUrn, defendantId, defendantExternalId, asList(offence, offence().withId(offenceId).build()), MCC, null);
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getId(), is(offenceId));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(1).getId(), is(offence.getId()));
//    }
//
//
//    @Test
//    public void shouldAddProposedChargeForDefendantWithNoProposedCharge() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String defendantExternalId = STRING.next();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final List<Defendant> defendants = new ArrayList<>();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .build();
//        defendants.add(defendant);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withDefendants(defendants)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), nullValue());
//
//        final ProposedCharge proposedCharge = proposedCharge().withId(randomUUID()).build();
//
//        caseAggregate.addProposedCharges(caseId, caseUrn, defendantId, defendantExternalId, null, null, asList(proposedCharge), MCC, null);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getId(), is(proposedCharge.getId()));
//    }
//
//    @Test
//    public void shouldAddNewProposedChargeForDefendantWithProposedCharge() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String defendantExternalId = STRING.next();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final List<Defendant> defendants = new ArrayList<>();
//        final UUID proposedId = randomUUID();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .withProposedCharges(asList(proposedCharge().withId(proposedId).build()))
//                .build();
//        defendants.add(defendant);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withDefendants(defendants)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(1));
//
//        final ProposedCharge proposedCharge = proposedCharge().withId(randomUUID()).build();
//
//        caseAggregate.addProposedCharges(caseId, caseUrn, defendantId, defendantExternalId, null, null, asList(proposedCharge), MCC, null);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getId(), is(proposedId));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(1).getId(), is(proposedCharge.getId()));
//    }
//
//    @Test
//    public void shouldAddNewAndIgnoreExistingProposedChargesForDefendantWithSomeMatchingProposedCharges() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String defendantExternalId = STRING.next();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final List<Defendant> defendants = new ArrayList<>();
//        final UUID proposedId = randomUUID();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("John").withLastName("Brown").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .withProposedCharges(asList(proposedCharge().withId(proposedId).build()))
//                .build();
//        defendants.add(defendant);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withDefendants(defendants)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(1));
//
//
//        final ProposedCharge proposedCharge = proposedCharge().withId(randomUUID()).build();
//
//        caseAggregate.addProposedCharges(caseId, caseUrn, defendantId, defendantExternalId, null, null, asList(proposedCharge, proposedCharge().withId(proposedId).build()), MCC, null);
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getId(), is(proposedId));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(1).getId(), is(proposedCharge.getId()));
//    }
//
//
//    @Test
//    public void shouldAddProposedChargeForSuspectWithNoProposedCharge() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String suspectExternalId = STRING.next();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final List<Suspect> suspects = new ArrayList<>();
//
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("Brown").withLastName("John").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .build();
//
//        suspects.add(suspect);
//
//        caseAggregate.addPrechargeCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withSuspects(suspects)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), nullValue());
//
//        final ProposedCharge proposedCharge = proposedCharge().withId(randomUUID()).build();
//
//        caseAggregate.addProposedCharges(caseId, caseUrn, null, null, suspectId, suspectExternalId, asList(proposedCharge), MCC, null);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getId(), is(proposedCharge.getId()));
//    }
//
//    @Test
//    public void shouldAddNewProposedChargeForSuspectWithProposedCharge() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final String suspectExternalId = STRING.next();
//        final String caseUrn = STRING.next();
//
//        final UUID proposedId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("Brown").withLastName("John").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .withProposedCharges(asList(proposedCharge().withId(proposedId).build()))
//                .build();
//        final List<Suspect> suspects = new ArrayList<>();
//
//        suspects.add(suspect);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withSuspects(suspects)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(1));
//
//        final ProposedCharge proposedCharge = proposedCharge().withId(randomUUID()).build();
//
//        caseAggregate.addProposedCharges(caseId, caseUrn, null, null, suspectId, suspectExternalId, asList(proposedCharge), MCC, null);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getId(), is(proposedId));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(1).getId(), is(proposedCharge.getId()));
//    }
//
//    @Test
//    public void shouldAddNewAndIgnoreExistingProposedChargesForSuspectsWithSomeMatchingProposedCharges() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final String defendantExternalId = STRING.next();
//        final UUID suspectId = randomUUID();
//        final String suspectExternalId = STRING.next();
//        final String caseUrn = STRING.next();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final UUID proposedId = randomUUID();
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withOrganisation(organisation().withName("XYZ Test LTD").build())
//                .withPerson(Person.person().
//                        withName(Name.name().
//                                withForenames("Brown").withLastName("John").build())
//                        .withDateOfBirth(LocalDate.of(2000, 1, 1))
//                        .build())
//                .withProposedCharges(asList(proposedCharge().withId(proposedId).build()))
//                .build();
//        final List<Suspect> suspects = new ArrayList<>();
//
//        suspects.add(suspect);
//
//        caseAggregate.addChargedCase(caseDetails()
//                        .withCaseId(caseId)
//                        .withUrn(caseUrn)
//                        .withSuspects(suspects)
//                        .build(),
//                MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(1));
//
//
//        final ProposedCharge proposedCharge = proposedCharge().withId(randomUUID()).build();
//
//        caseAggregate.addProposedCharges(caseId, caseUrn, defendantId, defendantExternalId, suspectId, suspectExternalId, asList(proposedCharge, proposedCharge().withId(proposedId).build()), MCC, null);
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getId(), is(proposedId));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(1).getId(), is(proposedCharge.getId()));
//    }
//
//    private UserDetails buldUserDetail() {
//        return userDetails()
//                .build();
//    }
//
//    @Test
//    public void shouldUpdateVictim() {
//        final UUID victimId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withVictims(asList(victim()
//                        .withId(victimId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Victim updatedVictim = victim().withId(victimId).withPerson(person().withName(name().withForenames("Updated forenames").build()).build()).build();
//        Stream<Object> eventStream = caseAggregate.updatePersonVictim(caseId,
//                updatedVictim, "12345", null, MCC);
//        List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimUpdated.class));
//        PersonVictimUpdated personVictimUpdateReceived = (PersonVictimUpdated) eventList.get(0);
//        assertThat(personVictimUpdateReceived.getUrn(), is("12345"));
//        assertThat(personVictimUpdateReceived.getCaseId(), is(caseId));
//        assertThat(personVictimUpdateReceived.getVictim().getPerson().getName().getForenames(), is("Updated forenames"));
//        assertThat(personVictimUpdateReceived.getReason(), is(nullValue()));
//        assertThat(caseAggregate.getCaseDetails().getVictims().get(0).getPerson().getName().getForenames(), is("Updated forenames"));
//    }
//
//
//    @Test
//    public void shouldUpdateWitness() {
//        final UUID witnessId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withWitnesses(asList(witness()
//                        .withId(witnessId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Witness updatedWitness = witness().withId(witnessId).withPerson(person().withName(name().withForenames("Updated forenames").build()).build()).build();
//        Stream<Object> eventStream = caseAggregate.updatePersonWitness(caseId,
//                updatedWitness, "12345", null, MCC);
//        List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonWitnessUpdated.class));
//        PersonWitnessUpdated personWitnessUpdateReceived = (PersonWitnessUpdated) eventList.get(0);
//        assertThat(personWitnessUpdateReceived.getUrn(), is("12345"));
//        assertThat(personWitnessUpdateReceived.getCaseId(), is(caseId));
//        assertThat(personWitnessUpdateReceived.getWitness().getPerson().getName().getForenames(), is("Updated forenames"));
//        assertThat(personWitnessUpdateReceived.getReason(), is(nullValue()));
//        assertThat(caseAggregate.getCaseDetails().getWitnesses().get(0).getPerson().getName().getForenames(), is("Updated forenames"));
//
//    }
//
//    @Test
//    public void shouldUpdateVictimWitness() {
//        final UUID victimWitnessId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withVictimsAndWitnesses(asList(victimAndWitness()
//                        .withId(victimWitnessId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final VictimAndWitness updatedVictimWitness = victimAndWitness().withId(victimWitnessId).withPerson(person().withName(name().withForenames("Updated forenames").build()).build()).build();
//        Stream<Object> eventStream = caseAggregate.updatePersonVictimWitness(caseId,
//                updatedVictimWitness, "12345", null, MCC);
//        List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimWitnessUpdated.class));
//        PersonVictimWitnessUpdated personVictimWitnessUpdated = (PersonVictimWitnessUpdated) eventList.get(0);
//        assertThat(personVictimWitnessUpdated.getUrn(), is("12345"));
//        assertThat(personVictimWitnessUpdated.getCaseId(), is(caseId));
//        assertThat(personVictimWitnessUpdated.getVictimAndWitness().getPerson().getName().getForenames(), is("Updated forenames"));
//        assertThat(personVictimWitnessUpdated.getReason(), is(nullValue()));
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses().get(0).getPerson().getName().getForenames(), is("Updated forenames"));
//    }
//
//    @Test
//    public void shouldConvertWitnessToVictim() {
//        final UUID witnessId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withWitnesses(asList(witness()
//                        .withId(witnessId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Victim updateVictim = victim().withId(witnessId).withPerson(person().withName(name().withForenames("Updated forenames").build()).build()).build();
//        Stream<Object> eventStream = caseAggregate.updatePersonVictim(caseId,
//                updateVictim, "12345", null, MCC);
//        List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimUpdated.class));
//        PersonVictimUpdated personVictimUpdated = (PersonVictimUpdated) eventList.get(0);
//        assertThat(personVictimUpdated.getUrn(), is("12345"));
//        assertThat(personVictimUpdated.getCaseId(), is(caseId));
//        assertThat(personVictimUpdated.getVictim().getPerson().getName().getForenames(), is("Updated forenames"));
//        assertThat(personVictimUpdated.getReason(), is(nullValue()));
//        assertThat(caseAggregate.getCaseDetails().getWitnesses(), empty());
//        assertThat(caseAggregate.getCaseDetails().getVictims().get(0).getId(), is(witnessId));
//    }
//
//    @Test
//    public void shouldConvertVictimToWitness() {
//        final UUID victimId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withVictims(asList(victim()
//                        .withId(victimId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Witness updatedWitness = witness().withId(victimId).withPerson(person().withName(name().withForenames("Updated forenames").build()).build()).build();
//        Stream<Object> eventStream = caseAggregate.updatePersonWitness(caseId,
//                updatedWitness, "12345", null, MCC);
//        List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonWitnessUpdated.class));
//        PersonWitnessUpdated personWitnessUpdated = (PersonWitnessUpdated) eventList.get(0);
//        assertThat(personWitnessUpdated.getUrn(), is("12345"));
//        assertThat(personWitnessUpdated.getCaseId(), is(caseId));
//        assertThat(personWitnessUpdated.getWitness().getPerson().getName().getForenames(), is("Updated forenames"));
//        assertThat(personWitnessUpdated.getReason(), is(nullValue()));
//        assertThat(caseAggregate.getCaseDetails().getVictims(), is(empty()));
//        assertThat(caseAggregate.getCaseDetails().getWitnesses().get(0).getId(), is(victimId));
//    }
//
//    @Test
//    public void shouldConvertVictimToVictimWitness() {
//        final UUID victimId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withVictims(asList(victim()
//                        .withId(victimId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final VictimAndWitness updatedVictimWitness = victimAndWitness().withId(victimId).withPerson(person().withName(name().withForenames("Updated forenames").build()).build()).build();
//        Stream<Object> eventStream = caseAggregate.updatePersonVictimWitness(caseId,
//                updatedVictimWitness, "12345", null, MCC);
//        List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimWitnessUpdated.class));
//        PersonVictimWitnessUpdated personVictimWitnessUpdated = (PersonVictimWitnessUpdated) eventList.get(0);
//        assertThat(personVictimWitnessUpdated.getUrn(), is("12345"));
//        assertThat(personVictimWitnessUpdated.getCaseId(), is(caseId));
//        assertThat(personVictimWitnessUpdated.getVictimAndWitness().getPerson().getName().getForenames(), is("Updated forenames"));
//        assertThat(personVictimWitnessUpdated.getReason(), is(nullValue()));
//        assertThat(caseAggregate.getCaseDetails().getVictims(), is(empty()));
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses().get(0).getId(), is(victimId));
//    }
//
//    @Test
//    public void shouldConvertWitnessToVictimWitness() {
//        final UUID witnessId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withWitnesses(asList(witness()
//                        .withId(witnessId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final VictimAndWitness updatedVictimWitness = victimAndWitness().withId(witnessId).withPerson(person().withName(name().withForenames("Updated forenames").build()).build()).build();
//        Stream<Object> eventStream = caseAggregate.updatePersonVictimWitness(caseId,
//                updatedVictimWitness, "12345", null, MCC);
//        List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimWitnessUpdated.class));
//        PersonVictimWitnessUpdated personVictimWitnessUpdated = (PersonVictimWitnessUpdated) eventList.get(0);
//        assertThat(personVictimWitnessUpdated.getUrn(), is("12345"));
//        assertThat(personVictimWitnessUpdated.getCaseId(), is(caseId));
//        assertThat(personVictimWitnessUpdated.getVictimAndWitness().getPerson().getName().getForenames(), is("Updated forenames"));
//        assertThat(personVictimWitnessUpdated.getReason(), is(nullValue()));
//        assertThat(caseAggregate.getCaseDetails().getWitnesses(), is(empty()));
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses().get(0).getId(), is(witnessId));
//    }
//
//    @Test
//    public void shouldConvertVictimWitnessToVictim() {
//        final UUID victimWitnessId = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        caseAggregate.addChargedCase(caseDetails()
//                .withCaseId(caseId)
//                .withVictimsAndWitnesses(asList(victimAndWitness()
//                        .withId(victimWitnessId)
//                        .build()))
//                .build(), MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final Victim updatedVictim = victim().withId(victimWitnessId).withPerson(person().withName(name().withForenames("Updated forenames").build()).build()).build();
//        Stream<Object> eventStream = caseAggregate.updatePersonVictim(caseId,
//                updatedVictim, "12345", null, MCC);
//        List<?> eventList = eventStream.collect(toList());
//        assertThat("Unexpected number of events", eventList, hasSize(1));
//        assertThat("Unexpected event type", eventList.get(0), instanceOf(PersonVictimUpdated.class));
//        PersonVictimUpdated personVictimUpdated = (PersonVictimUpdated) eventList.get(0);
//        assertThat(personVictimUpdated.getUrn(), is("12345"));
//        assertThat(personVictimUpdated.getCaseId(), is(caseId));
//        assertThat(personVictimUpdated.getVictim().getPerson().getName().getForenames(), is("Updated forenames"));
//        assertThat(personVictimUpdated.getReason(), is(nullValue()));
//        assertThat(caseAggregate.getCaseDetails().getVictims().get(0).getId(), is(victimWitnessId));
//        assertThat(caseAggregate.getCaseDetails().getVictimsAndWitnesses(), empty());
//    }
//
//
//    @Test
//    public void shouldChangeSuspectToDefendantWhenOffenceAddedToSuspect() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final ProposedCharge proposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CJsCode")
//                .build();
//
//        final List<ProposedCharge> proposedCharges = new ArrayList<>();
//        proposedCharges.add(proposedCharge);
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("ForeName")
//                                .withLastName("LastName")
//                                .build())
//                        .build())
//                .withProposedCharges(proposedCharges)
//                .build();
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withSuspects(asList(suspect))
//                .build();
//        caseAggregate.addPrechargeCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(0));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getId(), is(suspectId));
//
//        final Offence offence = offence()
//                .withId(randomUUID())
//                .withCjsCode("Cjs Code")
//                .withDescription("DVLA")
//                .build();
//        final Stream<Object> privateEventStream = caseAggregate.addOffence(caseId, CASE_URN, suspectId, asList(offence), MCC, null, false);
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//
//        final SuspectCharged suspectCharged = (SuspectCharged) listOfStream.get(0);
//        assertThat(suspectCharged.getCaseId(), is(caseId));
//        assertThat(suspectCharged.getParticipantId(), is(suspectId));
//        assertThat(suspectCharged.getOffences().get(0).getId(), is(offence.getId()));
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), nullValue());
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getId(), is(suspectId));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getPerson().getName().getForenames(), is(suspect.getPerson().getName().getForenames()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getId(), is(proposedCharge.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getId(), is(offence.getId()));
//    }
//
//    @Test
//    public void shouldSuspectChangeToDefendantForCaseWithSuspectsAndDefendantWhenOffenceAddedToSuspect() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final ProposedCharge proposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CJsCode")
//                .build();
//
//        final List<ProposedCharge> proposedCharges = new ArrayList<>();
//        proposedCharges.add(proposedCharge);
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("ForeName")
//                                .withLastName("LastName")
//                                .build())
//                        .build())
//                .withProposedCharges(proposedCharges)
//                .build();
//
//        final Suspect secondSuspect = suspect()
//                .withId(randomUUID())
//                .withOrganisation(organisation().withName("Org Ltd").build())
//                .withProposedCharges(proposedCharges)
//                .build();
//
//        final List<Suspect> suspects = new ArrayList<>();
//        suspects.add(suspect);
//        suspects.add(secondSuspect);
//
//        final Defendant defendant = defendant()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .build();
//        final List<Defendant> defendants = new ArrayList<>();
//        defendants.add(defendant);
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withSuspects(suspects)
//                .withDefendants(defendants)
//                .build();
//        caseAggregate.addChargedCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getId(), is(suspectId));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(1).getId(), is(secondSuspect.getId()));
//
//        final Offence offence = offence()
//                .withId(randomUUID())
//                .withCjsCode("Cjs Code")
//                .withDescription("DVLA")
//                .build();
//        final Stream<Object> privateEventStream = caseAggregate.addOffence(caseId, CASE_URN, secondSuspect.getId(), asList(offence), MCC, null, false);
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//
//        final SuspectCharged suspectCharged = (SuspectCharged) listOfStream.get(0);
//        assertThat(suspectCharged.getCaseId(), is(caseId));
//        assertThat(suspectCharged.getParticipantId(), is(secondSuspect.getId()));
//        assertThat(suspectCharged.getOffences().get(0).getId(), is(offence.getId()));
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getId(), is(suspect.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getId(), is(defendant.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getId(), is(secondSuspect.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getOrganisation().getName(), is(secondSuspect.getOrganisation().getName()));
//    }
//
//    @Test
//    public void shouldSuspectChangeToDefendantForCaseWithSuspectsAndDefendantWhenSameMultiOffencesAddedToSuspect() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final UUID proposedChargeId1 = randomUUID();
//        final UUID proposedChargeId2 = randomUUID();
//
//        final ProposedCharge proposedCharge1 = proposedCharge()
//                .withId(proposedChargeId1)
//                .withCjsCode("CJsCode")
//                .build();
//
//        final ProposedCharge proposedCharge2 = proposedCharge()
//                .withId(proposedChargeId2)
//                .withCjsCode("CJsCode")
//                .build();
//        final ProposedCharge proposedCharge3 = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CJsCode")
//                .build();
//
//        final List<ProposedCharge> proposedCharges = new ArrayList<>();
//        proposedCharges.add(proposedCharge1);
//        proposedCharges.add(proposedCharge2);
//        proposedCharges.add(proposedCharge3);
//
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("ForeName")
//                                .withLastName("LastName")
//                                .build())
//                        .build())
//                .withProposedCharges(proposedCharges)
//                .build();
//
//        final Suspect secondSuspect = suspect()
//                .withId(randomUUID())
//                .withOrganisation(organisation().withName("Org Ltd").build())
//                .withProposedCharges(proposedCharges)
//                .build();
//
//        final List<Suspect> suspects = new ArrayList<>();
//        suspects.add(suspect);
//        suspects.add(secondSuspect);
//
//        final Defendant defendant = defendant()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .build();
//        final List<Defendant> defendants = new ArrayList<>();
//        defendants.add(defendant);
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withSuspects(suspects)
//                .withDefendants(defendants)
//                .build();
//        caseAggregate.addChargedCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getId(), is(suspectId));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(1).getId(), is(secondSuspect.getId()));
//
//        final Offence offence1 = offence()
//                .withId(proposedChargeId1)
//                .withCjsCode("Cjs Code")
//                .withDescription("DVLA")
//                .build();
//        final Offence offence2 = offence()
//                .withId(proposedChargeId2)
//                .withCjsCode("Cjs Code")
//                .withDescription("DVLA")
//                .build();
//        List<Offence> offences = asList(offence1, offence2);
//        final Stream<Object> privateEventStream = caseAggregate.addOffence(caseId, CASE_URN, secondSuspect.getId(), offences, MCC, null, true);
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//
//        final SuspectCharged suspectCharged = (SuspectCharged) listOfStream.get(0);
//        assertThat(suspectCharged.getCaseId(), is(caseId));
//        assertThat(suspectCharged.getParticipantId(), is(secondSuspect.getId()));
//        assertThat(suspectCharged.getOffences().get(0).getId(), is(offence1.getId()));
//        assertThat(suspectCharged.getOffences().get(1).getId(), is(offence2.getId()));
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getId(), is(suspect.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getId(), is(defendant.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getId(), is(secondSuspect.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getOrganisation().getName(), is(secondSuspect.getOrganisation().getName()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getOffences(), hasSize(2));
//    }
//
//    @Test
//    public void shouldSuspectChangeToDefendantForCaseWithSuspectsAndDefendantWhenDifferentMultiOffencesAddedToSuspect() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//
//        final ProposedCharge proposedCharge1 = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CJsCode")
//                .build();
//
//        final ProposedCharge proposedCharge2 = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CJsCode")
//                .build();
//        final ProposedCharge proposedCharge3 = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CJsCode")
//                .build();
//
//        final List<ProposedCharge> proposedCharges = new ArrayList<>();
//        proposedCharges.add(proposedCharge1);
//        proposedCharges.add(proposedCharge2);
//        proposedCharges.add(proposedCharge3);
//
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("ForeName")
//                                .withLastName("LastName")
//                                .build())
//                        .build())
//                .withProposedCharges(proposedCharges)
//                .build();
//
//        final Suspect secondSuspect = suspect()
//                .withId(randomUUID())
//                .withOrganisation(organisation().withName("Org Ltd").build())
//                .withProposedCharges(proposedCharges)
//                .build();
//
//        final List<Suspect> suspects = new ArrayList<>();
//        suspects.add(suspect);
//        suspects.add(secondSuspect);
//
//        final Defendant defendant = defendant()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .build();
//        final List<Defendant> defendants = new ArrayList<>();
//        defendants.add(defendant);
//
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withSuspects(suspects)
//                .withDefendants(defendants)
//                .build();
//        caseAggregate.addChargedCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getId(), is(suspectId));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(1).getId(), is(secondSuspect.getId()));
//
//        final Offence offence1 = offence()
//                .withId(randomUUID())
//                .withCjsCode("Cjs Code")
//                .withDescription("DVLA")
//                .build();
//        final Offence offence2 = offence()
//                .withId(randomUUID())
//                .withCjsCode("Cjs Code")
//                .withDescription("DVLA")
//                .build();
//        List<Offence> offences = asList(offence1, offence2);
//        final Stream<Object> privateEventStream = caseAggregate.addOffence(caseId, CASE_URN, secondSuspect.getId(), offences, MCC, null, true);
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//
//        final SuspectCharged suspectCharged = (SuspectCharged) listOfStream.get(0);
//        assertThat(suspectCharged.getCaseId(), is(caseId));
//        assertThat(suspectCharged.getParticipantId(), is(secondSuspect.getId()));
//        assertThat(suspectCharged.getOffences().get(0).getId(), is(offence1.getId()));
//        assertThat(suspectCharged.getOffences().get(1).getId(), is(offence2.getId()));
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getId(), is(suspect.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getId(), is(defendant.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getId(), is(secondSuspect.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getOrganisation().getName(), is(secondSuspect.getOrganisation().getName()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getProposedCharges(), hasSize(3));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getOffences(), hasSize(2));
//    }
//
//    @Test
//    public void shouldUpdateHearing() {
//        final UUID defendantId = randomUUID();
//        final UUID defendantId2 = randomUUID();
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withDefendants(asList(defendant()
//                                .withId(defendantId)
//                                .withNextHearing(nextHearing()
//                                        .withCourtId("Court ID")
//                                        .build())
//                                .build(),
//                        defendant()
//                                .withId(defendantId2)
//                                .withNextHearing(nextHearing()
//                                        .withCourtId("Court ID Another")
//                                        .build())
//                                .build()))
//                .build();
//
//        caseAggregate.addChargedCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        final NextHearing updatedHearing = nextHearing()
//                .withCourtId("Court ID Updated")
//                .build();
//        final Stream<Object> privateEventStream = caseAggregate.updateHearing(caseId, "URN12345",
//                defendantId, updatedHearing, null, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//
//        final HearingUpdated hearingUpdated = (HearingUpdated) listOfStream.get(0);
//        assertThat(hearingUpdated.getCaseId(), is(caseId));
//        assertThat(hearingUpdated.getHearing().getCourtId(), is("Court ID Updated"));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getNextHearing().getCourtId(), is("Court ID Another"));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getNextHearing().getCourtId(), is("Court ID Updated"));
//
//    }
//
//    @Test
//    public void shouldRemoveOffenceFromDefendant() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final Offence existingOffence = offence()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Offence existingOffence1 = offence()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withOffences(asList(existingOffence, existingOffence1))
//                .build();
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withDefendants(asList(defendant))
//                .build();
//        caseAggregate.addChargedCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(2));
//
//        final Stream<Object> privateEventStream = caseAggregate.removeOffence(caseId, CASE_URN, defendantId, existingOffence.getId(), NOT_REQUIRED, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final OffenceRemoved offenceRemoved = (OffenceRemoved) listOfStream.get(0);
//        assertThat(offenceRemoved.getDefendantId(), is(defendantId));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getId(), is(existingOffence1.getId()));
//    }
//
//    @Test
//    public void shouldRemoveOffenceFromRightDefendant() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final Offence existingOffence = offence()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Offence existingOffence1 = offence()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withOffences(asList(existingOffence, existingOffence1))
//                .build();
//
//        final Defendant defendant1 = defendant()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withOffences(asList(existingOffence1))
//                .build();
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withDefendants(asList(defendant, defendant1))
//                .build();
//        caseAggregate.addChargedCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getOffences(), hasSize(1));
//
//        final Stream<Object> privateEventStream = caseAggregate.removeOffence(caseId, CASE_URN, defendantId, existingOffence1.getId(), NOT_REQUIRED, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final OffenceRemoved offenceRemoved = (OffenceRemoved) listOfStream.get(0);
//        assertThat(offenceRemoved.getDefendantId(), is(defendantId));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getOffences(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getId(), is(existingOffence.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getOffences().get(0).getId(), is(existingOffence1.getId()));
//    }
//
//    @Test
//    public void shouldRemoveProposedChargeFromDefendant() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final ProposedCharge existingProposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withProposedCharges(asList(existingProposedCharge))
//                .build();
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withDefendants(asList(defendant))
//                .build();
//        caseAggregate.addChargedCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(1));
//
//        final Stream<Object> privateEventStream = caseAggregate.removeProposedCharge(caseId, CASE_URN, defendantId, null, existingProposedCharge.getId(), NOT_REQUIRED, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final ProposedChargeRemoved proposedChargeRemoved = (ProposedChargeRemoved) listOfStream.get(0);
//        assertThat(proposedChargeRemoved.getDefendantId(), is(defendantId));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(0));
//    }
//
//    @Test
//    public void shouldRemoveProposedChargeFromRightDefendantWithMultipleProposedCharges() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final ProposedCharge existingProposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final ProposedCharge existingProposedCharge1 = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withProposedCharges(asList(existingProposedCharge, existingProposedCharge1))
//                .build();
//
//        final Defendant defendant1 = defendant()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withProposedCharges(asList(existingProposedCharge1))
//                .build();
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withDefendants(asList(defendant, defendant1))
//                .build();
//        caseAggregate.addChargedCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getProposedCharges(), hasSize(1));
//
//        final Stream<Object> privateEventStream = caseAggregate.removeProposedCharge(caseId, CASE_URN, defendantId, null, existingProposedCharge1.getId(), NOT_REQUIRED, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final ProposedChargeRemoved proposedChargeRemoved = (ProposedChargeRemoved) listOfStream.get(0);
//        assertThat(proposedChargeRemoved.getDefendantId(), is(defendantId));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getId(), is(existingProposedCharge.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(1).getProposedCharges().get(0).getId(), is(existingProposedCharge1.getId()));
//    }
//
//    @Test
//    public void shouldRemoveProposedChargeFromSuspect() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final ProposedCharge existingProposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withProposedCharges(asList(existingProposedCharge))
//                .build();
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withSuspects(asList(suspect))
//                .build();
//        caseAggregate.addPrechargeCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(1));
//
//        final Stream<Object> privateEventStream = caseAggregate.removeProposedCharge(caseId, CASE_URN, null, suspectId, existingProposedCharge.getId(), NOT_REQUIRED, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final ProposedChargeRemoved proposedChargeRemoved = (ProposedChargeRemoved) listOfStream.get(0);
//        assertThat(proposedChargeRemoved.getSuspectId(), is(suspectId));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(0));
//    }
//
//    @Test
//    public void shouldRemoveProposedChargeFromRightSuspectWithMultipleProposedCharges() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final ProposedCharge existingProposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final ProposedCharge existingProposedCharge1 = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withProposedCharges(asList(existingProposedCharge, existingProposedCharge1))
//                .build();
//
//        final Suspect suspect1 = suspect()
//                .withId(randomUUID())
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withProposedCharges(asList(existingProposedCharge1))
//                .build();
//
//        final CaseDetails caseDetails = caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withSuspects(asList(suspect, suspect1))
//                .build();
//        caseAggregate.addPrechargeCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(1).getProposedCharges(), hasSize(1));
//
//        final Stream<Object> privateEventStream = caseAggregate.removeProposedCharge(caseId, CASE_URN, null, suspectId, existingProposedCharge1.getId(), NOT_REQUIRED, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final ProposedChargeRemoved proposedChargeRemoved = (ProposedChargeRemoved) listOfStream.get(0);
//        assertThat(proposedChargeRemoved.getSuspectId(), is(suspectId));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(1).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getId(), is(existingProposedCharge.getId()));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(1).getProposedCharges().get(0).getId(), is(existingProposedCharge1.getId()));
//    }
//
//    @Test
//    public void shouldUpdateProposedChargeForAGiveSuspect() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final ProposedCharge existingProposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withProposedCharges(asList(existingProposedCharge))
//                .build();
//
//        final CaseDetails caseDetails = CaseDetails.caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withSuspects(asList(suspect))
//                .build();
//        caseAggregate.addPrechargeCase(caseDetails, null, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getId(), is(existingProposedCharge.getId()));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getCjsCode(), is(existingProposedCharge.getCjsCode()));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getDescription(), is(existingProposedCharge.getDescription()));
//
//        final ProposedCharge updatedProposedCharge = proposedCharge()
//                .withId(existingProposedCharge.getId())
//                .withCjsCode("UpdatedCjsCode")
//                .withDescription("TV1")
//                .build();
//
//
//        final Stream<Object> privateEventStream = caseAggregate.updateProposedCharge(caseId, CASE_URN, null, suspectId, updatedProposedCharge, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final ProposedChargeUpdateReceived proposedChargeUpdateReceived = (ProposedChargeUpdateReceived) listOfStream.get(0);
//        assertThat(proposedChargeUpdateReceived.getProposedCharge().getId(), is(updatedProposedCharge.getId()));
//        assertThat(proposedChargeUpdateReceived.getSuspectId(), is(suspectId));
//
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getId(), is(existingProposedCharge.getId()));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getCjsCode(), is(updatedProposedCharge.getCjsCode()));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges().get(0).getDescription(), is(updatedProposedCharge.getDescription()));
//    }
//
//    @Test
//    public void shouldUpdateProposedChargeForAGivenDefendant() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final ProposedCharge existingProposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withProposedCharges(asList(existingProposedCharge))
//                .build();
//
//        final CaseDetails caseDetails = CaseDetails.caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withDefendants(asList(defendant))
//                .build();
//        caseAggregate.addChargedCase(caseDetails, null, null, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(1));
//
//        final ProposedCharge updatedProposedCharge = proposedCharge()
//                .withId(existingProposedCharge.getId())
//                .withCjsCode("UpdatedCjsCode")
//                .withDescription("TV1")
//                .build();
//
//        final Stream<Object> privateEventStream = caseAggregate.updateProposedCharge(caseId, CASE_URN, defendantId, null, updatedProposedCharge, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final ProposedChargeUpdateReceived proposedChargeUpdateReceived = (ProposedChargeUpdateReceived) listOfStream.get(0);
//        assertThat(proposedChargeUpdateReceived.getProposedCharge().getId(), is(updatedProposedCharge.getId()));
//        assertThat(proposedChargeUpdateReceived.getDefendantId(), is(defendantId));
//
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getId(), is(existingProposedCharge.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getCjsCode(), is(updatedProposedCharge.getCjsCode()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getDescription(), is(updatedProposedCharge.getDescription()));
//    }
//
//    @Test
//    public void shouldMoveSuspectToDefendantWithUpdatedOffenceAndRemoveFromProposedChargeWhenOffenceFoundInProposedChargeForSuspect() {
//        final UUID caseId = randomUUID();
//        final UUID suspectId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final ProposedCharge existingProposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Suspect suspect = suspect()
//                .withId(suspectId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withProposedCharges(asList(existingProposedCharge))
//                .build();
//
//        final CaseDetails caseDetails = CaseDetails.caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withSuspects(asList(suspect))
//                .build();
//        caseAggregate.addPrechargeCase(caseDetails, null, USER_DETAILS, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getSuspects().get(0).getProposedCharges(), hasSize(1));
//
//        final Offence offence = offence()
//                .withId(existingProposedCharge.getId())
//                .withCjsCode("Updated CjsCode")
//                .withDescription("TV1")
//                .build();
//
//
//        final Stream<Object> privateEventStream = caseAggregate.addOffence(caseId, CASE_URN, suspectId, asList(offence), MCC, null, true);
//
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final SuspectCharged suspectCharged = (SuspectCharged) listOfStream.get(0);
//        assertThat(suspectCharged.getOffenceWasProposedCharge(), is(true));
//
//        assertThat(caseAggregate.getCaseDetails().getSuspects(), nullValue());
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), empty());
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getId(), is(offence.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getCjsCode(), is(offence.getCjsCode()));
//    }
//
//    @Test
//    public void shouldUpdateOffenceForGivenDefendant() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final Offence existingOffence = offence()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withOffences(asList(existingOffence))
//                .build();
//
//        final CaseDetails caseDetails = CaseDetails.caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withDefendants(asList(defendant))
//                .build();
//        caseAggregate.addChargedCase(caseDetails, null, null, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getCjsCode(), is(existingOffence.getCjsCode()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getDescription(), is(existingOffence.getDescription()));
//
//        final Offence offence = offence()
//                .withId(existingOffence.getId())
//                .withCjsCode("Updates CjsCode")
//                .withDescription("DVLA")
//                .build();
//
//        final Stream<Object> privateEventStream = caseAggregate.updateOffence(caseId, CASE_URN, defendantId, offence, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final OffenceUpdateReceived offenceUpdateReceived = (OffenceUpdateReceived) listOfStream.get(0);
//        assertThat(offenceUpdateReceived.getDefendantId(), is(defendantId));
//        assertThat(offenceUpdateReceived.getOffence().getId(), is(offence.getId()));
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getCjsCode(), is(offence.getCjsCode()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getDescription(), is(offence.getDescription()));
//
//    }
//
//    @Test
//    public void shouldMoveProposedChargeToOffenceForGivenDefendant() {
//        final UUID caseId = randomUUID();
//        final UUID defendantId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = new ArrayList<>();
//        final Optional<String> linkedReasonId = Optional.empty();
//        final Offence existingOffence = offence()
//                .withId(randomUUID())
//                .withCjsCode("CjsCode")
//                .withDescription("TV")
//                .build();
//        final ProposedCharge proposedCharge = proposedCharge()
//                .withId(randomUUID())
//                .withCjsCode("Propose Charge Cjs Code")
//                .withDescription("TV")
//                .build();
//        final Defendant defendant = defendant()
//                .withId(defendantId)
//                .withPerson(person()
//                        .withName(name()
//                                .withForenames("DefForeName")
//                                .withLastName("DefLastName")
//                                .build())
//                        .build())
//                .withOffences(asList(existingOffence))
//                .withProposedCharges(asList(proposedCharge))
//                .build();
//
//        final CaseDetails caseDetails = CaseDetails.caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .withDefendants(asList(defendant))
//                .build();
//        caseAggregate.addChargedCase(caseDetails, null, null, linkedCaseIds, linkedReasonId);
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getCjsCode(), is(existingOffence.getCjsCode()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getDescription(), is(existingOffence.getDescription()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getDescription(), is(proposedCharge.getDescription()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges().get(0).getCjsCode(), is(proposedCharge.getCjsCode()));
//
//        final Offence offence = offence()
//                .withId(proposedCharge.getId())
//                .withCjsCode("Updates CjsCode")
//                .withDescription("DVLA")
//                .build();
//
//        final Stream<Object> privateEventStream = caseAggregate.updateOffence(caseId, CASE_URN, defendantId, offence, MCC, null);
//
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        final OffenceUpdateReceived offenceUpdateReceived = (OffenceUpdateReceived) listOfStream.get(0);
//        assertThat(offenceUpdateReceived.getDefendantId(), is(defendantId));
//        assertThat(offenceUpdateReceived.getOffence().getId(), is(offence.getId()));
//
//        assertThat(caseAggregate.getCaseDetails().getDefendants(), hasSize(1));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences(), hasSize(2));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getProposedCharges(), empty());
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getCjsCode(), is(existingOffence.getCjsCode()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(0).getDescription(), is(existingOffence.getDescription()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(1).getId(), is(proposedCharge.getId()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(1).getDescription(), is(offence.getDescription()));
//        assertThat(caseAggregate.getCaseDetails().getDefendants().get(0).getOffences().get(1).getCjsCode(), is(offence.getCjsCode()));
//
//    }
//
//    @Test
//    public void shouldRaiseCasesLinkedEventIfThereAreLinkedCases() {
//        final UUID caseId = randomUUID();
//        final UUID linkedcaseId1 = randomUUID();
//        final UUID linkedcaseId2 = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = asList(LinkedCaseIds.linkedCaseIds().withLinkedCaseId(linkedcaseId1).build(), LinkedCaseIds.linkedCaseIds().withLinkedCaseId(linkedcaseId2).build());
//        final Optional<String> linkedReasonId = Optional.of(randomUUID().toString());
//
//        final CaseDetails caseDetails = CaseDetails.caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .build();
//        final Stream<Object> privateEventStream = caseAggregate.addPrechargeCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(2));
//        PrechargeCaseAdded prechargeCaseAdded = (PrechargeCaseAdded) listOfStream.get(0);
//        assertThat(prechargeCaseAdded.getCaseDetails().getCaseId(), is(caseId));
//        CasesLinked casesLinked = (CasesLinked) listOfStream.get(1);
//        assertThat(casesLinked.getLinkedCaseIds().size(), is(2));
//        assertThat(casesLinked.getLinkedCaseIds().get(0).getLinkedCaseId(), is(linkedcaseId1));
//        assertThat(casesLinked.getLinkedCaseIds().get(1).getLinkedCaseId(), is(linkedcaseId2));
//    }
//
//    @Test
//    public void shouldNotRaiseCasesLinkedEventIfThereAreNoLinkedCases() {
//        final UUID caseId = randomUUID();
//        final List<LinkedCaseIds> linkedCaseIds = Collections.emptyList();
//        final Optional<String> linkedReasonId = Optional.empty();
//
//        final CaseDetails caseDetails = CaseDetails.caseDetails()
//                .withCaseId(caseId)
//                .withUrn(CASE_URN)
//                .build();
//        final Stream<Object> privateEventStream = caseAggregate.addPrechargeCase(caseDetails, MCC, USER_DETAILS, linkedCaseIds, linkedReasonId);
//        final List<Object> listOfStream = privateEventStream.collect(toList());
//        assertThat(listOfStream, hasSize(1));
//        PrechargeCaseAdded prechargeCaseAdded = (PrechargeCaseAdded) listOfStream.get(0);
//        assertThat(prechargeCaseAdded.getCaseDetails().getCaseId(), is(caseId));
//    }
//
//}
//
