package uk.gov.moj.cpp.staging.civil.processor.utils;

import static java.time.LocalDate.parse;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.UUID.fromString;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.rangeClosed;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address.address;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails.contactDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails.defendantDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails.hearingDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails.nameDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails.offenceDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian.parentGuardian;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianIndividual.parentGuardianIndividual;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianNameDetails.parentGuardianNameDetails;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianOrganisation.parentGuardianOrganisation;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase.prosecutionCase;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.SelfDefinedInformation.selfDefinedInformation;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Channel;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Gender;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDateCode;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Organisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.CivilProsecutionSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.GroupSubmissionSucceeded;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicCivilProsecutionRejected;
import uk.gov.moj.cps.prosecutioncasefile.domain.event.PublicGroupProsecutionRejected;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Prosecutors {

    public static final String URN = "TVL123456";
    public static final UUID SUBMISSION_ID = fromString("be5bb607-0e98-43a3-93da-123741c6f73d");
    public static final String TIME_OF_HEARING = "Time  Hearing";
    public static final String COURT_HEARING_LOCATION = "Court Hearing Location";
    public static final String INFORMANT = "informant";
    public static final String CASE_MARKER = "ABC";
    public static final String PAYMENT_REFERENCE = "ref_number";
    public static final String ASN = "asn";
    public static final String CRO_NUMBER = "CroNumber";
    public static final String PNC_IDENTIFIER = "PnCidentifier";
    public static final String ORGANISATION_NAME = "Organisation Name";
    public static final String COMPANY_TELEPHONE_NUMBER = "12323453456";

    public static List<Offence> prosecutorsOffenceList(final int numberElements) {
        return rangeClosed(1, numberElements)
                .mapToObj(Prosecutors::prosecutorsOffence)
                .collect(toList());
    }

    public static Offence prosecutorsOffence(final int sequenceNo) {

        return Offence.offence()
                .withOffenceDetails(offenceDetails()
                        .withBackDuty(("12"))
                        .withBackDutyDateFrom((parse("2018-02-02")))
                        .withBackDutyDateTo((parse("2018-02-20")))
                        .withCjsOffenceCode("PS0000" + sequenceNo)
                        .withOffenceCommittedDate(parse("2018-02-25"))
                        .withOffenceCommittedEndDate((parse("2018-02-25")))
                        .withOffenceDateCode(OffenceDateCode.NUMBER_1)
                        .withOffenceLocation(("fenceLocation"))
                        .withOffenceSequenceNo(sequenceNo)
                        .withOffenceWording("fenceWording")
                        .withOffenceWordingWelsh(("fenceWordingWelsh"))
                        .withProsecutorCompensation(("23"))
                        .withVehicleMake(("Ford"))
                        .withVehicleRegistrationMark(("AA11 ABC"))
                        .build())
                .withStatementOfFacts("Statement  facts")
                .withStatementOfFactsWelsh("Statement  facts welsh")
                .withArrestDate(parse("2018-02-02"))
                .build();

    }

    public static ContactDetails prosecutorsContactDetails() {
        return contactDetails()
                .withPrimaryEmail("address1@email.com")
                .withSecondaryEmail("address2@email.com")
                .withWorkTelephoneNumber("02012345678")
                .withHomeTelephoneNumber("02087654321")
                .withMobileTelephoneNumber("0789123456")
                .build();
    }


    public static Address prosecutorsAddress() {
        return address()
                .withAddress1("address1") //CHECKED
                .withAddress2("address2") //CHECKED
                .withAddress3("address3") //CHECKED
                .withAddress4("address4") //CHECKED
                .withAddress5("address5") //NOT IN ATCM - added by Ivo
                .withPostcode("postc ode") //CHECKED
                .build();
    }

    public static SelfDefinedInformation prosecutorsSelfDefinedInformation() {
        return selfDefinedInformation()
                .withGender(1) //MAGIC NUMBER FOR GENDER DEFINED IN CJS DATA STANDARDS...
                .withEthnicity("W1")
                .build();
    }

    public static Individual prosecutorsIndividualWithParentGuardianIndividual() {
        return prosecutorsIndividual()
                .withParentGuardian((prosecutorsParentGuardianIndividual()))
                .build();
    }

    public static Individual prosecutorsIndividualWithParentGuardianOrganisation() {
        return prosecutorsIndividual()
                .withParentGuardian((prosecutorsParentGuardianOrganisation()))
                .build();
    }

    public static PublicGroupProsecutionRejected updateCivilCaseReceived() {
        return PublicGroupProsecutionRejected.publicGroupProsecutionRejected()
                .withExternalId(SUBMISSION_ID)
                .withCaseErrors(asList(
                        Problem.problem()
                                .withCode("ERR01")
                                .build()))
                .withDefendantErrors(asList(
                        DefendantProblem.defendantProblem()
                                .withProblems(asList(
                                        Problem.problem()
                                                .withCode("ERR02")
                                                .build()))
                                .build()))
                .withGroupCaseErrors(asList(
                        Problem.problem()
                                .withCode("ERR03")
                                .build()))
                .build();
    }

    public static PublicCivilProsecutionRejected updateCivilProsecutionCaseReceived() {
        return PublicCivilProsecutionRejected.publicCivilProsecutionRejected()
                .withExternalId(SUBMISSION_ID)
                .withCaseErrors(asList(
                        Problem.problem()
                                .withCode("ERR01")
                                .build()))
                .withDefendantErrors(asList(
                        DefendantProblem.defendantProblem()
                                .withProblems(asList(
                                        Problem.problem()
                                                .withCode("ERR02")
                                                .build()))
                                .build()))
                .build();
    }

    public static CivilProsecutionSubmissionSucceeded updateCivilProsecutionSubmissionSucceeded() {
        return CivilProsecutionSubmissionSucceeded.civilProsecutionSubmissionSucceeded()
                .withExternalId(SUBMISSION_ID)
                .withChannel(Channel.CIVIL)
                .build();
    }

    public static GroupSubmissionSucceeded updateGroupSubmissionSucceeded() {
        return GroupSubmissionSucceeded.groupSubmissionSucceeded()
                .withExternalId(SUBMISSION_ID)
                .withGroupId(randomUUID())
                .build();
    }

    private static Individual.Builder prosecutorsIndividual() {
        return individual()
                .withAliases(asList(prosecutorsNameDetails(), prosecutorsNameDetails()))
                .withContactDetails(prosecutorsContactDetails())
                .withDateOfBirth(LocalDate.now())
                .withDriverNumber(("Driver Number"))
                .withEthnicity(("Ethnicity"))
                .withGender(Gender.NUMBER_1)
                .withNameDetails(prosecutorsNameDetails())
                .withNationalInsuranceNumber(("National Insurance Number"))
                .withObservedEthnicity((BigDecimal.ONE))
                .withOccupation(("Occupation"))
                .withCustodyStatus(("E"))
                .withBailConditions(("BAIL CONDITIONS"))
                .withLanguageRequirement(("languageNeeds"))
                .withSpecificRequirements(("specialNeeds"))
                .withOccupationCode((1));
    }

    private static NameDetails prosecutorsNameDetails() {
        return nameDetails()
                .withForename("Adam")
                .withForename2(("forename2"))
                .withForename3(("forename3"))
                .withSurname("SMITH")
                .withTitle(("Mr"))
                .build();
    }

    private static ParentGuardian prosecutorsParentGuardianIndividual() {
        return parentGuardian()
                .withAddress((prosecutorsAddress()))
                .withIndividual((parentGuardianIndividual()
                        .withContactDetails((prosecutorsContactDetails()))
                        .withDateOfBirth(LocalDate.now())
                        .withGender(Gender.NUMBER_2)
                        .withNameDetails(parentGuardianNameDetails()
                                .withForename(("Forename"))
                                .withForename2(("Forename2"))
                                .withForename3(("Forename3"))
                                .withSurname("Surname")
                                .withTitle(("Mr"))
                                .build())
                        .withObservedEthnicity((BigDecimal.ONE))
                        .withSelfDefinedEthnicity(("Self Define Ethnicity"))
                        .build()))
                .build();
    }

    private static ParentGuardian prosecutorsParentGuardianOrganisation() {
        return parentGuardian()
                .withAddress((prosecutorsAddress()))
                .withOrganisation((parentGuardianOrganisation()
                        .withOrganisationName("Organisation Name")
                        .withCompanyTelephoneNumber(("1111111111"))
                        .build()))
                .withIndividual(null)
                .build();
    }

    public static Defendant prosecutorsDefendant() {
        final List<Offence> offenceList = asList(prosecutorsOffence(1), prosecutorsOffence(1), prosecutorsOffence(1));
        return Defendant.defendant()
                .withDefendantDetails(defendantDetails()
                        .withAddress(prosecutorsAddress())
                        .withAsn(ASN)
                        .withCroNumber(CRO_NUMBER)
                        .withDocumentationLanguage(Language.E)
                        .withHearingLanguage(Language.E)
                        .withNumPreviousConvictions(1)
                        .withPncIdentifier(PNC_IDENTIFIER)
                        .withProsecutorCosts(BigDecimal.TEN.toString())
                        .withProsecutorDefendantId(randomUUID().toString())
                        .build())
                .withOffences(offenceList)
                .withIndividual(prosecutorsIndividual().build())
                .withOrganisation(Organisation.organisation()
                        .withOrganisationName(ORGANISATION_NAME)
                        .withCompanyTelephoneNumber(COMPANY_TELEPHONE_NUMBER)
                        .withAliasOrganisationNames(asList("Alias1", "Alias2", "Alias3"))
                        .build())
                .build();
    }

    public static Defendant prosecutorsDefendantWithDifferentOffences() {
        final List<Offence> offenceList = asList(prosecutorsOffence(1), prosecutorsOffence(2), prosecutorsOffence(3));
        return Defendant.defendant()
                .withDefendantDetails(defendantDetails()
                        .withAddress(prosecutorsAddress())
                        .withAsn(ASN)
                        .withCroNumber(CRO_NUMBER)
                        .withDocumentationLanguage(Language.E)
                        .withHearingLanguage(Language.E)
                        .withNumPreviousConvictions(1)
                        .withPncIdentifier(PNC_IDENTIFIER)
                        .withProsecutorCosts(BigDecimal.TEN.toString())
                        .withProsecutorDefendantId(randomUUID().toString())
                        .build())
                .withOffences(offenceList)
                .withIndividual(prosecutorsIndividual().build())
                .withOrganisation(Organisation.organisation()
                        .withOrganisationName(ORGANISATION_NAME)
                        .withCompanyTelephoneNumber(COMPANY_TELEPHONE_NUMBER)
                        .withAliasOrganisationNames(asList("Alias1", "Alias2", "Alias3"))
                        .build())
                .build();
    }

    public static ChargeProsecutionReceived ccChargeProsecutionReceived() {
        return ChargeProsecutionReceived.chargeProsecutionReceived()
                .withSubmissionId(SUBMISSION_ID)
                .withProsecutionCases(Arrays.asList(chargeProsecutionCaseDetail(), chargeProsecutionCaseDetail()))
                .withHearingDetails(hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .withTimeOfHearing(TIME_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .build();
    }
    public static ChargeProsecutionReceived groupChargeProsecutionReceived() {
        return ChargeProsecutionReceived.chargeProsecutionReceived()
                .withSubmissionId(SUBMISSION_ID)
                .withProsecutionCases(Arrays.asList(chargeProsecutionCaseDetail(), chargeProsecutionCaseDetail()))
                .withHearingDetails(hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .withTimeOfHearing(TIME_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .build();
    }

    public static SummonsProsecutionReceived groupSummonsProsecutionReceived() {
        return SummonsProsecutionReceived.summonsProsecutionReceived()
                .withSubmissionId(SUBMISSION_ID)
                .withProsecutionCases(Arrays.asList(summonsProsecutionCaseDetail(), summonsProsecutionCaseDetail()))
                .withHearingDetails(hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .withTimeOfHearing(TIME_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .build();
    }

    public static ChargeProsecutionReceived chargeProsecutionReceived() {
        return ChargeProsecutionReceived.chargeProsecutionReceived()
                .withSubmissionId(SUBMISSION_ID)
                .withProsecutionCases(Arrays.asList(chargeProsecutionCaseDetail()))
                .withHearingDetails(hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .withTimeOfHearing(TIME_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .build();
    }

    public static SummonsProsecutionReceived summonsProsecutionReceived() {
        return SummonsProsecutionReceived.summonsProsecutionReceived()
                .withSubmissionId(SUBMISSION_ID)
                .withProsecutionCases(Arrays.asList(summonsProsecutionCaseDetail()))
                .withHearingDetails(hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .withTimeOfHearing(TIME_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .build();
    }

    public static ProsecutionCase chargeProsecutionCaseDetail() {
        return prosecutionCase()
                .withUrn(URN)
                .withInformant(INFORMANT)
                .withCaseMarker(CASE_MARKER)
                .withDefendants(singletonList(prosecutorsDefendant()))
                .withPaymentReference(PAYMENT_REFERENCE)
                .build();
    }

    public static ProsecutionCase summonsProsecutionCaseDetail() {
        return prosecutionCase()
                .withUrn(URN)
                .withInformant(INFORMANT)
                .withCaseMarker(CASE_MARKER)
                .withDefendants(singletonList(prosecutorsDefendant()))
                .withPaymentReference(PAYMENT_REFERENCE)
                .withSummonsCode("summons_code")
                .build();
    }

    public static ChargeProsecutionReceived prosecutorsProsecutionReceivedWithMultipleDefendent() {
        return ChargeProsecutionReceived.chargeProsecutionReceived()
                .withSubmissionId(SUBMISSION_ID)
                .withProsecutionCases(singletonList(prosecutionCaseDetailWithMultipleDefendant()))
                .withHearingDetails(hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .withTimeOfHearing(TIME_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .build();
    }

    public static ChargeProsecutionReceived prosecutorsProsecutionReceivedWithDefendentHavingDifferentOffences() {
        return ChargeProsecutionReceived.chargeProsecutionReceived()
                .withSubmissionId(SUBMISSION_ID)
                .withProsecutionCases(singletonList(prosecutionCaseDetailWithDefendantAndMultipleOffences()))
                .withHearingDetails(hearingDetails()
                        .withDateOfHearing(LocalDate.now())
                        .withTimeOfHearing(TIME_OF_HEARING)
                        .withCourtHearingLocation(COURT_HEARING_LOCATION)
                        .build())
                .build();
    }

    public static ProsecutionCase prosecutionCaseDetailWithMultipleDefendant() {
        return prosecutionCase()
                .withUrn(URN)
                .withInformant(INFORMANT)
                .withCaseMarker(CASE_MARKER)
                .withDefendants(asList(prosecutorsDefendant(), prosecutorsDefendant()))
                .withPaymentReference(PAYMENT_REFERENCE)
                .build();
    }

    public static ProsecutionCase prosecutionCaseDetailWithDefendantAndMultipleOffences() {
        return prosecutionCase()
                .withUrn(URN)
                .withInformant(INFORMANT)
                .withCaseMarker(CASE_MARKER)
                .withDefendants(singletonList(prosecutorsDefendantWithDifferentOffences()))
                .withPaymentReference(PAYMENT_REFERENCE)
                .build();
    }
}
