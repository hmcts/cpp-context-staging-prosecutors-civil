package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.Submission;

import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@ApplicationScoped
public class SubmissionRepository {

    @PersistenceContext(unitName = "stagingprosecutorscivil-persistence-unit")
    EntityManager entityManager;

    public Submission findBy(final UUID submissionId) {
        return entityManager.find(Submission.class, submissionId);
    }

    public Submission save(final Submission submission) {
        return entityManager.merge(submission);
    }
}
