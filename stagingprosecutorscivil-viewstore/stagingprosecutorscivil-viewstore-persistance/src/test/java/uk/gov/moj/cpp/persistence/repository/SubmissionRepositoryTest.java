package uk.gov.moj.cpp.persistence.repository;

import static java.time.ZonedDateTime.now;
import static java.util.UUID.randomUUID;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.entity.SubmissionType;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class SubmissionRepositoryTest {

    private static final String PERSISTENCE_UNIT = "stagingprosecutorscivil-test-persistence-unit";

    @RegisterExtension
    static HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider(PERSISTENCE_UNIT);

    private SubmissionRepository repository;

    @BeforeEach
    void createRepository() {
        repository = new SubmissionRepository();
        hibernateTestEntityManagerProvider.injectEntityManagerInto(repository);
    }

    @Test
    void shouldSaveAndReadOtherCase() {
        final UUID key = randomUUID();
        final UUID caseId = randomUUID();
        repository.save(aSubmission(key, caseId, SubmissionType.PROSECUTION));
        flushAndClear();

        final Submission result = repository.findBy(key);
        assertThat(result, is(notNullValue()));
        assertThat(result.getSubmissionId(), is(key));
        assertThat(result.getCaseDetail().stream().findFirst().get().getId(), is(caseId));
        assertThat(result.getType(), is(SubmissionType.PROSECUTION));
    }

    @Test
    void shouldSaveAndReadMaterialSubmission() {
        final UUID key = randomUUID();
        final UUID caseId = randomUUID();
        repository.save(aSubmission(key, caseId, SubmissionType.MATERIAL));
        flushAndClear();

        final Submission result = repository.findBy(key);
        assertThat(result, is(notNullValue()));
        assertThat(result.getSubmissionId(), is(key));
        assertThat(result.getCaseDetail().stream().findFirst().get().getId(), is(caseId));
        assertThat(result.getType(), is(SubmissionType.MATERIAL));
    }

    @Test
    void shouldUpdateExistingSubmissionOnSave() {
        final UUID key = randomUUID();
        repository.save(aSubmission(key, randomUUID(), SubmissionType.PROSECUTION));
        flushAndClear();

        final Submission existing = repository.findBy(key);
        existing.setSubmissionStatus("SUCCESS");
        existing.setCompletedAt(now());
        repository.save(existing);
        flushAndClear();

        final Submission result = repository.findBy(key);
        assertThat(result.getSubmissionStatus(), is("SUCCESS"));
        assertThat(result.getCompletedAt(), is(notNullValue()));
        assertThat(result.getCaseDetail().size(), is(1));
    }

    @Test
    void shouldReturnNullWhenSubmissionNotFound() {
        assertThat(repository.findBy(randomUUID()), is(nullValue()));
    }

    private Submission aSubmission(final UUID key, final UUID caseId, final SubmissionType type) {
        return Submission.builder()
                .withSubmissionId(key)
                .withCaseDetail(Collections.singleton(CaseDetail.builder().withId(caseId).build()))
                .withReceivedAt(now())
                .withType(type)
                .build();
    }

    private void flushAndClear() {
        repository.entityManager.flush();
        repository.entityManager.clear();
    }
}
