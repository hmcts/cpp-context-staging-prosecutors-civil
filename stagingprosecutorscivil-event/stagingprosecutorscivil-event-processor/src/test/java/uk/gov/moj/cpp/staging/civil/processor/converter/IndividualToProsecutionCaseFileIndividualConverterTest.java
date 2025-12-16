package uk.gov.moj.cpp.staging.civil.processor.converter;

import static java.lang.Integer.parseInt;
import static java.util.Optional.ofNullable;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.prosecutorsAddress;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.prosecutorsIndividualWithParentGuardianIndividual;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.prosecutorsIndividualWithParentGuardianOrganisation;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails.defendantDetails;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ContactDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ParentGuardianInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.PersonalInformation;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.SelfDefinedInformation;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ParentGuardian;

import org.junit.jupiter.api.Test;

public class IndividualToProsecutionCaseFileIndividualConverterTest {

    private static Converter<Integer, Gender> integerGenderToProsecutionCaseFileGenderConverter = new IntegerGenderToProsecutionCaseFileGenderConverter();


    @Test
    public void shouldConvertIndividualWithParentGuardianIndividualToProsecutionCaseFileIndividualWithParentGuardianIndividual() {
        final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual> converter = new IndividualToProsecutionCaseFileIndividualConverter();

        final Defendant defendant = Defendant.defendant()
                .withIndividual(prosecutorsIndividualWithParentGuardianIndividual())
                .withOrganisation(null)
                .withDefendantDetails(defendantDetails()
                        .withAddress(prosecutorsAddress())
                        .build())
                .build();

        final Individual prosecutorCaseFileIndividual = converter.convert(defendant);

        assertParentGuardianIndividualInformation(prosecutorCaseFileIndividual.getParentGuardianInformation(), defendant.getIndividual().getParentGuardian());
        assertProsecutionCaseFileIndividualMatchesStagingIndividual(prosecutorCaseFileIndividual, defendant);
    }

    @Test
    public void shouldConvertIndividualWithParentGuardianOrganisationToProsecutionCaseFileIndividualWithParentGuardianOrganisation() {
        final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Individual> converter = new IndividualToProsecutionCaseFileIndividualConverter();

        final Defendant defendant = Defendant.defendant()
                .withIndividual(prosecutorsIndividualWithParentGuardianOrganisation())
                .withOrganisation(null)
                .withDefendantDetails(defendantDetails()
                        .withAddress(prosecutorsAddress())
                        .build())
                .build();

        final Individual prosecutorCaseFileIndividual = converter.convert(defendant);

        assertParentGuardianOrganisationInformation(prosecutorCaseFileIndividual.getParentGuardianInformation(), defendant.getIndividual().getParentGuardian());
        assertProsecutionCaseFileIndividualMatchesStagingIndividual(prosecutorCaseFileIndividual, defendant);
    }

    private static void assertProsecutionCaseFileIndividualMatchesStagingIndividual(final Individual pcfIndividual, final Defendant stagingDefendant) {
        assertThat(pcfIndividual.getDriverNumber(), is(stagingDefendant.getIndividual().getDriverNumber()));
        assertThat(pcfIndividual.getNationalInsuranceNumber(), is(stagingDefendant.getIndividual().getNationalInsuranceNumber()));

        assertThat(pcfIndividual.getOffenderCode(), is(nullValue()));
        assertThat(pcfIndividual.getDriverLicenceIssue(), is(nullValue()));
        assertThat(pcfIndividual.getDriverLicenceCode(), is(nullValue()));

        assertPersonalInformation(pcfIndividual.getPersonalInformation(), stagingDefendant);
        assertSelfDefinedInformation(pcfIndividual.getSelfDefinedInformation(), stagingDefendant.getIndividual());

        assertThat(pcfIndividual.getBailConditions(), is(stagingDefendant.getIndividual().getBailConditions()));
        assertThat(pcfIndividual.getCustodyStatus(), is(stagingDefendant.getIndividual().getCustodyStatus()));
    }

    private static void assertParentGuardianIndividualInformation(final ParentGuardianInformation pcfParentGuardian, final ParentGuardian stagingParentGuardian) {
        if(pcfParentGuardian == null) {
            return;
        }
        assertAddress(pcfParentGuardian.getPersonalInformation().getAddress(), stagingParentGuardian.getAddress());
        assertThat(pcfParentGuardian.getDateOfBirth(), is(stagingParentGuardian.getIndividual().getDateOfBirth()));
        ofNullable(stagingParentGuardian.getIndividual()).ifPresent(stagingParentGuardianIndividual ->
                assertThat(pcfParentGuardian.getGender(), is(integerGenderToProsecutionCaseFileGenderConverter.convert(parseInt(stagingParentGuardianIndividual.getGender().toString())))));
        assertThat(pcfParentGuardian.getObservedEthnicity(), is(String.valueOf(stagingParentGuardian.getIndividual().getObservedEthnicity().intValue())));
        assertThat(pcfParentGuardian.getSelfDefinedEthnicity(), is(stagingParentGuardian.getIndividual().getSelfDefinedEthnicity()));
    }

