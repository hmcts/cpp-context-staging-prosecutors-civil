package uk.gov.moj.cpp.staging.civil.processor.converter;

import static java.lang.Integer.parseInt;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.prosecutorsOffenceList;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

public class OffenceToProsecutionCaseFileOffenceConverterTest {

    private final OffenceToProsecutionCaseFileOffenceConverter converter = new OffenceToProsecutionCaseFileOffenceConverter();

    private static void assertProsecutionCaseFileOffenceListMatchesProsecutionOffenceList(final List<Offence> prosecutionCaseFileOffencesList,
                                                                                          final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence> prosecutorsOffenceList) {

        assertThat(prosecutionCaseFileOffencesList, is(notNullValue()));

        assertThat(prosecutionCaseFileOffencesList.size(), is(prosecutorsOffenceList.size()));


        prosecutionCaseFileOffencesList.forEach(
                offences -> {
                    final uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence offence = prosecutorsOffenceList.get(offences.getOffenceSequenceNumber() - 1);
                    assertThat(offences.getBackDuty(), is(BigDecimal.valueOf(Integer.parseInt(offence.getOffenceDetails().getBackDuty()))));
                    assertThat(offences.getBackDutyDateFrom(), is(offence.getOffenceDetails().getBackDutyDateFrom()));
                    assertThat(offences.getBackDutyDateTo(), is(offence.getOffenceDetails().getBackDutyDateTo()));
                    assertThat(offences.getOffenceCode(), is(offence.getOffenceDetails().getCjsOffenceCode()));
                    assertThat(offences.getOffenceCommittedDate(), is(offence.getOffenceDetails().getOffenceCommittedDate()));
                    assertThat(offences.getOffenceCommittedEndDate(), is(offence.getOffenceDetails().getOffenceCommittedEndDate()));
                    assertThat(offences.getOffenceDateCode(), is(parseInt(offence.getOffenceDetails().getOffenceDateCode().toString())));
                    assertThat(offences.getOffenceLocation(), is(offence.getOffenceDetails().getOffenceLocation()));
                    assertThat(offences.getOffenceSequenceNumber(), is(offence.getOffenceDetails().getOffenceSequenceNo()));
                    assertThat(offences.getOffenceWording(), is(offence.getOffenceDetails().getOffenceWording()));
                    assertThat(offences.getOffenceWordingWelsh(), is(offence.getOffenceDetails().getOffenceWordingWelsh()));
                    assertThat(offences.getAppliedCompensation(), is(BigDecimal.valueOf(Integer.parseInt(offence.getOffenceDetails().getProsecutorCompensation()))));
                    assertThat(offences.getStatementOfFacts(), is(offence.getStatementOfFacts()));
                    assertThat(offences.getStatementOfFactsWelsh(), is(offences.getStatementOfFactsWelsh()));
                }
        );
    }

    @Test
    public void shouldConvertProsecutionOffenceToProsecutionCaseFileOffence() {

        final List<uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence> prosecutorsOffenceList = prosecutorsOffenceList(3);
        final List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence> prosecutionCaseFileOffencesList = converter.convert(prosecutorsOffenceList);

        assertProsecutionCaseFileOffenceListMatchesProsecutionOffenceList(prosecutionCaseFileOffencesList, prosecutorsOffenceList);

    }
}