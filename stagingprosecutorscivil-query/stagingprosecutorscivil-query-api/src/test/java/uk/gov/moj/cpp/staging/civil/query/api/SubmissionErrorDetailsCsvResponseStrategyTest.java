package uk.gov.moj.cpp.staging.civil.query.api;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static uk.gov.justice.services.messaging.JsonObjects.createObjectBuilder;

import uk.gov.justice.services.messaging.JsonEnvelope;
import uk.gov.justice.services.test.utils.core.enveloper.EnvelopeFactory;

import java.util.Optional;
import java.util.UUID;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class SubmissionErrorDetailsCsvResponseStrategyTest {

    private final SubmissionErrorDetailsCsvResponseStrategy responseStrategy = new SubmissionErrorDetailsCsvResponseStrategy();

    @Test
    public void shouldReturnCsvEntityWithTextCsvContentType() {

        final String submissionId = UUID.randomUUID().toString();
        final String csv = "Reference,Error Type,Error Code,Field,Value\n123,Case,SOME_CODE,field,value";

        final JsonEnvelope envelope = EnvelopeFactory.createEnvelope("stagingprosecutorscivil.query.submission-error-details-csv",
                createObjectBuilder()
                        .add("submissionId", submissionId)
                        .add("csv", csv)
                        .build());

        final Response response = responseStrategy.responseFor("stagingprosecutorscivil.submission-error-details-csv", Optional.of(envelope));

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is("text/csv"));
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION), containsString("attachment"));
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION), containsString(submissionId));
        assertThat(response.getEntity(), is(csv));
    }

    @Test
    public void shouldReturnOkWithNoEntityWhenEnvelopeAbsent() {
        final Response response = responseStrategy.responseFor("stagingprosecutorscivil.submission-error-details-csv", Optional.empty());

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }
}
