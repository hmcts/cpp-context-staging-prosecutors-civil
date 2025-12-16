package uk.gov.moj.cpp.staging.prosecutors.civil.healthchecks;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static uk.gov.justice.services.healthcheck.healthchecks.FileStoreHealthcheck.FILE_STORE_HEALTHCHECK_NAME;
import static uk.gov.justice.services.healthcheck.healthchecks.JobStoreHealthcheck.JOB_STORE_HEALTHCHECK_NAME;

import java.util.List;

@ExtendWith(MockitoExtension.class)
public class CivilIgnoredHealthcheckNamesProviderTest {

    @InjectMocks
    private CivilIgnoredHealthcheckNamesProvider ignoredHealthcheckNamesProvider;

  @Test
  public void shouldIgnoreFileStoreAndJobStoreHealthchecks() throws Exception {

    final List<String> namesOfIgnoredHealthChecks = ignoredHealthcheckNamesProvider.getNamesOfIgnoredHealthChecks();

    assertThat(namesOfIgnoredHealthChecks.size(), is(2));
    assertThat(namesOfIgnoredHealthChecks, hasItems(JOB_STORE_HEALTHCHECK_NAME, FILE_STORE_HEALTHCHECK_NAME));
  }

}