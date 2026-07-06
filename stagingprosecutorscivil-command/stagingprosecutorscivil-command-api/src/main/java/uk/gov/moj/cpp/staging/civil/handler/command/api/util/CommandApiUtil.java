package uk.gov.moj.cpp.staging.civil.handler.command.api.util;

import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Gender;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDateCode;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.OffenceDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Organisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianNameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianOrganisation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

public class CommandApiUtil {

    public static @NonNull CSVParser getCsvParserWithRightConfig(final String csvContent) throws IOException {
        return CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreEmptyLines()
                .withIgnoreSurroundingSpaces()
                .withNullString(StringUtils.EMPTY)
                .parse(new StringReader(csvContent));
    }

    public static HearingDetails buildHearingDetails(final CSVRecord row) {
        return HearingDetails.hearingDetails()
                .withCourtHearingLocation(row.get("hearingDetails.courtHearingLocation"))
                .withDateOfHearing(LocalDate.parse(row.get("hearingDetails.dateOfHearing")))
                .withTimeOfHearing(row.get("hearingDetails.timeOfHearing"))
                .build();
    }

    public static ProsecutionCase buildProsecutionCase(final CSVRecord row) {
        return ProsecutionCase.prosecutionCase()
                .withUrn(row.get("prosecutionCases.urn"))
                .withInformant(row.get("prosecutionCases.informant"))
                .withCaseMarker(row.get("prosecutionCases.caseMarker"))
                .withRelatedReferenceNumber(row.get("prosecutionCases.relatedReferenceNumber"))
                .withPaymentReference(row.get("prosecutionCases.paymentReference"))
                .withDefendants(List.of(buildDefendant(row)))
                .build();
    }

    public static Defendant buildDefendant(final CSVRecord row) {
        final DefendantDetails details = DefendantDetails.defendantDetails()
                .withProsecutorDefendantId(row.get("defendants.defendantDetails.prosecutorDefendantId"))
                .withAsn(row.get("defendants.defendantDetails.asn"))
                .withPncIdentifier(row.get("defendants.defendantDetails.pncIdentifier"))
                .withCroNumber(row.get("defendants.defendantDetails.croNumber"))
                .withDocumentationLanguage(Language.valueFor(row.get("defendants.defendantDetails.documentationLanguage")).orElse(null))
                .withHearingLanguage(Language.valueFor(row.get("defendants.defendantDetails.hearingLanguage")).orElse(null))
                .withAddress(buildAddress(row, "defendants.defendantDetails.address."))
                .withNumPreviousConvictions(intOrNull(row.get("defendants.defendantDetails.numPreviousConvictions")))
                .withProsecutorCosts(row.get("defendants.defendantDetails.prosecutorCosts"))
                .build();

        final Defendant.Builder builder = Defendant.defendant()
                .withDefendantDetails(details)
                .withOffences(List.of(buildOffence(row)));

        final String orgName = row.get("defendants.organisation.organisationName");
        if (orgName != null && !orgName.isBlank()) {
            builder.withOrganisation(buildOrganisation(row));
        } else {
            builder.withIndividual(buildIndividual(row));
        }
        return builder.build();
    }

