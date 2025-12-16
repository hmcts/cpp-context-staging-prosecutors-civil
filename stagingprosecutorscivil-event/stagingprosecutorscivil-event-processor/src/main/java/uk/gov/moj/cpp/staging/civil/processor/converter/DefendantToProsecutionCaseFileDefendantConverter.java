package uk.gov.moj.cpp.staging.civil.processor.converter;

import static java.util.Optional.ofNullable;
import static uk.gov.justice.cps.prosecutioncasefile.InitialHearing.initialHearing;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address.address;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias.individualAlias;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Language.valueOf;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.IndividualAlias;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Organisation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DefendantToProsecutionCaseFileDefendantConverter implements Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> {

    final HearingDetails hearingDetails;
    private final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual> individualToProsecutionCaseFileIndividualConverter
            = new IndividualToProsecutionCaseFileIndividualConverter();
    private final Converter<List<Offence>, List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence>> offenceToProsecutionCaseFileOffenceConverter
            = new OffenceToProsecutionCaseFileOffenceConverter();

    public DefendantToProsecutionCaseFileDefendantConverter(final HearingDetails hearingDetails) {
        this.hearingDetails = hearingDetails;
    }

    @Override
    public uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant convert(final Defendant defendant) {
        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.Builder pcfDefendantBuilder = uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant.defendant()
                .withId(UUID.randomUUID().toString())
                .withAsn(ofNullable(defendant.getDefendantDetails()).map(DefendantDetails::getAsn).orElse(null))
                .withAddress(ofNullable(defendant.getDefendantDetails()).map(d -> buildAddress(d.getAddress())).orElse(null))
                .withInitialHearing(buildInitialHearing())
                .withCroNumber(ofNullable(defendant.getDefendantDetails()).map(DefendantDetails::getCroNumber).orElse(null))
                .withDocumentationLanguage(valueOf(ofNullable(defendant.getDefendantDetails()).map(DefendantDetails::getDocumentationLanguage).map(Language::name).orElse(null)))
                .withEmailAddress1(ofNullable(defendant.getIndividual()).map(Individual::getContactDetails).map(ContactDetails::getPrimaryEmail).orElse(null))
                .withEmailAddress2(ofNullable(defendant.getIndividual()).map(Individual::getContactDetails).map(ContactDetails::getSecondaryEmail).orElse(null))
                .withHearingLanguage(valueOf(ofNullable(defendant.getDefendantDetails()).map(DefendantDetails::getHearingLanguage).map(Language::name).orElse(null)))
                .withIndividual(individualToProsecutionCaseFileIndividualConverter.convert(defendant))
                .withNumPreviousConvictions(ofNullable(defendant.getDefendantDetails()).map(DefendantDetails::getNumPreviousConvictions).orElse(null))
                .withOffences(offenceToProsecutionCaseFileOffenceConverter.convert(defendant.getOffences()))
                .withOrganisationName(ofNullable(defendant.getOrganisation()).map(Organisation::getOrganisationName).orElse(null))
                .withPncIdentifier(ofNullable(defendant.getDefendantDetails()).map(DefendantDetails::getPncIdentifier).orElse(null))
                .withProsecutorDefendantReference(ofNullable(defendant.getDefendantDetails()).map(DefendantDetails::getProsecutorDefendantId).orElse(null))
                .withAppliedProsecutorCosts(ofNullable(defendant.getDefendantDetails()).map(DefendantDetails::getProsecutorCosts).map(BigDecimal::new).orElse(null))
                .withTelephoneNumberBusiness(ofNullable(defendant.getOrganisation()).map(Organisation::getCompanyTelephoneNumber).orElse(null));

        ofNullable(defendant.getIndividual()).ifPresent(individual -> {
            if(ofNullable(individual.getAliases()).isPresent()) {
                pcfDefendantBuilder.withIndividualAliases(buildIndividualAliases(individual.getAliases()));
            }
            pcfDefendantBuilder
                    .withLanguageRequirement(ofNullable(defendant.getIndividual()).map(Individual::getLanguageRequirement).orElse(null))
                    .withSpecificRequirements(ofNullable(defendant.getIndividual()).map(Individual::getSpecificRequirements).orElse(null))
                    .withCustodyStatus(ofNullable(defendant.getIndividual()).map(Individual::getCustodyStatus).orElse(null));
        });
        ofNullable(defendant.getOrganisation()).ifPresent(organisation -> pcfDefendantBuilder.withAliasForCorporate(organisation.getAliasOrganisationNames()));

        return pcfDefendantBuilder.build();
    }

    private InitialHearing buildInitialHearing() {

        return initialHearing()
                .withTimeOfHearing(this.hearingDetails.getTimeOfHearing())
                .withCourtHearingLocation(this.hearingDetails.getCourtHearingLocation())
                .withDateOfHearing(this.hearingDetails.getDateOfHearing().toString())
                .build();
    }

    private List<IndividualAlias> buildIndividualAliases(final List<NameDetails> aliases) {
        final List<IndividualAlias> individualAliases = new ArrayList<>();
        aliases.forEach(alias ->
                    individualAliases.add(individualAlias()
                            .withTitle(ofNullable(alias.getTitle()).orElse(null))
                            .withFirstName(alias.getForename())
                            .withGivenName2(ofNullable(alias.getForename2()).orElse(null))
                            .withGivenName3(ofNullable(alias.getForename3()).orElse(null))
                            .withLastName(alias.getSurname())
                            .build())

        );
        return individualAliases;
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address buildAddress(final Address address) {
        if (null == address) {
            return null;
        }

        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address.Builder builder = address()
                .withAddress1(address.getAddress1())
                .withPostcode(formatPostcode(ofNullable(address.getPostcode()).orElse(null)));

        ofNullable(address.getAddress2()).ifPresent(builder::withAddress2);
        ofNullable(address.getAddress3()).ifPresent(builder::withAddress3);
        ofNullable(address.getAddress4()).ifPresent(builder::withAddress4);
        ofNullable(address.getAddress5()).ifPresent(builder::withAddress5);

        return builder.build();
    }

    private String formatPostcode(final String postcode) {
        if(postcode == null) {
            return null;
        }

        final StringBuilder postCodeBuilder = new StringBuilder(postcode.replaceAll("\\s",""));
        postCodeBuilder.insert(postCodeBuilder.length() - 3, " ");
        return postCodeBuilder.toString();
    }

}