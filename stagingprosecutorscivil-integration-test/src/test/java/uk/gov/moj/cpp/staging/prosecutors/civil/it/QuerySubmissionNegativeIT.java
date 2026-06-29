package uk.gov.moj.cpp.staging.prosecutors.civil.it;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.queryRaw;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.queryRawNoAuth;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.StagingProsecutorsCivilUtils.queryRawWrongMediaType;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupLoggedInUsersPermissionQueryStub;
import static uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils.setupNoPermissionsStub;

import uk.gov.moj.cpp.staging.prosecutors.civil.util.WiremockUtils;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class QuerySubmissionNegativeIT {

    @BeforeEach
    public void setUp() {
        new WiremockUtils();
        setupLoggedInUsersPermissionQueryStub(randomUUID().toString());
    }

    /**
     * AC5: An invalid (non-UUID) submissionId path parameter must be rejected with HTTP 400.
     * The query API validates the format in CivilProsecutionQueryApi.validateSubmissionId()
     * and throws BadRequestException for malformed values.
     */
    @Test
    public void shouldReturn400ForInvalidSubmissionIdFormatAC5() {
        final Response response = queryRaw("not-a-valid-uuid");
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    /**
     * AC6: A well-formed UUID that does not correspond to any submission returns HTTP 404.
     * CivilProsecutionQueryView returns a null payload for unknown IDs; the framework
     * translates that into a 404 response.
     */
    @Test
    public void shouldReturn404ForSubmissionNotFoundAC6() {
        final Response response = queryRaw(randomUUID().toString());
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    /**
     * AC8: A request without authentication credentials (no USER_ID header) must be
     * rejected with HTTP 401 by the framework authentication filter.
     */
    @Test
    public void shouldReturn401WhenMissingAuthHeaderAC8() {
        final Response response = queryRawNoAuth(randomUUID());
        assertThat(response.getStatus(), is(Response.Status.UNAUTHORIZED.getStatusCode()));
    }

    /**
     * AC9: A correctly authenticated user who lacks the CIVIL_CASE.GrantAccess permission
     * must receive HTTP 403. The Drools access-control rule requires that permission;
     * without it the rule does not fire and the framework returns Forbidden.
     */
    @Test
    public void shouldReturn403WhenUserHasNoPermissionsAC9() {
        setupNoPermissionsStub(randomUUID().toString());
        final Response response = queryRaw(randomUUID().toString());
        assertThat(response.getStatus(), is(Response.Status.FORBIDDEN.getStatusCode()));
    }

    /**
     * AC10: A request with an Accept header that does not match the endpoint's declared
     * media type must receive HTTP 406 Not Acceptable from the JAX-RS runtime.
     */
    @Test
    public void shouldReturn406WhenInvalidAcceptHeaderAC10() {
        final Response response = queryRawWrongMediaType(randomUUID());
        assertThat(response.getStatus(), is(Response.Status.NOT_ACCEPTABLE.getStatusCode()));
    }
}
