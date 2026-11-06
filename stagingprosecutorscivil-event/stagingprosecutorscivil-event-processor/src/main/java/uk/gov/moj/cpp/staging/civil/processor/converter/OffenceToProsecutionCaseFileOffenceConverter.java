package uk.gov.moj.cpp.staging.civil.processor.converter;

import static java.util.Optional.ofNullable;
import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toList;
import static uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence.offence;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.Offence;

import java.math.BigDecimal;
import java.util.List;

@SuppressWarnings("squid:S1188")
public class OffenceToProsecutionCaseFileOffenceConverter implements Converter<List<Offence>, List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence>> {
    @Override
    public List<uk.gov.moj.cpp.prosecution.casefile.json.schemas.Offence> convert(final List<Offence> source) {
        return source.stream()
                .map(offence -> offence()
                        .withOffenceId(randomUUID())
                        .withArrestDate(ofNullable(offence.getArrestDate()).orElse(null))
                        .withOffenceCode(offence.getOffenceDetails().getCjsOffenceCode())
                        .withOffenceCommittedDate(ofNullable(offence.getOffenceDetails().getOffenceCommittedDate()).orElse(null))
                        .withLaidDate(ofNullable(offence.getOffenceDetails().getLaidDate()).orElse(null))
                        .withOffenceCommittedEndDate(ofNullable(offence.getOffenceDetails().getOffenceCommittedEndDate()).orElse(null))
                        .withOffenceLocation(ofNullable(offence.getOffenceDetails().getOffenceLocation()).orElse(null))
                        .withOffenceSequenceNumber(offence.getOffenceDetails().getOffenceSequenceNo())
                        .withOffenceWording(offence.getOffenceDetails().getOffenceWording())
                        .withOffenceWordingWelsh(ofNullable(offence.getOffenceDetails().getOffenceWordingWelsh()).orElse(null))
                        .withStatementOfFacts(ofNullable(offence.getStatementOfFacts()).orElse(null))
                        .withStatementOfFactsWelsh(ofNullable(offence.getStatementOfFactsWelsh()).orElse(null))
                        .withAppliedCompensation(ofNullable(offence.getOffenceDetails().getProsecutorCompensation()).map(BigDecimal::new).orElse(null))
                        .build()
                )
                .collect(toList());
    }
}
