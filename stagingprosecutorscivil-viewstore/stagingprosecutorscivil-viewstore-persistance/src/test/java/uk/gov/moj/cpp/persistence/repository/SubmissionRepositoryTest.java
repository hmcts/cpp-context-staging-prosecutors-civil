package uk.gov.moj.cpp.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.BaseTransactionalJunit4Test;
import uk.gov.moj.cpp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.persistence.entity.Submission;

import java.util.Collections;
import java.util.UUID;

import javax.inject.Inject;

import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(CdiTestRunner.class)
public class SubmissionRepositoryTest extends BaseTransactionalJunit4Test {

    @Inject
    private SubmissionRepository repository;

    @Test
    public void shouldSaveAndReadChargeProsecution() {
        final UUID key = randomUUID();
        final UUID caseId = randomUUID();
        final Submission submission = Submission.builder().withSubmissionId(key).withCaseDetail(Collections.singleton(CaseDetail.builder().withId(caseId).build())).withReceivedAt(now()).build();
        repository.save(submission);

        final Submission result = repository.findBy(key);
        assertThat(result, is(notNullValue()));
        assertThat(result.getSubmissionId(), is(key));
        assertThat(result.getCaseDetail().stream().findFirst().get().getId(), is(caseId));
    }
}
