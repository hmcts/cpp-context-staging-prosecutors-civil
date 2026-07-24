package uk.gov.moj.cpp.staging.civil.query.api;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class ComplaintsFilesTemplateQueryApiTest {

    private final ComplaintsFilesTemplateQueryApi complaintsFilesTemplateQueryApi = new ComplaintsFilesTemplateQueryApi();

    @Test
    public void shouldReturnCsvTemplateWhenResourceFileExists() throws IOException {
        final Response response = complaintsFilesTemplateQueryApi.getComplaintsFilesTemplate();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_TYPE), is("text/csv"));
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION), containsString("attachment"));
        assertThat(response.getHeaderString(HttpHeaders.CONTENT_DISPOSITION), containsString("complaints-files-template.csv"));

        final Object entity = response.getEntity();
        assertThat(entity, notNullValue());
        final String csvContent = new String(((InputStream) entity).readAllBytes(), StandardCharsets.UTF_8);
        assertThat(csvContent, containsString("Column1"));
    }

    @Test
    public void shouldReturnNotFoundWhenResourceFileIsMissing() {
        final Response response = complaintsFilesTemplateQueryApi.buildCsvResponse("/does-not-exist-template.csv");

        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        assertThat(response.getEntity().toString(), containsString("not found"));
    }
}
