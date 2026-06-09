package uk.gov.moj.cpp.staging.civil.handler.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * Registers Jackson mixin annotations at application startup so that
 * Problem, ProblemValue and DefendantProblem — which are external library
 * classes with no @JsonCreator — can be deserialised from JSON by Jackson.
 *
 * Without these mixins, the UpdateCivilCase command fails to deserialise
 * when warnings are present, leaving submission status stuck at PENDING.
 */
@Singleton
@Startup
public class ProblemDeserializationConfigurer {

    @Inject
    ObjectMapper objectMapper;

    @PostConstruct
    public void configure() {
        objectMapper.addMixIn(Problem.class,       ProblemMixin.class);
        objectMapper.addMixIn(ProblemValue.class,  ProblemValueMixin.class);
        objectMapper.addMixIn(DefendantProblem.class, DefendantProblemMixin.class);
    }

    abstract static class ProblemMixin {
        @JsonCreator
        @SuppressWarnings("java:S1186") // intentionally empty — Jackson reads annotations only, body is never executed
        ProblemMixin(
                @JsonProperty("code")   String code,
                @JsonProperty("values") List<ProblemValue> values) {}
    }

    abstract static class ProblemValueMixin {
        @JsonCreator
        @SuppressWarnings("java:S1186") // intentionally empty — Jackson reads annotations only, body is never executed
        ProblemValueMixin(
                @JsonProperty("id")                   String id,
                @JsonProperty("key")                  String key,
                @JsonProperty("value")                String value,
                @JsonProperty("additionalProperties") Map<String, Object> additionalProperties) {}
    }

    abstract static class DefendantProblemMixin {
        @JsonCreator
        @SuppressWarnings("java:S1186") // intentionally empty — Jackson reads annotations only, body is never executed
        DefendantProblemMixin(
                @JsonProperty("problems")                    List<Problem> problems,
                @JsonProperty("prosecutorDefendantReference") String prosecutorDefendantReference) {}
    }
}
