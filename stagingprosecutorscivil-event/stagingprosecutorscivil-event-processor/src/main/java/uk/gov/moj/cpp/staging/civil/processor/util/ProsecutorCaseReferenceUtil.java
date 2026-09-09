package uk.gov.moj.cpp.staging.civil.processor.util;

import static java.lang.String.format;
import static java.util.Objects.isNull;

import uk.gov.moj.cpp.staging.civil.processor.exception.InvalidCaseUrnProvided;
import uk.gov.moj.cpp.staging.prosecutors.json.schemas.ProsecutionCase;

import java.util.List;
import java.util.stream.Collectors;

public class ProsecutorCaseReferenceUtil {

    public static final String PROSECUTOR_CASE_PATTERN = "%s:%s";

    private ProsecutorCaseReferenceUtil() {
    }

    public static String getProsecutorCaseReference(final String prosecutingAuthority, final String caseUrn) {
        if (isNull(prosecutingAuthority)) {
            return caseUrn;
        }

        if (isNull(caseUrn)) {
            throw new InvalidCaseUrnProvided("please provide a valid caseUrn");
        }

        return format(PROSECUTOR_CASE_PATTERN, prosecutingAuthority, caseUrn);
    }

    public static List<String> getProsecutorCaseReferences(final List<ProsecutionCase> prosecutionCases, final String prosecutingAuthority) {
        return prosecutionCases.stream()
                .map(pc -> ProsecutorCaseReferenceUtil.getProsecutorCaseReference(prosecutingAuthority, pc.getUrn())).collect(Collectors.toList());
    }
}