    public static Individual buildIndividual(final CSVRecord row) {
        final Individual.Builder b = Individual.individual()
                .withNameDetails(buildNameDetails(row, "defendants.individual.nameDetails."))
                .withContactDetails(buildContactDetails(row, "defendants.individual.contactDetails."))
                .withNationality(row.get("defendants.individual.nationality"))
                .withAdditionalNationality(row.get("defendants.individual.additionalNationality"))
                .withDateOfBirth(localDateOrNull(row.get("defendants.individual.dateOfBirth")))
                .withGender(Gender.valueFor(intOrNull(row.get("defendants.individual.gender"))).orElse(null))
                .withOccupation(row.get("defendants.individual.occupation"))
                .withOccupationCode(intOrNull(row.get("defendants.individual.occupationCode")))
                .withObservedEthnicity(decimalOrNull(row.get("defendants.individual.observedEthnicity")))
                .withEthnicity(row.get("defendants.individual.ethnicity"))
                .withDriverNumber(row.get("defendants.individual.driverNumber"))
                .withLanguageRequirement(row.get("defendants.individual.languageRequirement"))
                .withSpecificRequirements(row.get("defendants.individual.specificRequirements"))
                .withNationalInsuranceNumber(row.get("defendants.individual.nationalInsuranceNumber"))
                .withCustodyStatus(row.get("defendants.individual.custodyStatus"))
                .withBailConditions(row.get("defendants.individual.bailConditions"));

        final String aliasForename = row.get("defendants.individual.aliases.forename");
        if (aliasForename != null && !aliasForename.isBlank()) {
            b.withAliases(List.of(buildNameDetails(row, "defendants.individual.aliases.")));
        }

        final String pgOrgName = row.get("defendants.individual.parentGuardian.organisation.organisationName");
        final String pgIndividualForename = row.get("defendants.individual.parentGuardian.individual.nameDetails.forename");
        if (pgOrgName != null && !pgOrgName.isBlank()) {
            b.withParentGuardian(buildParentGuardianOrg(row));
        } else if (pgIndividualForename != null && !pgIndividualForename.isBlank()) {
            b.withParentGuardian(buildParentGuardianIndividual(row));
        }

        return b.build();
    }

    public static Organisation buildOrganisation(final CSVRecord row) {
        final String raw = row.get("defendants.organisation.aliasOrganisationNames");
        final List<String> aliases = (raw == null || raw.isBlank()) ? null : Arrays.asList(raw.split("\\|"));
        return Organisation.organisation()
                .withOrganisationName(row.get("defendants.organisation.organisationName"))
                .withCompanyTelephoneNumber(row.get("defendants.organisation.companyTelephoneNumber"))
                .withAliasOrganisationNames(aliases)
                .build();
    }

    public static ParentGuardian buildParentGuardianIndividual(final CSVRecord row) {
        final String prefix = "defendants.individual.parentGuardian.individual.nameDetails.";
        final ParentGuardianIndividual pgIndividual = ParentGuardianIndividual.parentGuardianIndividual()
                .withNameDetails(ParentGuardianNameDetails.parentGuardianNameDetails()
                        .withTitle(row.get(prefix + "title"))
                        .withForename(row.get(prefix + "forename"))
                        .withForename2(row.get(prefix + "forename2"))
                        .withForename3(row.get(prefix + "forename3"))
                        .withSurname(row.get(prefix + "surname"))
                        .build())
                .withContactDetails(buildContactDetails(row, "defendants.individual.parentGuardian.individual.contactDetails."))
                .withDateOfBirth(localDateOrNull(row.get("defendants.individual.parentGuardian.individual.dateOfBirth")))
                .withGender(Gender.valueFor(intOrNull(row.get("defendants.individual.parentGuardian.individual.gender"))).orElse(null))
                .withObservedEthnicity(decimalOrNull(row.get("defendants.individual.parentGuardian.individual.observedEthnicity")))
                .withSelfDefinedEthnicity(row.get("defendants.individual.parentGuardian.individual.selfDefinedEthnicity"))
                .build();
        return ParentGuardian.parentGuardian()
                .withIndividual(pgIndividual)
                .withAddress(buildAddress(row, "defendants.individual.parentGuardian.address."))
                .build();
    }

    public static ParentGuardian buildParentGuardianOrg(final CSVRecord row) {
        final ParentGuardianOrganisation pgOrg = ParentGuardianOrganisation.parentGuardianOrganisation()
                .withOrganisationName(row.get("defendants.individual.parentGuardian.organisation.organisationName"))
                .withCompanyTelephoneNumber(row.get("defendants.individual.parentGuardian.organisation.companyTelephoneNumber"))
                .build();
        return ParentGuardian.parentGuardian()
                .withOrganisation(pgOrg)
                .withAddress(buildAddress(row, "defendants.individual.parentGuardian.address."))
                .build();
    }

