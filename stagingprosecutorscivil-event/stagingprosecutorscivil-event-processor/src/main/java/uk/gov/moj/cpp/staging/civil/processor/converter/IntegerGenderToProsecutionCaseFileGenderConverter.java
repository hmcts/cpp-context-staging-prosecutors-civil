package uk.gov.moj.cpp.staging.civil.processor.converter;

import uk.gov.justice.services.common.converter.Converter;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;

public class IntegerGenderToProsecutionCaseFileGenderConverter implements Converter<Integer, Gender> {
    @Override
    public Gender convert(final Integer source) {
        switch (source) {
            case 0:
                return Gender.NOT_KNOWN;
            case 1:
                return Gender.MALE;
            case 2:
                return Gender.FEMALE;
            case 9:
                return Gender.NOT_SPECIFIED;
            default:
                throw new IllegalArgumentException("Civil doesn't allow other gender values");
        }
    }
}
