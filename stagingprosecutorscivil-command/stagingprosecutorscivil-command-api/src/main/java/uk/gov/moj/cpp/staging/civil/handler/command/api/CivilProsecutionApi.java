package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static uk.gov.justice.services.core.annotation.Component.COMMAND_API;
import static uk.gov.justice.services.core.enveloper.Enveloper.envelop;
import static uk.gov.justice.services.messaging.Envelope.envelopeFrom;

import uk.gov.justice.services.adapter.rest.exception.BadRequestException;
import uk.gov.justice.services.common.configuration.Value;
import uk.gov.justice.services.core.annotation.Handles;
import uk.gov.justice.services.core.annotation.ServiceComponent;
import uk.gov.justice.services.core.json.JsonSchemaValidationException;
import uk.gov.justice.services.core.json.JsonSchemaValidator;
import uk.gov.justice.services.core.sender.Sender;
import uk.gov.justice.services.messaging.Envelope;
import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.moj.cpp.staging.civil.handler.command.api.uuid.UUIDProducer;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.ChargeProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.ChargeProsecutionWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SubmitMaterialWithSubmissionId;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecution;
import uk.gov.moj.cpp.staging.prosecutors.civil.command.api.SummonsProsecutionWithSubmissionId;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cpp.moj.gov.uk.staging.prosecutors.json.schemas.UrlResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServiceComponent(COMMAND_API)
public class CivilProsecutionApi {
    private static final Logger LOGGER = LoggerFactory.getLogger(CivilProsecutionApi.class);
    @Inject
    @Value(key = "stagingprosecutorscivil.submit-prosecution-response.base-url", defaultValue = "https://replace-me.gov.uk/")
    String baseResponseURL;

    private static final String RESPONSE_URL_VERSION_PLACEHOLDER = "VERSION";
    private static final String VERSION_NO = "v1";

    private final Sender sender;
    @Inject
    private UUIDProducer uuidProducer;

    @Inject
    private JsonSchemaValidator jsonSchemaValidator;

    @Inject
    public CivilProsecutionApi(final Sender sender) {
        this.sender = sender;
    }

    @Handles("stagingprosecutorscivil.charge-prosecution")
    public Envelope<UrlResponse> chargeProsecution(final Envelope<ChargeProsecution> envelope) {
        final UUID submissionId = UUID.randomUUID();
        final ChargeProsecution chargeProsecution = envelope.payload();

        final ChargeProsecutionWithSubmissionId chargeProsecutionWithSubmissionId
                = ChargeProsecutionWithSubmissionId.chargeProsecutionWithSubmissionId()
                .withProsecutionCases(chargeProsecution.getProsecutionCases())
                .withProsecutingAuthority(chargeProsecution.getProsecutingAuthority())
                .withHearingDetails(chargeProsecution.getHearingDetails())
                .withSubmissionId(submissionId)
                .build();

        LOGGER.info("Received submission at stagingprosecutorscivil.charge-prosecution  with submissionId {}",submissionId);
        sender.send(envelop(chargeProsecutionWithSubmissionId)
                .withName("stagingprosecutorscivil.command.charge-prosecution")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                .withSubmissionId(submissionId).build());


    }

    @Handles("stagingprosecutorscivil.summons-prosecution")
    public Envelope<UrlResponse> summonsProsecution(final Envelope<SummonsProsecution> envelope) {
        final UUID submissionId = UUID.randomUUID();
        final SummonsProsecution summonsProsecution = envelope.payload();
        final SummonsProsecutionWithSubmissionId summonsProsecutionWithSubmissionId
                = SummonsProsecutionWithSubmissionId.summonsProsecutionWithSubmissionId()
                .withProsecutionCases(summonsProsecution.getProsecutionCases())
                .withProsecutingAuthority(summonsProsecution.getProsecutingAuthority())
                .withHearingDetails(summonsProsecution.getHearingDetails())
                .withSubmissionId(submissionId)
                .build();
        LOGGER.info("Received submission at  stagingprosecutorscivil.summons-prosecution with submissionId {}",submissionId);
        sender.send(envelop(summonsProsecutionWithSubmissionId)
                .withName("stagingprosecutorscivil.command.summons-prosecution")
                .withMetadataFrom(envelope));

        return Envelope.envelopeFrom(envelope.metadata(),
                UrlResponse.urlResponse()
                        .withStatusURL(getBaseResponseURLWithVersion() + submissionId.toString())
                        .withSubmissionId(submissionId).build());

    }