    public static Offence buildOffence(final CSVRecord row) {
        final OffenceDetails details = OffenceDetails.offenceDetails()
                .withCjsOffenceCode(row.get("offences.offenceDetails.cjsOffenceCode"))
                .withOffenceSequenceNo(intOrNull(row.get("offences.offenceDetails.offenceSequenceNo")))
                .withLaidDate(localDateOrNull(row.get("offences.offenceDetails.laidDate")))
                .withOffenceCommittedDate(localDateOrNull(row.get("offences.offenceDetails.offenceCommittedDate")))
                .withOffenceCommittedEndDate(localDateOrNull(row.get("offences.offenceDetails.offenceCommittedEndDate")))
                .withOffenceDateCode(OffenceDateCode.valueFor(intOrNull(row.get("offences.offenceDetails.offenceDateCode"))).orElse(null))
                .withOffenceLocation(row.get("offences.offenceDetails.offenceLocation"))
                .withOffenceWording(row.get("offences.offenceDetails.offenceWording"))
                .withOffenceWordingWelsh(row.get("offences.offenceDetails.offenceWordingWelsh"))
                .withProsecutorCompensation(row.get("offences.offenceDetails.prosecutorCompensation"))
                .withBackDuty(row.get("offences.offenceDetails.backDuty"))
                .withBackDutyDateFrom(localDateOrNull(row.get("offences.offenceDetails.backDutyDateFrom")))
                .withBackDutyDateTo(localDateOrNull(row.get("offences.offenceDetails.backDutyDateTo")))
                .withVehicleMake(row.get("offences.offenceDetails.vehicleMake"))
                .withVehicleRegistrationMark(row.get("offences.offenceDetails.vehicleRegistrationMark"))
                .build();
        return Offence.offence()
                .withOffenceDetails(details)
                .withArrestDate(localDateOrNull(row.get("offences.arrestDate")))
                .withStatementOfFacts(row.get("offences.statementOfFacts"))
                .withStatementOfFactsWelsh(row.get("offences.statementOfFactsWelsh"))
                .build();
    }

    public static NameDetails buildNameDetails(final CSVRecord row, final String prefix) {
        return NameDetails.nameDetails()
                .withTitle(row.get(prefix + "title"))
                .withForename(row.get(prefix + "forename"))
                .withForename2(row.get(prefix + "forename2"))
                .withForename3(row.get(prefix + "forename3"))
                .withSurname(row.get(prefix + "surname"))
                .build();
    }

    public static ContactDetails buildContactDetails(final CSVRecord row, final String prefix) {
        return ContactDetails.contactDetails()
                .withWorkTelephoneNumber(row.get(prefix + "workTelephoneNumber"))
                .withHomeTelephoneNumber(row.get(prefix + "homeTelephoneNumber"))
                .withMobileTelephoneNumber(row.get(prefix + "mobileTelephoneNumber"))
                .withPrimaryEmail(row.get(prefix + "primaryEmail"))
                .withSecondaryEmail(row.get(prefix + "secondaryEmail"))
                .build();
    }

    public static Address buildAddress(final CSVRecord row, final String prefix) {
        return Address.address()
                .withAddress1(row.get(prefix + "address1"))
                .withAddress2(row.get(prefix + "address2"))
                .withAddress3(row.get(prefix + "address3"))
                .withAddress4(row.get(prefix + "address4"))
                .withAddress5(row.get(prefix + "address5"))
                .withPostcode(row.get(prefix + "postcode"))
                .build();
    }

    private static LocalDate localDateOrNull(final String val) {
        return (val == null || val.isBlank()) ? null : LocalDate.parse(val);
    }

    private static Integer intOrNull(final String val) {
        return (val == null || val.isBlank()) ? null : Integer.parseInt(val);
    }

    private static BigDecimal decimalOrNull(final String val) {
        return (val == null || val.isBlank()) ? null : new BigDecimal(val);
    }

}
