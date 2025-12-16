package uk.gov.moj.cpp.staging.civil.processor.converter;

import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Gender;

import java.util.Map;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

public class IntegerGenderToProsecutionCaseFileGenderConverterTest {
    private IntegerGenderToProsecutionCaseFileGenderConverter converterUnderTest = new IntegerGenderToProsecutionCaseFileGenderConverter();

    private static final Map<Integer, Gender> MAPPED_VALUES = ImmutableMap.of(
            0, Gender.NOT_KNOWN,
            9, Gender.NOT_SPECIFIED,
            1, Gender.MALE,
            2, Gender.FEMALE
    );

    @Test
    public void convertGender() {
        IntStream.range(-10, 10)
                .forEach(inputGender -> {
                    try {
                        Gender actualConversion = converterUnderTest.convert(inputGender);
                        assertThat(
                                msg(inputGender, actualConversion),
                                actualConversion,
                                equalTo(MAPPED_VALUES.get(inputGender)));
                    } catch (IllegalArgumentException iae) {
                        assertThat(
                                msg(inputGender, null),
                                MAPPED_VALUES,
                                not(hasKey(inputGender))
                        );
                    }
                });

    }

    private static String msg(Integer inputGender, Gender expectedGender) {
        return format("Expected '%s' to be converted to '%s' but was '%s.'", inputGender, MAPPED_VALUES.get(inputGender), expectedGender);
    }
}


