package uk.gov.moj.cpp.staging.civil.processor.converter;

import static java.time.ZonedDateTime.now;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker.caseMarker;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.groupChargeProsecutionReceived;
import static uk.gov.moj.cpp.staging.civil.processor.utils.Prosecutors.summonsProsecutionCaseDetail;

import uk.gov.justice.services.test.utils.common.helper.StoppedClock;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.staging.civil.processor.SystemIdMapperService;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.ChargeProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;
import uk.gov.moj.cps.prosecutioncasefile.command.api.GroupProsecutions;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ProsecutionCaseToGroupProsecutionConverterForChargeTest {

    private final StoppedClock clock = new StoppedClock(now());

    @Mock
    SystemIdMapperService systemIdMapperService;

    @Test
    public void shouldConvertProsecutionToProsecutionCaseFile() {
        final UUID caseFileId = UUID.randomUUID();
        final UUID groupId = UUID.randomUUID();
        final ZonedDateTime dateReceived = clock.now();
        final Map<String, UUID>  caseRefToCaseId = new HashMap<>();
        final ChargeProsecutionReceived chargeProsecutionReceived = groupChargeProsecutionReceived();
        caseRefToCaseId.put(chargeProsecutionReceived.getProsecutionCases().get(0).getUrn(), caseFileId);
        final ProsecutionCaseToGroupProsecutionConverterForCharge converter = new ProsecutionCaseToGroupProsecutionConverterForCharge(dateReceived, chargeProsecutionReceived, groupId, caseRefToCaseId);
        final ProsecutionCase prosecutionCase = summonsProsecutionCaseDetail();
        final GroupProsecutions prosecutorsCaseFileGroupProsecutions = converter.convert(prosecutionCase);

        assertThat(prosecutorsCaseFileGroupProsecutions.getGroupId(), is(notNullValue()));
        assertThat(prosecutorsCaseFileGroupProsecutions.getIsCivil(), is(true));
        assertThat(prosecutorsCaseFileGroupProsecutions.getIsGroupMember(), is(true));
        assertThat(prosecutorsCaseFileGroupProsecutions.getIsGroupMaster(), is(notNullValue()));
        assertThat(prosecutorsCaseFileGroupProsecutions.getPaymentReference(), is(prosecutionCase.getPaymentReference()));
        assertCaseDetails(prosecutorsCaseFileGroupProsecutions.getCaseDetails(), prosecutionCase, chargeProsecutionReceived, caseFileId);
    }

    private void assertCaseDetails(final CaseDetails pcfCaseDetails,
                                   final ProsecutionCase prosecutionCase,
                                   final ChargeProsecutionReceived chargeProsecutionReceived,
                                   final UUID caseId) {

        assertThat(pcfCaseDetails, notNullValue());
        assertThat(pcfCaseDetails.getCaseId(), is(caseId));
        assertThat(pcfCaseDetails.getDateReceived(), is(clock.now().toLocalDate()));
        assertThat(pcfCaseDetails.getInitiationCode(), is("O"));
        assertThat(pcfCaseDetails.getOriginatingOrganisation(), is(chargeProsecutionReceived.getProsecutingAuthority()));
        assertThat(pcfCaseDetails.getProsecutor().getInformant(), is(prosecutionCase.getInformant()));
        assertThat(pcfCaseDetails.getProsecutor().getProsecutingAuthority(), is(chargeProsecutionReceived.getProsecutingAuthority()));
        assertThat(pcfCaseDetails.getCaseMarkers(), is(singletonList(caseMarker()
                .withMarkerTypeCode(prosecutionCase.getCaseMarker())
                .build())));
        assertThat(pcfCaseDetails.getProsecutorCaseReference(), is(prosecutionCase.getUrn()));
        assertThat(pcfCaseDetails.getSummonsCode(), is(prosecutionCase.getSummonsCode()));

        assertThat(pcfCaseDetails.getOtherPartyOfficerInCase(), is(nullValue()));
        assertThat(pcfCaseDetails.getCpsOrganisation(), is(nullValue()));
    }
}