    @Handles("stagingprosecutorscivil.submit-material")
    public Envelope<UrlResponse> submitMaterial(final JsonEnvelope envelope) {
        LOGGER.info("Received submission at stagingprosecutorscivil.submit-material");
        final String defendantIdField = "defendantId";
        final JsonObject requestPayload = envelope.payloadAsJsonObject();

        try {
            jsonSchemaValidator.validate(requestPayload.toString(), envelope.metadata().name());
        } catch (JsonSchemaValidationException e) {
            throw new BadRequestException("Error submitting material, request has schema violations", e);
        }

        final UUID submissionId = uuidProducer.generateUUID();
        final SubmitMaterialWithSubmissionId.Builder submitMaterialWithSubmissionIdBuilder = SubmitMaterialWithSubmissionId.submitMaterialWithSubmissionId()
                .withSubmissionId(submissionId)
                .withMaterialId(UUID.fromString(requestPayload.getString("material")))
                .withCaseUrn(requestPayload.getString("caseUrn"))
                .withMaterialType(requestPayload.getString("materialType"))
                .withProsecutingAuthority(requestPayload.getString("prosecutingAuthority"));

        if (requestPayload.containsKey(defendantIdField)) {
            submitMaterialWithSubmissionIdBuilder.withDefendantId(requestPayload.getString(defendantIdField));
        }

        sender.send(envelop(submitMaterialWithSubmissionIdBuilder.build())
                .withName("stagingprosecutorscivil.command.submit-material")
                .withMetadataFrom(envelope));

        return envelopeFrom(
                envelope.metadata(),
                new UrlResponse(getBaseResponseURLWithVersion() + submissionId, submissionId));
    }

    @Handles("stagingprosecutorscivil.upload-bulk-prosecution")
    public Envelope<UrlResponse> uploadBulkProsecution(final JsonEnvelope envelope) {
        LOGGER.info("Received submission at stagingprosecutorscivil.upload-bulk-prosecution");
        final String csvContent = envelope.payloadAsJsonObject().getString("file");

        final List<ProsecutionCase> prosecutionCases = new ArrayList<>();
        String prosecutingAuthority = null;
        HearingDetails hearingDetails = null;

        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(new StringReader(csvContent))) {
            for (final CSVRecord row : parser) {
                if (prosecutingAuthority == null) {
                    prosecutingAuthority = row.get("prosecutingAuthority");
                    hearingDetails = buildHearingDetails(row);
                }
                prosecutionCases.add(buildProsecutionCase(row));
            }
        } catch (final IOException e) {
            throw new BadRequestException("Failed to parse CSV", e);
        }

        final ChargeProsecution chargeProsecution = ChargeProsecution.chargeProsecution()
                .withProsecutingAuthority(prosecutingAuthority)
                .withHearingDetails(hearingDetails)
                .withProsecutionCases(prosecutionCases)
                .build();

        try {
            final String chargeJson = new ObjectMapper()
                    .findAndRegisterModules()
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                    .writeValueAsString(chargeProsecution);
            jsonSchemaValidator.validate(chargeJson, "stagingprosecutorscivil.charge-prosecution");
        } catch (final JsonSchemaValidationException e) {
            throw new BadRequestException("Charge prosecution schema validation failed", e);
        } catch (final JsonProcessingException e) {
            throw new BadRequestException("Failed to serialize charge prosecution payload", e);
        }

