package uk.gov.moj.cpp.staging.civil.handler.command.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

import uk.gov.justice.services.adapter.rest.interceptor.InputStreamFileInterceptor;
import uk.gov.justice.services.core.interceptor.InterceptorChainEntry;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StagingProsecutorsCivilServiceCommandApiInterceptorChainProviderTest {

    @InjectMocks
    private StagingProsecutorsCivilServiceCommandApiInterceptorChainProvider interceptorChainProvider;

    @Test
    @SuppressWarnings("unchecked")
    public void shouldProvideInterceptorChainTypes() {
        final List<InterceptorChainEntry> interceptorChainTypes = interceptorChainProvider.interceptorChainTypes();

        assertThat(interceptorChainTypes.size(), is(1));
        assertThat(interceptorChainTypes, containsInAnyOrder(
                new InterceptorChainEntry(6000, InputStreamFileInterceptor.class)
        ));
    }
}
