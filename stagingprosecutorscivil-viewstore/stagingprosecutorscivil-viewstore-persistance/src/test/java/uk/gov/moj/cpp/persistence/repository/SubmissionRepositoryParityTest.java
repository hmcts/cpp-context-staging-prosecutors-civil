package uk.gov.moj.cpp.persistence.repository;

import static java.util.UUID.randomUUID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.Matchers.samePropertyValuesAs;

import uk.gov.justice.services.test.utils.persistence.HibernateTestEntityManagerProvider;
import uk.gov.moj.cpp.persistence.entity.CaseDetail;
import uk.gov.moj.cpp.persistence.entity.Submission;
import uk.gov.moj.cpp.persistence.entity.SubmissionType;

import java.io.StringReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonReader;
import jakarta.persistence.EntityManager;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * J17/J25 parity characterisation (J17 golden master) for SubmissionRepository.
 *
 * On J17 the repository is a DeltaSpike EntityRepository using the framework's EntityManager. The J25 upgrade
 * replaces it with a hand-written class on a container-managed @PersistenceContext and Hibernate 6, whose save()
 * always merges. Submission has an eager, cascading, orphan-removing set of CaseDetail children and JsonArray columns
 * mapped through JsonArrayConverter. Every assertion below encodes the J17 result.
 *
 * Harness note: under HSQLDialect on H2 (J17) a UUID value does not bind, so nothing here looks an entity up by id
 * or loads the case-detail collection after the persistence context is cleared: the tests read back by ou_code and
 * check case details with a native join. The update path (load, change, save, keep the case details) is checked
 * against PostgreSQL in JavaUpgradeParityIT instead.
 */
public class SubmissionRepositoryParityTest {

    private static final ZoneId LONDON = ZoneId.of("Europe/London");

    @RegisterExtension
    static final HibernateTestEntityManagerProvider hibernateTestEntityManagerProvider =
            new HibernateTestEntityManagerProvider("stagingprosecutorscivil-test-persistence-unit");

    private final SubmissionRepository submissionRepository = new SubmissionRepository();

    private EntityManager entityManager;

    private TimeZone originalDefaultTimeZone;

    @BeforeEach
    public void rememberDefaultTimeZone() {
        hibernateTestEntityManagerProvider.injectEntityManagerInto(submissionRepository);
        entityManager = hibernateTestEntityManagerProvider.getEntityManager();
        originalDefaultTimeZone = TimeZone.getDefault();
    }

    @AfterEach
    public void restoreDefaultTimeZone() {
        TimeZone.setDefault(originalDefaultTimeZone);
    }

    // SC-P01 (BC-01/02): findBy with an unknown id gives null; the listeners and processors check for it.
    @Test
    public void scP01_findByUnknownIdShouldReturnNull() {
        assertThat(submissionRepository.findBy(randomUUID()), is(nullValue()));
    }

    // SC-P02: on J17 the framework EntityManagerProducer set the JVM default time zone to UTC when it created the
    // EntityManager the DeltaSpike repository used.
    // J25 repositories no longer go through that producer, so using the repository leaves the JVM default time zone as it is.
    @Test
    public void scP02_repositoryUseShouldLeaveTheJvmDefaultTimeZoneUnchanged() {
        TimeZone.setDefault(TimeZone.getTimeZone(LONDON));
        final String defaultTimeZoneId = TimeZone.getDefault().getID();

        submissionRepository.save(submission("OU-P02", "URN-P02"));

        assertThat(TimeZone.getDefault().getID(), is(defaultTimeZoneId));
    }

