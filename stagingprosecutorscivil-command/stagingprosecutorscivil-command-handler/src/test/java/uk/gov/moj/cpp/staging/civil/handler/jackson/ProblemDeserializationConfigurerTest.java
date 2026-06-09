package uk.gov.moj.cpp.staging.civil.handler.jackson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import uk.gov.moj.cpp.prosecution.casefile.json.schemas.DefendantProblem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.Problem;
import uk.gov.moj.cpp.prosecution.casefile.json.schemas.ProblemValue;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProblemDeserializationConfigurerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        final ProblemDeserializationConfigurer configurer = new ProblemDeserializationConfigurer();
        configurer.objectMapper = objectMapper;
        configurer.configure();
    }

    @Test
    void shouldDeserialiseProblemWithValues() throws Exception {
        final String json = "{\"code\":\"WRN001\",\"values\":[{\"key\":\"testField\",\"value\":\"testValue\"}]}";

        final Problem problem = objectMapper.readValue(json, Problem.class);

        assertThat(problem, notNullValue());
        assertThat(problem.getCode(), is("WRN001"));
        assertThat(problem.getValues(), hasSize(1));
        assertThat(problem.getValues().get(0).getKey(), is("testField"));
        assertThat(problem.getValues().get(0).getValue(), is("testValue"));
    }

    @Test
    void shouldDeserialiseProblemWithEmptyValues() throws Exception {
        final String json = "{\"code\":\"WRN002\",\"values\":[]}";

        final Problem problem = objectMapper.readValue(json, Problem.class);

        assertThat(problem.getCode(), is("WRN002"));
        assertThat(problem.getValues(), empty());
    }

    @Test
    void shouldDeserialiseListOfProblems() throws Exception {
        final String json = "[{\"code\":\"WRN001\",\"values\":[]},{\"code\":\"WRN002\",\"values\":[]}]";

        final List<Problem> problems = objectMapper.readValue(json,
                objectMapper.getTypeFactory().constructCollectionType(List.class, Problem.class));

        assertThat(problems, hasSize(2));
        assertThat(problems.get(0).getCode(), is("WRN001"));
        assertThat(problems.get(1).getCode(), is("WRN002"));
    }

    @Test
    void shouldDeserialiseProblemValueWithKeyAndValue() throws Exception {
        final String json = "{\"key\":\"field1\",\"value\":\"val1\"}";

        final ProblemValue problemValue = objectMapper.readValue(json, ProblemValue.class);

        assertThat(problemValue, notNullValue());
        assertThat(problemValue.getKey(), is("field1"));
        assertThat(problemValue.getValue(), is("val1"));
        assertThat(problemValue.getId(), nullValue());
    }

    @Test
    void shouldDeserialiseProblemValueWithAllFields() throws Exception {
        final String json = "{\"id\":\"id1\",\"key\":\"field1\",\"value\":\"val1\"}";

        final ProblemValue problemValue = objectMapper.readValue(json, ProblemValue.class);

        assertThat(problemValue.getId(), is("id1"));
        assertThat(problemValue.getKey(), is("field1"));
        assertThat(problemValue.getValue(), is("val1"));
    }

    @Test
    void shouldDeserialiseDefendantProblemWithProblemsAndReference() throws Exception {
        final String json = "{\"problems\":[{\"code\":\"WRN001\",\"values\":[{\"key\":\"k\",\"value\":\"v\"}]}]," +
                "\"prosecutorDefendantReference\":\"REF001\"}";

        final DefendantProblem defendantProblem = objectMapper.readValue(json, DefendantProblem.class);

        assertThat(defendantProblem, notNullValue());
        assertThat(defendantProblem.getProblems(), hasSize(1));
        assertThat(defendantProblem.getProblems().get(0).getCode(), is("WRN001"));
        assertThat(defendantProblem.getProsecutorDefendantReference(), is("REF001"));
    }

    @Test
    void shouldDeserialiseDefendantProblemWithoutReference() throws Exception {
        final String json = "{\"problems\":[{\"code\":\"WRN001\",\"values\":[]}]}";

        final DefendantProblem defendantProblem = objectMapper.readValue(json, DefendantProblem.class);

        assertThat(defendantProblem.getProblems(), hasSize(1));
        assertThat(defendantProblem.getProsecutorDefendantReference(), nullValue());
    }

    @Test
    void shouldFailToDeserialiseProblemWithoutMixinRegistered() {
        final ObjectMapper unmixedMapper = new ObjectMapper();
        final String json = "{\"code\":\"WRN001\",\"values\":[]}";

        assertThrows(InvalidDefinitionException.class,
                () -> unmixedMapper.readValue(json, Problem.class),
                "Problem should not be deserialisable without the mixin");
    }

    @Test
    void shouldFailToDeserialiseDefendantProblemWithoutMixinRegistered() {
        final ObjectMapper unmixedMapper = new ObjectMapper();
        final String json = "{\"problems\":[],\"prosecutorDefendantReference\":\"REF\"}";

        assertThrows(InvalidDefinitionException.class,
                () -> unmixedMapper.readValue(json, DefendantProblem.class),
                "DefendantProblem should not be deserialisable without the mixin");
    }
}
