package uk.gov.moj.cpp.staging.civil.handler.command.api.accesscontrol;

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

public class CommandCivilCaseRulesTest extends BaseDroolsAccessControlTest {

    private Action action;

    private final static String kSessionName = "COMMAND_API_SESSION";


    @Mock
    private UserAndGroupProvider userAndGroupProvider;

    public CommandCivilCaseRulesTest() {
        super(kSessionName);
    }


    @Override
    protected Map<Class<?>, Object> getProviderMocks() {
        return singletonMap(UserAndGroupProvider.class, userAndGroupProvider);
    }

    @Test
    public void shouldAllowAuthorisedUserToSubmitCivilCaseWithPermissionCivilAccess() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "stagingprosecutorscivil.charge-prosecution");
        action = createActionFor(metadata);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getCivilCasePermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getCivilCasePermission());
    }

    @Test
    public void shouldAllowAuthorisedUserToSubmitSummonWithPermissionCivilAccess() throws JsonProcessingException {
        final Map<String, String> metadata = new HashMap();
        metadata.putIfAbsent("id", UUID.randomUUID().toString());
        metadata.putIfAbsent("name", "stagingprosecutorscivil.summons-prosecution");
        action = createActionFor(metadata);
        given(userAndGroupProvider.hasPermission(action, RuleConstants.getCivilCasePermission())).willReturn(true);

        final ExecutionResults results = executeRulesWith(action);
        assertSuccessfulOutcome(results);
        verify(userAndGroupProvider, times(1)).hasPermission(action, RuleConstants.getCivilCasePermission());
    }
}
