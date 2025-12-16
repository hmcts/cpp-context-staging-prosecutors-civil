package uk.gov.moj.cpp.persistence.entity;

import uk.gov.moj.cpp.persistence.converter.JsonArrayConverter;

import java.io.Serializable;
import java.util.UUID;

import javax.json.JsonArray;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "case_detail")
@SuppressWarnings({"squid:S2384","squid:S1948"})
public class CaseDetail implements Serializable {
    private static final long serialVersionUID = 2801203406435125940L;

    @ManyToOne
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @Column(name = "id")
    @Id
    private UUID id;

    @Column(name = "case_urn")
    private String caseUrn;

    @Column(name = "case_errors")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray caseErrors;

    @Column(name = "defendant_errors")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray defendantErrors;

    @Column(name = "defendant_warnings")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray defendantWarnings;

    @Column(name = "case_warnings")
    @Convert(converter = JsonArrayConverter.class)
    private JsonArray caseWarnings;

    public CaseDetail() {
    }

    public CaseDetail(final Submission submission,
                      final UUID id,
                      final String caseUrn,
                      final JsonArray caseErrors,
                      final JsonArray defendantErrors,
                      final JsonArray defendantWarnings,
                      final JsonArray caseWarnings) {
        this.submission = submission;
        this.id = id;
        this.caseUrn = caseUrn;
        this.caseErrors = caseErrors;
        this.defendantErrors = defendantErrors;
        this.defendantWarnings = defendantWarnings;
        this.caseWarnings = caseWarnings;
    }

    public Submission getSubmission() {
        return submission;
    }

    public void setSubmission(final Submission submission) {
        this.submission = submission;
    }

    public UUID getId() {
        return id;
    }

    public void setId(final UUID id) {
        this.id = id;
    }

    public String getCaseUrn() {
        return caseUrn;
    }

    public void setCaseUrn(final String caseUrn) {
        this.caseUrn = caseUrn;
    }

    public JsonArray getCaseErrors() {
        return caseErrors;
    }

    public void setCaseErrors(final JsonArray caseErrors) {
        this.caseErrors = caseErrors;
    }

    public JsonArray getDefendantErrors() {
        return defendantErrors;
    }

    public void setDefendantErrors(final JsonArray defendantErrors) {
        this.defendantErrors = defendantErrors;
    }

    public JsonArray getDefendantWarnings() {
        return defendantWarnings;
    }

    public void setDefendantWarnings(final JsonArray defendantWarnings) {
        this.defendantWarnings = defendantWarnings;
    }

    public JsonArray getCaseWarnings() {
        return caseWarnings;
    }

    public void setCaseWarnings(final JsonArray caseWarnings) {
        this.caseWarnings = caseWarnings;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Submission submission;
        private UUID id;
        private String caseUrn;
        private JsonArray caseErrors;
        private JsonArray defendantErrors;
        private JsonArray defendantWarnings;
        private JsonArray caseWarnings;

        public static Builder aCaseDetail() {
            return new Builder();
        }

        public Builder withSubmission(Submission submission) {
            this.submission = submission;
            return this;
        }

        public Builder withId(UUID id) {
            this.id = id;
            return this;
        }

        public Builder withCaseUrn(String caseUrn) {
            this.caseUrn = caseUrn;
            return this;
        }

        public Builder withCaseErrors(JsonArray caseErrors) {
            this.caseErrors = caseErrors;
            return this;
        }

        public Builder withDefendantErrors(JsonArray defendantErrors) {
            this.defendantErrors = defendantErrors;
            return this;
        }

        public Builder withDefendantWarnings(JsonArray defendantWarnings) {
            this.defendantWarnings = defendantWarnings;
            return this;
        }

        public Builder withCaseWarnings(JsonArray caseWarnings) {
            this.caseWarnings = caseWarnings;
            return this;
        }

        public CaseDetail build() {
            return new CaseDetail(submission, id , caseUrn, caseErrors, defendantErrors, defendantWarnings, caseWarnings);
        }
    }
}
