package uk.gov.moj.cpp.staging.civil.processor.exception;

public class InvalidCaseUrnProvided extends RuntimeException {
    public InvalidCaseUrnProvided(final String message) {
        super(message);
    }
}
