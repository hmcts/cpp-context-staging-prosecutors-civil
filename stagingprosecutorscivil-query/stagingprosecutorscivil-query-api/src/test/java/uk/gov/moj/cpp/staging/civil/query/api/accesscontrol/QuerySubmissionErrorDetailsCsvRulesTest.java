package uk.gov.moj.cpp.staging.civil.query.api.accesscontrol;

import static java.util.Collections.singletonMap;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import uk.gov.moj.cpp.accesscontrol.common.providers.UserAndGroupProvider;
import uk.gov.moj.cpp.accesscontrol.drools.Action;
import uk.gov.moj.cpp.accesscontrol.test.utils.BaseDroolsAccessControlTest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import org.kie.api.runtime.ExecutionResults;
import org.mockito.Mock;

public class QuerySubmissionErrorDetailsCsvRulesTest extends BaseDroolsAccessControlTest {

    private Action action;

    private final static String kSessionName = "QUERY_API_SESSION";

    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public QuerySubmissionErrorDetailsCsvRulesTest() {
        super(kSessionName);
    }

    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToGetSubmissionErrorDetailsCsv() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap<>();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "stagingprosecutorscivil.submission-error-details-csv");
        action = createActionFor(metadata);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getCivilCasePermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getCivilCasePermission());
    }
}