    private static void assertParentGuardianOrganisationInformation(final ParentGuardianInformation pcfParentGuardian, final ParentGuardian stagingParentGuardian) {
        if(stagingParentGuardian == null) {
            return;
        }
        assertAddress(pcfParentGuardian.getAddress(), stagingParentGuardian.getAddress());
        assertThat(pcfParentGuardian.getOrganisationName(), is(stagingParentGuardian.getOrganisation().getOrganisationName()));
        assertThat(pcfParentGuardian.getCompanyTelephoneNumber(), is(stagingParentGuardian.getOrganisation().getCompanyTelephoneNumber()));
    }

    private static void assertPersonalInformation(final PersonalInformation personalInformation, final Defendant stagingDefendant) {
        assertAddress(personalInformation.getAddress(), stagingDefendant.getDefendantDetails().getAddress());
        assertContactDetails(personalInformation.getContactDetails(), stagingDefendant.getIndividual().getContactDetails());
        assertNameDetails(personalInformation, stagingDefendant.getIndividual().getNameDetails());
        assertThat(personalInformation.getObservedEthnicity().toString(), is(stagingDefendant.getIndividual().getObservedEthnicity().toString()));
        assertThat(personalInformation.getOccupation(), is(stagingDefendant.getIndividual().getOccupation()));
        assertThat(personalInformation.getOccupationCode(), is(stagingDefendant.getIndividual().getOccupationCode()));
    }

    private static void assertSelfDefinedInformation(final SelfDefinedInformation selfDefinedInformation, final uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual stagingIndividual) {
        assertThat(selfDefinedInformation.getAdditionalNationality(), is(isEmptyOrNullString()));
        assertThat(selfDefinedInformation.getDateOfBirth(), is(stagingIndividual.getDateOfBirth()));
        assertThat(selfDefinedInformation.getEthnicity(), is(stagingIndividual.getEthnicity()));
        assertThat(selfDefinedInformation.getGender(), is(integerGenderToProsecutionCaseFileGenderConverter.convert(parseInt(stagingIndividual.getGender().toString()))));
        assertThat(selfDefinedInformation.getNationality(), is(isEmptyOrNullString()));
    }

    private static void assertAddress(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address pcfAddress, final uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address stagingAddress) {
        if(stagingAddress == null) {
            return;
        }
        assertThat(pcfAddress.getAddress1(), is(stagingAddress.getAddress1()));
        assertThat(pcfAddress.getAddress2(), is(stagingAddress.getAddress2()));
        assertThat(pcfAddress.getAddress3(), is(stagingAddress.getAddress3()));
        assertThat(pcfAddress.getAddress4(), is(stagingAddress.getAddress4()));
        assertThat(pcfAddress.getAddress5(), is(stagingAddress.getAddress5()));
        assertThat(pcfAddress.getPostcode(), is(stagingAddress.getPostcode()));
    }

    private static void assertContactDetails(final ContactDetails pcfContactDetails, final uk.gov.moj.cpp.staging.prosecutors.json.schemas.ContactDetails stagingContactDetails) {
        if(stagingContactDetails == null) {
            return;
        }
        assertThat(pcfContactDetails.getPrimaryEmail(), is(stagingContactDetails.getPrimaryEmail()));
        assertThat(pcfContactDetails.getSecondaryEmail(), is(stagingContactDetails.getSecondaryEmail()));
        assertThat(pcfContactDetails.getHome(), is(stagingContactDetails.getHomeTelephoneNumber()));
        assertThat(pcfContactDetails.getMobile(), is(stagingContactDetails.getMobileTelephoneNumber()));
        assertThat(pcfContactDetails.getWork(), is(stagingContactDetails.getWorkTelephoneNumber()));
    }

    private static void assertNameDetails(final PersonalInformation pcfPersonalInformation, final NameDetails stagingNameDetails) {
        if(stagingNameDetails == null) {
            return;
        }
        assertThat(pcfPersonalInformation.getFirstName(), is(stagingNameDetails.getForename()));
        assertThat(pcfPersonalInformation.getGivenName2(), is(stagingNameDetails.getForename2()));
        assertThat(pcfPersonalInformation.getGivenName3(), is(stagingNameDetails.getForename3()));
        assertThat(pcfPersonalInformation.getLastName(), is(stagingNameDetails.getSurname()));
        assertThat(pcfPersonalInformation.getTitle(), is(stagingNameDetails.getTitle()));
    }
}