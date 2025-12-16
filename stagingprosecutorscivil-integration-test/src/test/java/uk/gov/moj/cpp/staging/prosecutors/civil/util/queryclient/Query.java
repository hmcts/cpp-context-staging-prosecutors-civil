package uk.gov.moj.cpp.staging.prosecutors.civil.util.queryclient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Query {

    String URI();

    String contentType();

}