    // SC-P03: save() of a new submission returns a managed submission with the same field values and case details.
    // DeltaSpike (J17) returned the same instance; the J25 merge() returns a managed copy. No caller relies on the identity.
    @Test
    public void scP03_saveOfNewSubmissionShouldReturnAManagedSubmissionWithTheSameFields() {
        final Submission submission = submission("OU-P03", "URN-P03-A", "URN-P03-B");
        final List<Matcher<? super CaseDetail>> sameCaseDetails = submission.getCaseDetail().stream()
                .<Matcher<? super CaseDetail>>map(caseDetail -> samePropertyValuesAs(caseDetail, "submission"))
                .toList();

        final Submission saved = submissionRepository.save(submission);

        assertThat(saved, samePropertyValuesAs(submission, "caseDetail"));
        assertThat(saved.getCaseDetail(), containsInAnyOrder(sameCaseDetails));
        saved.getCaseDetail().forEach(caseDetail -> assertThat(caseDetail.getSubmission(), sameInstance(saved)));
        assertThat(entityManager.contains(saved), is(true));
    }

    // SC-P04: the received handlers save a submission with errors/warnings null. The query view adds both to the
    // submission-details response unconditionally, so pin what is stored and what the entity reads back.
    @Test
    public void scP04_nullErrorsAndWarningsShouldBeStoredNullAndReadBackAsEmptyArrays() {
        submissionRepository.save(submission("OU-P04", "URN-P04"));
        entityManager.flush();
        entityManager.clear();

        final Object storedErrors = entityManager
                .createNativeQuery("select errors from submission where ou_code = 'OU-P04'")
                .getSingleResult();
        final Submission reloaded = reloadByOuCode("OU-P04");
        assertThat(storedErrors, is(nullValue()));
        assertThat(reloaded.getErrors() + " " + reloaded.getWarnings(), is("[] []"));
    }

    // SC-P05: JsonArrayConverter writes errors as the JsonArray's string form; the query view returns it parsed.
    @Test
    public void scP05_errorsShouldBeStoredInTheJ17TextForm() {
        final Submission submission = submission("OU-P05", "URN-P05");
        submission.setErrors(parseArray("[{\"code\":\"E1\",\"values\":[null,1,12.50,-0.0,true,\"café \\\"q\\\"\"],\"nested\":{\"k\":null}}]"));
        submissionRepository.save(submission);
        entityManager.flush();

        final Object storedErrors = entityManager
                .createNativeQuery("select errors from submission where ou_code = 'OU-P05'")
                .getSingleResult();
        // Glassfish writes the parsed -0.0 as 0.0
        assertThat(storedErrors, is("[{\"code\":\"E1\",\"values\":[null,1,12.50,0.0,true,\"caf\u00e9 \\\"q\\\"\"],\"nested\":{\"k\":null}}]"));
    }

    // SC-P06: saving a new submission cascades its case details, linked to the submission by submission_id.
    @Test
    public void scP06_saveOfNewSubmissionShouldCascadeItsCaseDetails() {
        submissionRepository.save(submission("OU-P06", "URN-A", "URN-B"));
        entityManager.flush();

        @SuppressWarnings("unchecked")
        final List<Object> caseUrns = entityManager
                .createNativeQuery("select cd.case_urn from case_detail cd join submission s on cd.submission_id = s.submission_id where s.ou_code = 'OU-P06'")
                .getResultList();
        assertThat(caseUrns, containsInAnyOrder("URN-A", "URN-B"));
    }


    private Submission reloadByOuCode(final String ouCode) {
        return entityManager
                .createQuery("select s from Submission s where s.ouCode = :ouCode", Submission.class)
                .setParameter("ouCode", ouCode)
                .getSingleResult();
    }



    private static JsonArray parseArray(final String json) {
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readArray();
        }
    }

    private static Submission submission(final String ouCode, final String... caseUrns) {
        final Set<CaseDetail> caseDetails = new HashSet<>();
        for (final String caseUrn : caseUrns) {
            caseDetails.add(CaseDetail.builder().withId(randomUUID()).withCaseUrn(caseUrn).build());
        }
        return Submission.builder()
                .withSubmissionId(randomUUID())
                .withSubmissionStatus("PENDING")
                .withOuCode(ouCode)
                .withReceivedAt(ZonedDateTime.now(LONDON))
                .withCaseDetail(caseDetails)
                .withErrors(null)
                .withWarnings(null)
                .withType(SubmissionType.PROSECUTION)
                .build();
    }
}
