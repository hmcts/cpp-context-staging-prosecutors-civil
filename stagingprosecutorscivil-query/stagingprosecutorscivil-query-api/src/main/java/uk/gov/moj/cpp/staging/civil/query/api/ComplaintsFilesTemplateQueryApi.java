package uk.gov.moj.cpp.staging.civil.query.api;

import static javax.ws.rs.core.HttpHeaders.CONTENT_DISPOSITION;
import static javax.ws.rs.core.HttpHeaders.CONTENT_TYPE;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.OK;
import static javax.ws.rs.core.Response.status;

import java.io.InputStream;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComplaintsFilesTemplateQueryApi {

    private static final Logger LOGGER = LoggerFactory.getLogger(ComplaintsFilesTemplateQueryApi.class);

    private static final String CSV_FILE_NAME = "complaints-files-template.csv";
    static final String CSV_RESOURCE_PATH = "/" + CSV_FILE_NAME;
    private static final String CSV_MIME_TYPE = "text/csv";

    public Response getComplaintsFilesTemplate() {
        return buildCsvResponse(CSV_RESOURCE_PATH);
    }

    Response buildCsvResponse(final String resourcePath) {
        final InputStream csvStream = getClass().getResourceAsStream(resourcePath);

        if (csvStream == null) {
            LOGGER.error("Complaints files CSV template not found on classpath at '{}'", resourcePath);
            return status(NOT_FOUND)
                    .entity("Complaints files CSV template not found")
                    .build();
        }

        return status(OK)
                .entity(csvStream)
                .header(CONTENT_TYPE, CSV_MIME_TYPE)
                .header(CONTENT_DISPOSITION, "attachment; filename=\"" + CSV_FILE_NAME + "\"")
                .build();
    }
}
