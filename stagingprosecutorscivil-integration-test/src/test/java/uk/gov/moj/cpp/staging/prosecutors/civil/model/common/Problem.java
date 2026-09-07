package uk.gov.moj.cpp.staging.prosecutors.civil.model.common;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import lombok.Builder;

@Builder
public class Problem {

    @Builder.Default
    public String code = "DEFENDANT_DOB_IN_FUTURE";

    @Builder.Default
    public List<ProblemValue> values = ImmutableList.of(ProblemValue.builder().build());

    @JsonCreator
    public Problem(@JsonProperty("code") String code, @JsonProperty("values") List<ProblemValue> values) {
        this.code = code;
        this.values = values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Problem problem = (Problem) o;
        return Objects.equals(code, problem.code) &&
                Objects.equals(values, problem.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code, values);
    }

    @Override
    public String toString() {
        return "Problem{" +
                "code='" + code + '\'' +
                ", values=" + values +
                '}';
    }

    @Builder
    public static class ProblemValue {

        @Builder.Default
        public String key = "dob";

        @Builder.Default
        public String value = "2050-01-01";

        @JsonCreator
        public ProblemValue(@JsonProperty("key") String key, @JsonProperty("value") String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProblemValue that = (ProblemValue) o;
            return Objects.equals(key, that.key) &&
                    Objects.equals(value, that.value);
        }

        @Override
        public int hashCode() {

            return Objects.hash(key, value);
        }

        @Override
        public String toString() {
            return "ProblemValue{" +
                    "key='" + key + '\'' +
                    ", value='" + value + '\'' +
                    '}';
        }
    }

}
