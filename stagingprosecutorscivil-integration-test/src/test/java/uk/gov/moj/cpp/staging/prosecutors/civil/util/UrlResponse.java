package uk.gov.moj.cpp.staging.prosecutors.civil.util;

import java.util.UUID;

public class UrlResponse {
    private final String statusURL;

    private final UUID submissionId;

    public UrlResponse(final String statusURL, final UUID submissionId) {
        this.statusURL = statusURL;
        this.submissionId = submissionId;
    }

    public String getStatusURL() {
        return statusURL;
    }

    public UUID getSubmissionId() {
        return submissionId;
    }

    public static Builder urlResponse() {
        return new UrlResponse.Builder();
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        final UrlResponse that = (UrlResponse) obj;

        return java.util.Objects.equals(this.statusURL, that.statusURL) &&
                java.util.Objects.equals(this.submissionId, that.submissionId);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(statusURL, submissionId);}

    @Override
    public String toString() {
        return "UrlResponse{" +
                "statusURL='" + statusURL + "'," +
                "submissionId='" + submissionId + "'" +
                "}";
    }

    public static class Builder {
        private String statusURL;

        private UUID submissionId;

        public Builder withStatusURL(final String statusURL) {
            this.statusURL = statusURL;
            return this;
        }

        public Builder withSubmissionId(final UUID submissionId) {
            this.submissionId = submissionId;
            return this;
        }

        public Builder withValuesFrom(final UrlResponse urlResponse) {
            this.statusURL = urlResponse.getStatusURL();
            this.submissionId = urlResponse.getSubmissionId();
            return this;
        }

        public UrlResponse build() {
            return new UrlResponse(statusURL, submissionId);
        }
    }
}
