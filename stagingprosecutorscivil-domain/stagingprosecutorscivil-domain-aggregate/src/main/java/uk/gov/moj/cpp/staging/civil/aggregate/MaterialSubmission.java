package uk.gov.moj.cpp.staging.civil.aggregate;

import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.match;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.otherwiseDoNothing;
import static uk.gov.justice.domain.aggregate.matcher.EventSwitcher.when;
import static uk.gov.moj.cpp.staging.prosecutors.civil.event.SubmissionStatus.PENDING;

import uk.gov.justice.domain.aggregate.Aggregate;
import uk.gov.moj.cpp.staging.prosecutors.civil.event.MaterialSubmitted;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public class MaterialSubmission implements Aggregate {

    private static final long serialVersionUID = 1L;

    private UUID submissionId;

    @Override
    public Object apply(Object event) {
        return match(event).with(
                when(MaterialSubmitted.class).apply(e -> submissionId = e.getSubmissionId()),
                otherwiseDoNothing()
        );
    }

    public Stream<Object> submitMaterial(final UUID submissionId,
                                         final UUID materialId,
                                         final String caseUrn,
                                         final String prosecutingAuthority,
                                         final String materialType,
                                         final Optional<String> defendantId) {

        return apply(Stream.of(MaterialSubmitted.materialSubmitted()
                .withSubmissionId(submissionId)
                .withMaterialId(materialId)
                .withCaseUrn(caseUrn)
                .withProsecutingAuthority(prosecutingAuthority)
                .withMaterialType(materialType)
                .withSubmissionStatus(PENDING)
                .withDefendantId(defendantId.orElse(null))
                .build()));
    }
}
