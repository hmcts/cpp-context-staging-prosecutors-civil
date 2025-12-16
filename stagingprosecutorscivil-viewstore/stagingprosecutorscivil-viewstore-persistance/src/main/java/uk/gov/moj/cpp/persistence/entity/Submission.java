package uk.gov.moj.cpp.persistence.entity;

import static org.apache.deltaspike.core.util.CollectionUtils.isEmpty;

import uk.gov.moj.cpp.persistence.converter.JsonArrayConverter;

import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.json.JsonArray;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "submission")
@SuppressWarnings({"squid:S2384", "squid:S1948"})
public class Submission implements Serializable {
    private static final long serialVersionUID = 2601201206463125180L;

    @Id
    @Column(name = "submission_id")
    private UUID submissionId;

    @Column(name = "submission_status")
    private String submissionStatus;

    @Column(name = "ou_code")
    private String ouCode;

    @Column(name = "errors")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray errors;

    @Column(name = "warnings")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray warnings;

    @Column(name = "case_errors")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray groupCaseErrors;

    @Column(name = "defendant_errors")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray defendantErrors;

    @Column(name = "received_at", nullable = false)
    private ZonedDateTime receivedAt;

    @Column(name = "completed_at", nullable = true)
    private ZonedDateTime completedAt;

    @Column(name = "case_warnings")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray caseWarnings;

    @Column(name = "defendant_warnings")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray defendantWarnings;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "submission", orphanRemoval = true)
    private Set<CaseDetail> caseDetail = new HashSet<>();

    public Submission() {
    }

    public Submission(final UUID submissionId,
                      final String submissionStatus,
                      final String ouCode,
                      final JsonArray errors,
                      final JsonArray warnings,
                      final JsonArray caseWarnings,
                      final JsonArray defendantWarnings,
                      final ZonedDateTime receivedAt,
                      final ZonedDateTime completedAt,
                      final Set<CaseDetail> caseDetail) {
        this.submissionId = submissionId;
        this.submissionStatus = submissionStatus;
        this.ouCode = ouCode;
        this.errors = errors;
        this.warnings = warnings;
        this.caseWarnings = caseWarnings;
        this.defendantWarnings = defendantWarnings;
        this.receivedAt = receivedAt;
        this.completedAt = completedAt;
        this.caseDetail = caseDetail;
        this.setCaseDetail(caseDetail);
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public void setSubmissionId(final UUID submissionId) {
        this.submissionId = submissionId;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(final String submissionStatus) {
        this.submissionStatus = submissionStatus;
    }

    public String getOuCode() {
        return ouCode;
    }

    public void setOuCode(final String ouCode) {
        this.ouCode = ouCode;
    }

    public JsonArray getErrors() {
        return errors;
    }

    public void setErrors(final JsonArray errors) {
        this.errors = errors;
    }

    public JsonArray getWarnings() {
        return warnings;
    }

    public void setWarnings(final JsonArray warnings) {
        this.warnings = warnings;
    }

    public JsonArray getCaseWarnings() {
        return caseWarnings;
    }

    public void setCaseWarnings(final JsonArray caseWarnings) {
        this.caseWarnings = caseWarnings;
    }

    public JsonArray getDefendantWarnings() {
        return defendantWarnings;
    }

    public void setDefendantWarnings(final JsonArray defendantWarnings) {
        this.defendantWarnings = defendantWarnings;
    }

    public ZonedDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(final ZonedDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public ZonedDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(final ZonedDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Set<CaseDetail> getCaseDetail() {
        return caseDetail;
    }

    public void setCaseDetail(final Set<CaseDetail> caseDetails) {
        if (!isEmpty(caseDetails)) {
            this.caseDetail = new HashSet<>(caseDetails);
            this.caseDetail.forEach(cd -> cd.setSubmission(this));
        } else {
            this.caseDetail = new HashSet<>();
        }
    }

    public JsonArray getGroupCaseErrors() {
        return groupCaseErrors;
    }

    public void setGroupCaseErrors(JsonArray groupCaseErrors) {
        this.groupCaseErrors = groupCaseErrors;
    }

    public JsonArray getDefendantErrors() {
        return defendantErrors;
    }

    public void setDefendantErrors(JsonArray defendantErrors) {
        this.defendantErrors = defendantErrors;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID submissionId;
        private String submissionStatus;
        private String ouCode;
        private JsonArray errors;
        private JsonArray warnings;
        private JsonArray caseWarnings;
        private JsonArray defendantWarnings;
        private ZonedDateTime receivedAt;
        private ZonedDateTime completedAt;
        private Set<CaseDetail> caseDetail = new HashSet<>();

        public static Builder aSubmission() {
            return new Builder();
        }

        public Builder withSubmissionId(UUID submissionId) {
            this.submissionId = submissionId;
            return this;
        }

        public Builder withSubmissionStatus(String submissionStatus) {
            this.submissionStatus = submissionStatus;
            return this;
        }

        public Builder withOuCode(String ouCode) {
            this.ouCode = ouCode;
            return this;
        }

        public Builder withErrors(JsonArray errors) {
            this.errors = errors;
            return this;
        }

        public Builder withWarnings(JsonArray warnings) {
            this.warnings = warnings;
            return this;
        }

        public Builder withCaseWarnings(JsonArray caseWarnings) {
            this.caseWarnings = caseWarnings;
            return this;
        }

        public Builder withDefendantWarnings(JsonArray defendantWarnings) {
            this.defendantWarnings = defendantWarnings;
            return this;
        }

        public Builder withReceivedAt(ZonedDateTime receivedAt) {
            this.receivedAt = receivedAt;
            return this;
        }

        public Builder withCompletedAt(ZonedDateTime completedAt) {
            this.completedAt = completedAt;
            return this;
        }

        public Builder withCaseDetail(Set<CaseDetail> caseDetails) {
            if (!isEmpty(caseDetails)) {
                this.caseDetail = new HashSet<>(caseDetails);
            } else {
                this.caseDetail = new HashSet<>();
            }
            return this;
        }

        public Submission build() {
            return new Submission(submissionId, submissionStatus, ouCode, errors, warnings, caseWarnings, defendantWarnings, receivedAt, completedAt, caseDetail);
        }
    }
}
