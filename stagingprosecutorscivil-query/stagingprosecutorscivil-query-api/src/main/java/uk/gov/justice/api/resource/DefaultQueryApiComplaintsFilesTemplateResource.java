package uk.gov.justice.api.resource;

import uk.gov.justice.services.core.annotation.Adapter;
import uk.gov.justice.services.core.annotation.Component;
import uk.gov.moj.cpp.staging.civil.query.api.ComplaintsFilesTemplateQueryApi;

import javax.inject.Inject;
import javax.ws.rs.core.Response;

@Adapter(Component.QUERY_API)
public class DefaultQueryApiComplaintsFilesTemplateResource implements QueryApiComplaintsFilesTemplateResource {

    @Inject
    private ComplaintsFilesTemplateQueryApi complaintsFilesTemplateQueryApi;

    @Override
    public Response getComplaintsFilesTemplate() {
        return complaintsFilesTemplateQueryApi.getComplaintsFilesTemplate();
    }
}
