package uk.gov.moj.cpp.staging.prosecutors.civil.model;

import uk.gov.moj.cpp.staging.prosecutors.civil.common.Problem;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.queryclient.Query;
import uk.gov.moj.cpp.staging.prosecutors.civil.util.queryclient.QueryPoller;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@Query(URI = "/v1/submissions/{submissionId}", contentType = "application/vnd.hmcts.cjs.submission+json")
public class Submission {

    private final UUID submissionId;

    private final String submissionStatus;

    private final List<Problem> materialErrors;

    private final List<Problem> materialWarnings;

    private final String type;

    private final ZonedDateTime receivedAt;

    private final ZonedDateTime completedAt;

    @JsonCreator
    public Submission(
            @JsonProperty("id") UUID submissionId,
            @JsonProperty("status") String submissionStatus,
            @JsonProperty("materialErrors") List<Problem> materialErrors,
            @JsonProperty("materialWarnings") List<Problem> materialWarnings,
            @JsonProperty("type") String type,
            @JsonProperty("receivedAt") ZonedDateTime receivedAt,
            @JsonProperty("completedAt") ZonedDateTime completedAt) {

        this.submissionId = submissionId;
        this.submissionStatus = submissionStatus;
        this.materialErrors = materialErrors;
        this.materialWarnings = materialWarnings;
        this.type = type;
        this.receivedAt = receivedAt;
        this.completedAt = completedAt;
    }

    public static QueryPoller<Submission> poller() {
        return new QueryPoller<>(Submission.class);
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public String getType() {
        return type;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public ZonedDateTime getReceivedAt() {
        return receivedAt;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    public List<Problem> getMaterialErrors() {
        return materialErrors;
    }

    public List<Problem> getMaterialWarnings() {
        return materialWarnings;
    }
}
