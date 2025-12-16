package uk.gov.moj.cpp.staging.civil.processor.converter;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker.caseMarker;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseDetails;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.CaseMarker;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Prosecutor;
import uk.gov.moj.cpp.staging.civil.processor.util.ProsecutorCaseReferenceUtil;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.SummonsProsecutionReceived;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Defendant;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;
import uk.gov.moj.cps.prosecutioncasefile.command.api.GroupProsecutions;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProsecutionCaseToGroupProsecutionConverterForSummons implements Converter<ProsecutionCase, GroupProsecutions> {

    private static final String INITIATION_CODE_SUMMONS_CASE = "S";
    private final ZonedDateTime dateReceived;
    private final SummonsProsecutionReceived summonsProsecutionReceived;
    private final UUID groupId;
    private Map<String, UUID> caseRefToCaseId;
    public ProsecutionCaseToGroupProsecutionConverterForSummons(final ZonedDateTime dateReceived, final SummonsProsecutionReceived summonsProsecutionReceived, final UUID groupId, final Map<String, UUID> caseRefToCaseId) {
        this.dateReceived = dateReceived;
        this.summonsProsecutionReceived = summonsProsecutionReceived;
        this.groupId = groupId;
        this.caseRefToCaseId = caseRefToCaseId;
    }

    @Override
    public GroupProsecutions convert(final ProsecutionCase prosecutionCase) {

        final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantToProsecutionCaseFileDefendantConverter
                = new DefendantToProsecutionCaseFileDefendantConverter(summonsProsecutionReceived.getHearingDetails());

        return GroupProsecutions.groupProsecutions()
                .withCaseDetails(buildCaseDetails(prosecutionCase))
                .withDefendants(buildDefendants(prosecutionCase, defendantToProsecutionCaseFileDefendantConverter))
                .withGroupId(groupId)
                .withIsCivil(true)
                .withIsGroupMember(true)
                .withIsGroupMaster(false)
                .withPaymentReference(prosecutionCase.getPaymentReference())
                .build();
    }

    private List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> buildDefendants(final ProsecutionCase prosecutionCase,
                                                                                             final Converter<Defendant, uk.gov.moj.cpp.prosecution.casefile.json.schemas.Defendant> defendantToProsecutionCaseFileDefendantConverter) {
        return prosecutionCase.getDefendants()
                .stream()
                .map(defendantToProsecutionCaseFileDefendantConverter::convert)
                .collect(toList());
    }

    private CaseDetails buildCaseDetails(final ProsecutionCase prosecutionCase) {
        final UUID caseFileId = caseRefToCaseId.get(ProsecutorCaseReferenceUtil.getProsecutorCaseReference(summonsProsecutionReceived.getProsecutingAuthority(), prosecutionCase.getUrn()));

        return CaseDetails.caseDetails()
                .withDateReceived(dateReceived.toLocalDate())
                .withProsecutorCaseReference(prosecutionCase.getUrn())
                .withOtherPartyOfficerInCase(null)
                .withCaseId(caseFileId)
                .withSummonsCode(prosecutionCase.getSummonsCode())
                .withCpsOrganisation(null)
                .withInitiationCode(INITIATION_CODE_SUMMONS_CASE)
                .withOriginatingOrganisation(summonsProsecutionReceived.getProsecutingAuthority())
                .withCaseMarkers(buildCaseMarkers(ofNullable(prosecutionCase.getCaseMarker()).orElse(null)))
                .withProsecutor(Prosecutor.prosecutor()
                        .withInformant(ofNullable(prosecutionCase.getInformant()).orElse(null))
                        .withProsecutingAuthority(summonsProsecutionReceived.getProsecutingAuthority()).build())
                .build();
    }

    private List<CaseMarker> buildCaseMarkers(final String caseMarkers) {
        if (caseMarkers == null) {
            return emptyList();
        }
        final String[] caseMarkersArray = caseMarkers.split("\\s+");
        return stream(caseMarkersArray)
                .map(caseMarker -> caseMarker().withMarkerTypeCode(caseMarker).build())
                .distinct()
                .collect(toList());
    }
}