package uk.gov.justice.api.resource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import uk.gov.moj.cpp.staging.civil.query.api.ComplaintsFilesTemplateQueryApi;

import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DefaultQueryApiComplaintsFilesTemplateResourceTest {

    @Mock
    private ComplaintsFilesTemplateQueryApi complaintsFilesTemplateQueryApi;

    @Mock
    private Response expectedResponse;

    @InjectMocks
    private DefaultQueryApiComplaintsFilesTemplateResource resource;

    @Test
    public void shouldDelegateToComplaintsFilesTemplateQueryApi() {
        when(complaintsFilesTemplateQueryApi.getComplaintsFilesTemplate()).thenReturn(expectedResponse);

        final Response response = resource.getComplaintsFilesTemplate();

        verify(complaintsFilesTemplateQueryApi).getComplaintsFilesTemplate();
        assertThat(response, is(expectedResponse));
    }
}
