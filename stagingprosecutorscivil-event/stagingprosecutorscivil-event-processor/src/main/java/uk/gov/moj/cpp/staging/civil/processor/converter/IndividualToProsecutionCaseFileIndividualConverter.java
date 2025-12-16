package uk.gov.moj.cpp.staging.civil.processor.converter;

import static java.lang.Integer.parseInt;
import static java.util.Objects.isNull;
import static java.util.Optional.ofNullable;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address.address;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails.contactDetails;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual.individual;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation.parentGuardianInformation;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation.selfDefinedInformation;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianIndividual;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianNameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardianOrganisation;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.function.Function;

public class IndividualToProsecutionCaseFileIndividualConverter implements Converter<Defendant, Individual> {

    private static final Converter<Integer, Gender> integerGenderToProsecutionCaseFileGenderConverter = new IntegerGenderToProsecutionCaseFileGenderConverter();

    private final Function<ContactDetails, uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails> convertContactDetailsFnc =
            prosecutionContactDetails ->
                    contactDetails()
                            .withHome(ofNullable(prosecutionContactDetails.getHomeTelephoneNumber()).orElse(null))
                            .withMobile(ofNullable(prosecutionContactDetails.getMobileTelephoneNumber()).orElse(null))
                            .withPrimaryEmail(ofNullable(prosecutionContactDetails.getPrimaryEmail()).orElse(null))
                            .withSecondaryEmail(ofNullable(prosecutionContactDetails.getSecondaryEmail()).orElse(null))
                            .withWork(ofNullable(prosecutionContactDetails.getWorkTelephoneNumber()).orElse(null))
                            .build();

    @Override
    public Individual convert(final Defendant defendant) {

        final uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual individual = defendant.getIndividual();

        if (isNull(individual)) {
            return null;
        }


        return individual()
                .withDriverNumber(ofNullable(individual.getDriverNumber()).orElse(null))
                .withNationalInsuranceNumber(ofNullable(individual.getNationalInsuranceNumber()).orElse(null))
                .withPersonalInformation(buildPersonalInformationForIndividual(defendant))
                .withSelfDefinedInformation(buildSelfDefinedInformation(defendant))
                .withOffenderCode(null)
                .withDriverLicenceIssue(null)
                .withDriverLicenceCode(null)
                .withCustodyStatus(ofNullable(individual.getCustodyStatus()).map(String::toString).orElse(null))
                .withBailConditions(ofNullable(individual.getBailConditions()).map(String::toString).orElse(null))
                .withParentGuardianInformation(buildParentGuardianInformation(ofNullable(individual.getParentGuardian()).orElse(null)))
                .build();

    }

