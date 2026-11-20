package uk.gov.moj.cpp.staging.civil.query;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.json.JsonObject;

import org.junit.jupiter.api.Test;

/**
 * Each scenario is a pair of files under {@code src/test/resources/error-report/}:
 * {@code <scenario>.json} holds the submission's stored {@code caseErrors} / {@code defendantErrors}
 * (either may be absent) and {@code <scenario>.csv} the expected error report.
 */
public class SubmissionErrorDetailsCsvBuilderTest {

    private static final String FIXTURES = "/error-report/";

    @Test
    public void shouldReturnHeaderOnlyWhenBothArraysAreNull() {
        assertThat(buildFrom("no-errors", false), is(expectedCsv("no-errors")));
    }

    @Test
    public void shouldReturnHeaderOnlyWhenBothArraysAreEmpty() {
        assertThat(buildFrom("empty-errors", false), is(expectedCsv("empty-errors")));
    }

    @Test
    public void shouldEmitOneRowWithEmptyFieldAndValueWhenProblemHasNoValues() {
        assertThat(buildFrom("problem-without-values", false), is(expectedCsv("problem-without-values")));
    }

    @Test
    public void shouldPreserveOrderOfCasesBeforeDefendants() {
        assertThat(buildFrom("case-and-defendant-errors", false), is(expectedCsv("case-and-defendant-errors")));
    }

    @Test
    public void shouldReportProsecutionCaseFileKeysAsTemplateColumnsForComplaintsFileUploads() {
        assertThat(buildFrom("prosecution-case-file-keys", true), is(expectedCsv("prosecution-case-file-keys")));
    }

    @Test
    public void shouldReportAReusedKeyAgainstTheColumnItsProblemCodeRefersTo() {
        assertThat(buildFrom("reused-key-by-problem-code", true), is(expectedCsv("reused-key-by-problem-code")));
    }

    @Test
    public void shouldLeaveTemplateColumnKeysFromSchemaErrorsUnchanged() {
        assertThat(buildFrom("schema-error-column-keys", true), is(expectedCsv("schema-error-column-keys")));
    }

    @Test
    public void shouldKeepTheRawKeyWhenAProsecutionCaseFileKeyHasNoTemplateColumn() {
        assertThat(buildFrom("unmapped-key", true), is(expectedCsv("unmapped-key")));
    }

    @Test
    public void shouldKeepRawKeysForSubmissionsThatDidNotComeFromAComplaintsFile() {
        assertThat(buildFrom("prosecution-case-file-keys", false), is(expectedCsv("prosecution-case-file-keys-raw")));

        final JsonObject errors = readJson("prosecution-case-file-keys");
        assertThat(SubmissionErrorDetailsCsvBuilder.build(errors.getJsonArray("caseErrors"), errors.getJsonArray("defendantErrors")),
                is(expectedCsv("prosecution-case-file-keys-raw")));
    }

    @Test
    public void shouldLeaveTheReferenceBlankWhenTheErrorHasNone() {
        assertThat(buildFrom("blank-reference", true), is(expectedCsv("blank-reference")));
    }

    private static String buildFrom(final String scenario, final boolean useCsvColumnNames) {
        final JsonObject errors = readJson(scenario);
        return SubmissionErrorDetailsCsvBuilder.build(
                errors.getJsonArray("caseErrors"), errors.getJsonArray("defendantErrors"), useCsvColumnNames);
    }

    private static JsonObject readJson(final String scenario) {
        try (InputStream in = resource(scenario + ".json")) {
            return createReader(in).readObject();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * The expected report, without the file's trailing newline - the builder does not end the CSV with one.
     */
    private static String expectedCsv(final String scenario) {
        try (InputStream in = resource(scenario + ".csv")) {
            return new String(in.readAllBytes(), UTF_8).replaceFirst("\\R\\z", "");
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static InputStream resource(final String name) {
        final InputStream in = SubmissionErrorDetailsCsvBuilderTest.class.getResourceAsStream(FIXTURES + name);
        if (in == null) {
            throw new IllegalArgumentException("Missing test fixture " + FIXTURES + name);
        }
        return in;
    }
}
