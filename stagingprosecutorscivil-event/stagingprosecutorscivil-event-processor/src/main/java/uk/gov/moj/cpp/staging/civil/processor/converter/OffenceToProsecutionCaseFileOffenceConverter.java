package uk.gov.moj.cpp.staging.civil.processor.converter;

import static java.lang.Integer.parseInt;
import static java.util.Objects.nonNull;
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
                        .withBackDuty(ofNullable(offence.getOffenceDetails().getBackDuty()).map(BigDecimal::new).orElse(null))
                        .withBackDutyDateFrom(ofNullable(offence.getOffenceDetails().getBackDutyDateFrom()).orElse(null))
                        .withBackDutyDateTo(ofNullable(offence.getOffenceDetails().getBackDutyDateTo()).orElse(null))
                        .withArrestDate(ofNullable(offence.getArrestDate()).orElse(null))
                        .withOffenceCode(offence.getOffenceDetails().getCjsOffenceCode())
                        .withOffenceCommittedDate(ofNullable(offence.getOffenceDetails().getOffenceCommittedDate()).orElse(null))
                        .withLaidDate(ofNullable(offence.getOffenceDetails().getLaidDate()).orElse(null))
                        .withOffenceCommittedEndDate(ofNullable(offence.getOffenceDetails().getOffenceCommittedEndDate()).orElse(null))
                        .withOffenceDateCode(getOffenceDateCode(offence))
                        .withOffenceLocation(ofNullable(offence.getOffenceDetails().getOffenceLocation()).orElse(null))
                        .withOffenceSequenceNumber(offence.getOffenceDetails().getOffenceSequenceNo())
                        .withOffenceWording(offence.getOffenceDetails().getOffenceWording())
                        .withOffenceWordingWelsh(ofNullable(offence.getOffenceDetails().getOffenceWordingWelsh()).orElse(null))
                        .withStatementOfFacts(ofNullable(offence.getStatementOfFacts()).orElse(null))
                        .withStatementOfFactsWelsh(ofNullable(offence.getStatementOfFactsWelsh()).orElse(null))
                        .withAppliedCompensation(ofNullable(offence.getOffenceDetails().getProsecutorCompensation()).map(BigDecimal::new).orElse(null))
                        .withVehicleMake(ofNullable(offence.getOffenceDetails().getVehicleMake()).orElse(null))
                        .withVehicleRegistrationMark(ofNullable(offence.getOffenceDetails().getVehicleRegistrationMark()).orElse(null))
                        .build()
                )
                .collect(toList());
    }

    private static Integer getOffenceDateCode(final Offence offence) {
        if(nonNull(offence.getOffenceDetails()) && nonNull(offence.getOffenceDetails().getOffenceDateCode())){
            return parseInt(offence.getOffenceDetails().getOffenceDateCode().toString());
        }
        return null;
    }
}
