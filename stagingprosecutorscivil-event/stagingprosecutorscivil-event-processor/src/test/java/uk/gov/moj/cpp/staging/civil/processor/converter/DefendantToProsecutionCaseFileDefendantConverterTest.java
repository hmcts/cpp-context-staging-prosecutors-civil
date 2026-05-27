package uk.gov.moj.cpp.staging.civil.processor.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.groupChargeProsecutionReceived;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.prosecutorsDefendant;

import uk.gov.justice.cps.prosecutioncasefile.InitialHearing;
import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Address;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.HearingDetails;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    public void shouldUseHearingDateFromWhenDateOfHearingIsAbsent() {

        final LocalDate hearingDateFrom = LocalDate.of(2025, 6, 1);
        final HearingDetails hearingDetails = HearingDetails.hearingDetails()
                .withCourtHearingLocation("COURT1")
                .withHearingDateFrom(hearingDateFrom)
                .build();

        final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> converter
                = new DefendantToProsecutionCaseFileDefendantConverter(hearingDetails);

        final uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant result = converter.convert(prosecutorsDefendant());

        assertThat(result.getInitialHearing().getDateOfHearing(), is(hearingDateFrom.toString()));
        assertThat(result.getInitialHearing().getCourtHearingLocation(), is("COURT1"));
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
        final String expectedDate = stagingHearing.getDateOfHearing() != null
                ? stagingHearing.getDateOfHearing().toString()
                : stagingHearing.getHearingDateFrom().toString();
        assertThat(pcfHearing.getDateOfHearing(), is(expectedDate));
        assertThat(pcfHearing.getCourtHearingLocation(), is(stagingHearing.getCourtHearingLocation()));
    }
}