package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.getComplaintsFilesTemplate;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

public class ComplaintsFilesTemplateIT {

    @Test
    public void shouldReturnComplaintsFilesCsvTemplate() {
        final Response response = getComplaintsFilesTemplate();

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getHeaderString("Content-Type"), containsString("text/csv"));
        assertThat(response.getHeaderString("Content-Disposition"), containsString("attachment"));
        assertThat(response.getHeaderString("Content-Disposition"), containsString("complaints-files-template.csv"));

        final String csvContent = response.readEntity(String.class);
        assertThat(csvContent, containsString("Column1"));
    }
}
