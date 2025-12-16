package uk.gov.moj.cpp.persistence.repository;

import uk.gov.moj.cpp.persistence.entity.Submission;

import java.util.UUID;

import org.apache.deltaspike.data.api.EntityRepository;
import org.apache.deltaspike.data.api.Repository;

@Repository
public interface SubmissionRepository extends EntityRepository<Submission, UUID> {
}