        return chargeProsecution(Envelope.envelopeFrom(envelope.metadata(), chargeProsecution));
    }

    private HearingDetails buildHearingDetails(final CSVRecord row) {
        return HearingDetails.hearingDetails()
                .withCourtHearingLocation(row.get("hearingDetails.courtHearingLocation"))
                .withDateOfHearing(LocalDate.parse(row.get("hearingDetails.dateOfHearing")))
                .withTimeOfHearing(row.get("hearingDetails.timeOfHearing"))
                .build();
    }

    private ProsecutionCase buildProsecutionCase(final CSVRecord row) {
        return ProsecutionCase.prosecutionCase()
                .withUrn(row.get("prosecutionCases.urn"))
                .withInformant(row.get("prosecutionCases.informant"))
                .withCaseMarker(row.get("prosecutionCases.caseMarker"))
                .withRelatedReferenceNumber(row.get("prosecutionCases.relatedReferenceNumber"))
                .withPaymentReference(row.get("prosecutionCases.paymentReference"))
                .withDefendants(List.of(buildDefendant(row)))
                .build();
    }

    private Defendant buildDefendant(final CSVRecord row) {
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

    private Individual buildIndividual(final CSVRecord row) {
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

    private Organisation buildOrganisation(final CSVRecord row) {
        final String raw = row.get("defendants.organisation.aliasOrganisationNames");
        final List<String> aliases = (raw == null || raw.isBlank()) ? null : Arrays.asList(raw.split("\\|"));
        return Organisation.organisation()
                .withOrganisationName(row.get("defendants.organisation.organisationName"))
                .withCompanyTelephoneNumber(row.get("defendants.organisation.companyTelephoneNumber"))
                .withAliasOrganisationNames(aliases)
                .build();
    }

    private ParentGuardian buildParentGuardianIndividual(final CSVRecord row) {
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

    private ParentGuardian buildParentGuardianOrg(final CSVRecord row) {
        final ParentGuardianOrganisation pgOrg = ParentGuardianOrganisation.parentGuardianOrganisation()
                .withOrganisationName(row.get("defendants.individual.parentGuardian.organisation.organisationName"))
                .withCompanyTelephoneNumber(row.get("defendants.individual.parentGuardian.organisation.companyTelephoneNumber"))
                .build();
        return ParentGuardian.parentGuardian()
                .withOrganisation(pgOrg)
                .withAddress(buildAddress(row, "defendants.individual.parentGuardian.address."))
                .build();
    }

    private Offence buildOffence(final CSVRecord row) {
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

    private NameDetails buildNameDetails(final CSVRecord row, final String prefix) {
        return NameDetails.nameDetails()
                .withTitle(row.get(prefix + "title"))
                .withForename(row.get(prefix + "forename"))
                .withForename2(row.get(prefix + "forename2"))
                .withForename3(row.get(prefix + "forename3"))
                .withSurname(row.get(prefix + "surname"))
                .build();
    }

    private ContactDetails buildContactDetails(final CSVRecord row, final String prefix) {
        return ContactDetails.contactDetails()
                .withWorkTelephoneNumber(row.get(prefix + "workTelephoneNumber"))
                .withHomeTelephoneNumber(row.get(prefix + "homeTelephoneNumber"))
                .withMobileTelephoneNumber(row.get(prefix + "mobileTelephoneNumber"))
                .withPrimaryEmail(row.get(prefix + "primaryEmail"))
                .withSecondaryEmail(row.get(prefix + "secondaryEmail"))
                .build();
    }

    private Address buildAddress(final CSVRecord row, final String prefix) {
        return Address.address()
                .withAddress1(row.get(prefix + "address1"))
                .withAddress2(row.get(prefix + "address2"))
                .withAddress3(row.get(prefix + "address3"))
                .withAddress4(row.get(prefix + "address4"))
                .withAddress5(row.get(prefix + "address5"))
                .withPostcode(row.get(prefix + "postcode"))
                .build();
    }

    private LocalDate localDateOrNull(final String val) {
        return (val == null || val.isBlank()) ? null : LocalDate.parse(val);
    }

    private Integer intOrNull(final String val) {
        return (val == null || val.isBlank()) ? null : Integer.parseInt(val);
    }

    private BigDecimal decimalOrNull(final String val) {
        return (val == null || val.isBlank()) ? null : new BigDecimal(val);
    }

    private String getBaseResponseURLWithVersion() {
        return this.baseResponseURL.replace(RESPONSE_URL_VERSION_PLACEHOLDER, VERSION_NO);
    }

}
