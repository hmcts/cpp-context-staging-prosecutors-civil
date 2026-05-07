package uk.gov.moj.cpp.staging.civil.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.COURT_HEARING_LOCATION;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.HEARING_TYPE;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.TIME_OF_HEARING;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.groupChargeProsecutionReceived;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.prosecutorsDefendant;
import static uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails.hearingDetails;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class DefendantToProsecutionCaseFileDefendantConverterTest {

    @Test
    public void shouldConvertProsecutionDefendantToProsecutionCaseFileDefendant() {

        final ChargeProsecutionReceived chargeProsecutionReceived = groupChargeProsecutionReceived();
        final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> converter
                = new DefendantToProsecutionCaseFileDefendantConverter(chargeProsecutionReceived.getHearingDetails());

        final Defendant prosecutorsDefendant = prosecutorsDefendant();


        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant prosecutionCaseFileDefendant = converter.convert(prosecutorsDefendant);

        assertProsecutionCaseFileDefendantMatchesProsecutionDefendant(prosecutionCaseFileDefendant, prosecutorsDefendant, chargeProsecutionReceived);
    }

    @Test
    public void shouldHandleDefendantWithNullAddressAndNullPostcode() {
        // Covers the early-return branches in buildAddress(null) and formatPostcode(null).
        final ChargeProsecutionReceived chargeProsecutionReceived = groupChargeProsecutionReceived();
        final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> converter
                = new DefendantToProsecutionCaseFileDefendantConverter(chargeProsecutionReceived.getHearingDetails());

        // (a) null address — exercises buildAddress null-guard
        final Defendant defendantWithNullAddress = uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant.defendant()
                .withDefendantDetails(uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails.defendantDetails()
                        .withDocumentationLanguage(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language.E)
                        .withHearingLanguage(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language.E)
                        .build())
                .withIndividual(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual.individual()
                        .withGender(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Gender.NUMBER_1)
                        .withNameDetails(uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails.nameDetails()
                                .withForename("Jane")
                                .withSurname("DOE")
                                .build())
                        .build())
                .withOffences(java.util.Collections.singletonList(
                        uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.prosecutorsOffence(1)))
                .build();

        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant pcfDefendant1 = converter.convert(defendantWithNullAddress);
        assertThat(pcfDefendant1.getAddress(), is(nullValue()));

        // (b) non-null address but null postcode — exercises formatPostcode null-guard
        final Defendant defendantWithNullPostcode = uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant.defendant()
                .withDefendantDetails(uk.gov.moj.cpp.staging.prosecutors.json.schemas.DefendantDetails.defendantDetails()
                        .withDocumentationLanguage(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language.E)
                        .withHearingLanguage(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Language.E)
                        .withAddress(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address.address()
                                .withAddress1("address1")
                                .build())
                        .build())
                .withIndividual(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Individual.individual()
                        .withGender(uk.gov.moj.cpp.staging.prosecutors.json.schemas.Gender.NUMBER_1)
                        .withNameDetails(uk.gov.moj.cpp.staging.prosecutors.json.schemas.NameDetails.nameDetails()
                                .withForename("Jane")
                                .withSurname("DOE")
                                .build())
                        .build())
                .withOffences(java.util.Collections.singletonList(
                        uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.prosecutorsOffence(1)))
                .build();

        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant pcfDefendant2 = converter.convert(defendantWithNullPostcode);
        assertThat(pcfDefendant2.getAddress(), is(notNullValue()));
        assertThat(pcfDefendant2.getAddress().getPostcode(), is(nullValue()));
    }

    @Test
    public void shouldHandleNullDateOfHearingForEnforcementStyleSubmissions() {
        // dateOfHearing is no longer schema-required (NA for enforcement). The converter must
        // not NPE when it is null — instead it should pass null through to the downstream
        // InitialHearing.dateOfHearing field.
        final HearingDetails hearingDetails = hearingDetails()
                .withCourtHearingLocation(COURT_HEARING_LOCATION)
                .withTimeOfHearing(TIME_OF_HEARING)
                .withHearingType(HEARING_TYPE)
                .build();

        final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> converter
                = new DefendantToProsecutionCaseFileDefendantConverter(hearingDetails);

        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant pcfDefendant = converter.convert(prosecutorsDefendant());

        assertThat(pcfDefendant.getInitialHearing(), is(notNullValue()));
        assertThat(pcfDefendant.getInitialHearing().getDateOfHearing(), is(nullValue()));
        assertThat(pcfDefendant.getInitialHearing().getCourtHearingLocation(), is(COURT_HEARING_LOCATION));
        assertThat(pcfDefendant.getInitialHearing().getTimeOfHearing(), is(TIME_OF_HEARING));
        assertThat(pcfDefendant.getInitialHearing().getHearingTypeCode(), is(HEARING_TYPE));
    }


    private static void assertProsecutionCaseFileDefendantMatchesProsecutionDefendant(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant prosecutionCaseFileDefendant,
                                                                                      final Defendant prosecutorsDefendant, final ChargeProsecutionReceived chargeProsecutionReceived) {

        assertThat(prosecutionCaseFileDefendant, is(notNullValue()));

        assertThat(prosecutionCaseFileDefendant.getAliasForCorporate(), is(prosecutorsDefendant.getOrganisation().getAliasOrganisationNames()));
        assertThat(prosecutionCaseFileDefendant.getAppliedProsecutorCosts(), is( new BigDecimal(prosecutorsDefendant.getDefendantDetails().getProsecutorCosts())));
        assertThat(prosecutionCaseFileDefendant.getAsn(), is(prosecutorsDefendant.getDefendantDetails().getAsn()));
        assertThat(prosecutionCaseFileDefendant.getCroNumber(), is(prosecutorsDefendant.getDefendantDetails().getCroNumber()));
        assertThat(prosecutionCaseFileDefendant.getDocumentationLanguage().name(), is(prosecutorsDefendant.getDefendantDetails().getDocumentationLanguage().toString()));
        assertThat(prosecutionCaseFileDefendant.getHearingLanguage().name(), is(prosecutorsDefendant.getDefendantDetails().getHearingLanguage().toString()));
        assertThat(prosecutionCaseFileDefendant.getOrganisationName(), is(prosecutorsDefendant.getOrganisation().getOrganisationName()));
        assertThat(prosecutionCaseFileDefendant.getTelephoneNumberBusiness(), is(prosecutorsDefendant.getOrganisation().getCompanyTelephoneNumber()));
        assertThat(prosecutionCaseFileDefendant.getPncIdentifier(), is(prosecutorsDefendant.getDefendantDetails().getPncIdentifier()));
        assertThat(prosecutionCaseFileDefendant.getNumPreviousConvictions(), is(prosecutorsDefendant.getDefendantDetails().getNumPreviousConvictions()));

        assertAddress(prosecutionCaseFileDefendant.getAddress(), prosecutorsDefendant.getDefendantDetails().getAddress());
        assertHearingDetails(prosecutionCaseFileDefendant.getInitialHearing(), chargeProsecutionReceived.getHearingDetails());

        assertThat(prosecutionCaseFileDefendant.getCustodyStatus(), is(prosecutorsDefendant.getIndividual().getCustodyStatus()));
        assertThat(prosecutionCaseFileDefendant.getIndividual().getCustodyStatus(), is(prosecutorsDefendant.getIndividual().getCustodyStatus()));
        assertThat(prosecutionCaseFileDefendant.getLanguageRequirement(), is(prosecutorsDefendant.getIndividual().getLanguageRequirement()));
        assertThat(prosecutionCaseFileDefendant.getSpecificRequirements(), is(prosecutorsDefendant.getIndividual().getSpecificRequirements()));

    }

    private static void assertAddress(final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Address pcfAddress, final Address stagingAddress) {
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

    private static void assertHearingDetails(final InitialHearing pcfHearing, final HearingDetails stagingHearing) {
        assertThat(pcfHearing.getTimeOfHearing(), is(stagingHearing.getTimeOfHearing()));
        assertThat(pcfHearing.getDateOfHearing(), is(stagingHearing.getDateOfHearing().toString()));
        assertThat(pcfHearing.getCourtHearingLocation(), is(stagingHearing.getCourtHearingLocation()));
        assertThat(pcfHearing.getHearingTypeCode(), is(stagingHearing.getHearingType()));
    }
}