package uk.gov.moj.cpp.staging.civil.query.api.accesscontrol;

import static uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission.builder;

import uk.gov.justice.services.common.converter.jackson.ObjectMapperProducer;
import uk.gov.moj.cpp.accesscontrol.drools.ExpectedPermission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class RuleConstants {

    private static final ObjectMapper objectMapper = new ObjectMapperProducer().objectMapper();

    public static final String NON_CPS_PROSECUTOR_GROUP = "Non CPS Prosecutors";
    public static final String SYSTEM_USERS = "System Users";

    private RuleConstants() {
        throw new IllegalAccessError("Utility class");
    }

    public static String getCivilCasePermission() throws JsonProcessingException {
        final ExpectedPermission expectedPermission = builder()
                .withObject("CIVIL_CASE")
                .withAction("GrantAccess")
                .build();
        return objectMapper.writeValueAsString(expectedPermission);
    }

    public static String[] getComplaintsFilesTemplateGroups() {
        return new String[]{NON_CPS_PROSECUTOR_GROUP, SYSTEM_USERS};
    }
}