    private PersonalInformation buildPersonalInformationForIndividual(final Defendant defendant) {
        return PersonalInformation.personalInformation()
                .withAddress(buildAddress(ofNullable(defendant.getDefendantDetails()).map(DefendantDetails::getAddress).orElse(null)))
                .withContactDetails(buildIndividualContactDetails(defendant))
                .withFirstName(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getNameDetails).map(NameDetails::getForename).orElse(null))
                .withLastName(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getNameDetails).map(NameDetails::getSurname).orElse(null))
                .withGivenName2(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getNameDetails).map(NameDetails::getForename2).orElse(null))
                .withGivenName3(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getNameDetails).map(NameDetails::getForename3).orElse(null))
                .withObservedEthnicity(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getObservedEthnicity).map(BigDecimal::intValue).orElse(null))
                .withOccupation(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getOccupation).orElse(null))
                .withOccupationCode(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getOccupationCode).orElse(null))
                .withTitle(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getNameDetails).map(NameDetails::getTitle).orElse(null))
                .build();
    }


    private ParentGuardianInformation buildParentGuardianInformation(final ParentGuardian parentGuardian) {
        if (null == parentGuardian) {
            return null;
        }


        if(ofNullable(parentGuardian.getIndividual()).isPresent()) {
            return buildParentGuardianIndividual(parentGuardian);
        }else {
            return buildParentGuardianOrganisation(parentGuardian);
        }

    }

    private ParentGuardianInformation buildParentGuardianOrganisation(final ParentGuardian parentGuardian) {
        return parentGuardianInformation()
                .withOrganisationName(ofNullable(parentGuardian.getOrganisation()).map(ParentGuardianOrganisation::getOrganisationName).orElse(null))
                .withCompanyTelephoneNumber(ofNullable(parentGuardian.getOrganisation()).map(ParentGuardianOrganisation::getCompanyTelephoneNumber).orElse(null))
                .withAddress(buildAddress(ofNullable(parentGuardian.getAddress()).orElse(null)))
                .build();
    }

    private ParentGuardianInformation buildParentGuardianIndividual(final ParentGuardian parentGuardian) {
        final String observedEthnicity = ofNullable(parentGuardian.getIndividual()).map(ParentGuardianIndividual::getObservedEthnicity).map(BigDecimal::toString).orElse(null);
        final String selfDefinedEthnicity = ofNullable(parentGuardian.getIndividual()).map(ParentGuardianIndividual::getSelfDefinedEthnicity).map(String::toString).orElse(null);

        final ParentGuardianInformation.Builder parentGuardianBuilder = parentGuardianInformation()
                .withPersonalInformation(buildPersonalInformation(parentGuardian))
                .withObservedEthnicity(observedEthnicity)
                .withSelfDefinedEthnicity(selfDefinedEthnicity)
                .withDateOfBirth(ofNullable(parentGuardian.getIndividual()).map(ParentGuardianIndividual::getDateOfBirth).orElse(null));

        ofNullable(parentGuardian.getIndividual()).ifPresent(parentGuardianIndividual ->  parentGuardianBuilder.withGender(integerGenderToProsecutionCaseFileGenderConverter.convert(parseInt(parentGuardianIndividual.getGender().toString()))));
        return parentGuardianBuilder.build();

    }


    private PersonalInformation buildPersonalInformation(final ParentGuardian parentGuardian) {

        return PersonalInformation.personalInformation()
                .withAddress(buildAddress(ofNullable(parentGuardian.getAddress()).orElse(null)))
                .withContactDetails(buildParentGuardianContactDetails(parentGuardian))
                .withFirstName(ofNullable(parentGuardian.getIndividual()).map(ParentGuardianIndividual::getNameDetails).map(ParentGuardianNameDetails::getForename).orElse(null))
                .withLastName(ofNullable(parentGuardian.getIndividual()).map(ParentGuardianIndividual::getNameDetails).map(ParentGuardianNameDetails::getSurname).orElse(null))
                .withGivenName2(ofNullable(parentGuardian.getIndividual()).map(ParentGuardianIndividual::getNameDetails).map(ParentGuardianNameDetails::getForename2).orElse(null))
                .withGivenName3(ofNullable(parentGuardian.getIndividual()).map(ParentGuardianIndividual::getNameDetails).map(ParentGuardianNameDetails::getForename3).orElse(null))
                .withObservedEthnicity(ofNullable(parentGuardian.getIndividual()).flatMap(i -> ofNullable(i.getObservedEthnicity())).map(BigDecimal::intValue).orElse(null))
                .withOccupation(null)
                .withOccupationCode(null)
                .withTitle(ofNullable(parentGuardian.getIndividual()).map(ParentGuardianIndividual::getNameDetails).map(ParentGuardianNameDetails::getTitle).orElse(null))
                .build();
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails buildParentGuardianContactDetails(final ParentGuardian parentGuardian) {
        final Optional<ContactDetails> contactDetailsOptional = ofNullable(parentGuardian.getIndividual()).map(ParentGuardianIndividual::getContactDetails);

        return contactDetailsOptional.map(convertContactDetailsFnc).orElse(null);
    }

    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails buildIndividualContactDetails(final Defendant defendant) {
        final Optional<ContactDetails> contactDetailsOptional = ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getContactDetails);

        return contactDetailsOptional.map(convertContactDetailsFnc).orElse(null);
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


    private uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation buildSelfDefinedInformation(final Defendant defendant) {


        return selfDefinedInformation()
                .withEthnicity(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getEthnicity).orElse(null))
                .withGender(getGender(defendant))
                .withDateOfBirth(ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getDateOfBirth).orElse(null))
                .build();
    }

    private Gender getGender(final Defendant defendant) {
        final IntegerGenderToProsecutionCaseFileGenderConverter genderToProsecutionCaseFileGenderConverter = new IntegerGenderToProsecutionCaseFileGenderConverter();

        final uk.gov.moj.cpp.staging.prosecutors.json.schemas.Gender gender = ofNullable(defendant.getIndividual()).map(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual::getGender).orElse(null);

        return gender != null ? genderToProsecutionCaseFileGenderConverter.convert(parseInt(gender.toString())) : null;
    }
}
