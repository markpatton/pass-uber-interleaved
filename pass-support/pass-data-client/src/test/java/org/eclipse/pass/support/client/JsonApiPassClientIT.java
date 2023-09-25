package org.eclipse.pass.support.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.pass.support.client.model.AggregatedDepositStatus;
import org.eclipse.pass.support.client.model.AwardStatus;
import org.eclipse.pass.support.client.model.CopyStatus;
import org.eclipse.pass.support.client.model.Deposit;
import org.eclipse.pass.support.client.model.DepositStatus;
import org.eclipse.pass.support.client.model.EventType;
import org.eclipse.pass.support.client.model.File;
import org.eclipse.pass.support.client.model.FileRole;
import org.eclipse.pass.support.client.model.Funder;
import org.eclipse.pass.support.client.model.Grant;
import org.eclipse.pass.support.client.model.IntegrationType;
import org.eclipse.pass.support.client.model.Journal;
import org.eclipse.pass.support.client.model.PassEntity;
import org.eclipse.pass.support.client.model.PerformerRole;
import org.eclipse.pass.support.client.model.PmcParticipation;
import org.eclipse.pass.support.client.model.Policy;
import org.eclipse.pass.support.client.model.Publication;
import org.eclipse.pass.support.client.model.Repository;
import org.eclipse.pass.support.client.model.RepositoryCopy;
import org.eclipse.pass.support.client.model.Source;
import org.eclipse.pass.support.client.model.Submission;
import org.eclipse.pass.support.client.model.SubmissionEvent;
import org.eclipse.pass.support.client.model.SubmissionStatus;
import org.eclipse.pass.support.client.model.User;
import org.eclipse.pass.support.client.model.UserRole;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class JsonApiPassClientIT {
    private static PassClient client;

    @BeforeAll
    public static void setup() {
        client = PassClient.newInstance();
    }

    @Test
    public void testCreateSimpleObject() throws IOException {
        Publication pub = new Publication();
        pub.setIssue("issue");
        pub.setPmid("pmid");

        client.createObject(pub);

        assertNotNull(pub.getId());

        Publication test = client.getObject(pub);

        assertEquals(pub, test);
    }

    @Test
    public void testCreateGetObject() throws IOException {
        User pi = new User();
        pi.setDisplayName("Bessie Cow");
        pi.setRoles(Arrays.asList(UserRole.ADMIN));

        client.createObject(pi);

        List<User> copis = new ArrayList<>();

        for (String name : Arrays.asList("Jessie Farmhand", "Cassie Farmhand")) {
            User user = new User();
            user.setDisplayName(name);
            user.setRoles(Arrays.asList(UserRole.SUBMITTER));

            client.createObject(user);
            copis.add(user);
        }

        Funder funder = new Funder();
        funder.setName("Farmer Bob");

        client.createObject(funder);

        Grant grant = new Grant();

        grant.setAwardNumber("award");
        grant.setLocalKey("localkey");
        grant.setAwardDate(dt("2014-03-28T00:00:00.000Z"));
        grant.setStartDate(dt("2016-01-10T02:12:13.040Z"));
        grant.setDirectFunder(funder);
        grant.setPi(pi);
        grant.setCoPis(copis);

        client.createObject(grant);

        // Get the grant with the relationship target objects included
        Grant test = client.getObject(grant, "directFunder", "pi", "coPis");

        assertEquals(grant, test);

        // Get the grant without the relationship target objects included
        test = client.getObject(grant);

        // Relationship targets should just have id
        grant.setDirectFunder(new Funder(funder.getId()));
        grant.setPi(new User(pi.getId()));
        grant.setCoPis(copis.stream().map(u -> new User(u.getId())).collect(Collectors.toList()));

        assertEquals(grant, test);

        // Get the grant with one relationship, other relationship targets should just
        // have id
        test = client.getObject(grant, "directFunder");

        grant.setDirectFunder(funder);

        assertEquals(grant, test);
    }

    @Test
    public void testUpdateObject_OptimisticLock_Submission() throws IOException {

        Submission sub = new Submission();
        sub.setAggregatedDepositStatus(AggregatedDepositStatus.NOT_STARTED);
        sub.setSource(Source.PASS);
        sub.setSubmitterName("Name");
        sub.setSubmitted(false);

        client.createObject(sub);

        Submission updateSubmission1 = client.getObject(Submission.class, sub.getId());
        Submission updateSubmission2 = client.getObject(Submission.class, sub.getId());

        updateSubmission1.setSource(Source.OTHER);
        client.updateObject(updateSubmission1);

        IOException ioException = assertThrows(IOException.class, () -> {
            updateSubmission2.setSubmitterName("Anothernewname");
            client.updateObject(updateSubmission2);
        });

        assertTrue(ioException.getMessage().contains("Update failed: http://localhost:8080/data/submission/"));
        assertTrue(ioException.getMessage().contains(
            "returned 409 {\"errors\":[{\"detail\":\"Optimistic lock check failed for Submission [ID="));
        assertTrue(ioException.getMessage().contains("Request version: 0, Stored version: 1\"}]}"));

        Submission updateSubmission3 = client.getObject(Submission.class, sub.getId());
        assertEquals("Name", updateSubmission3.getSubmitterName());
    }

    @Test
    public void testUpdateObject_OptimisticLock_Deposit() throws IOException {

        Deposit deposit = new Deposit();
        deposit.setDepositStatus(DepositStatus.SUBMITTED);

        client.createObject(deposit);

        Deposit updateDeposit1 = client.getObject(Deposit.class, deposit.getId());
        Deposit updateDeposit2 = client.getObject(Deposit.class, deposit.getId());

        updateDeposit1.setDepositStatus(DepositStatus.FAILED);
        client.updateObject(updateDeposit1);

        IOException ioException = assertThrows(IOException.class, () -> {
            updateDeposit2.setDepositStatus(DepositStatus.ACCEPTED);
            client.updateObject(updateDeposit2);
        });

        assertTrue(ioException.getMessage().contains("Update failed: http://localhost:8080/data/deposit/"));
        assertTrue(ioException.getMessage().contains(
            "returned 409 {\"errors\":[{\"detail\":\"Optimistic lock check failed for Deposit [ID="));
        assertTrue(ioException.getMessage().contains("Request version: 0, Stored version: 1\"}]}"));

        Deposit updateDeposit3 = client.getObject(Deposit.class, deposit.getId());
        assertEquals(DepositStatus.FAILED, updateDeposit3.getDepositStatus());
    }

    @Test
    public void testUpdateObject() throws IOException {

        Publication pub1 = new Publication();
        pub1.setTitle("Ten puns");

        Publication pub2 = new Publication();
        pub1.setTitle("Twenty puns");

        client.createObject(pub1);
        client.createObject(pub2);

        Submission sub = new Submission();

        sub.setAggregatedDepositStatus(AggregatedDepositStatus.NOT_STARTED);
        sub.setSource(Source.PASS);
        sub.setPublication(pub1);
        sub.setSubmitterName("Name");
        sub.setSubmitted(false);

        client.createObject(sub);

        assertEquals(sub, client.getObject(sub, "publication"));

        // Try to update pub1 attributes
        pub1.setTitle("Different title");
        client.updateObject(pub1);
        assertEquals(pub1, client.getObject(pub1));

        // Try to remove attribute
        pub1.setTitle(null);
        client.updateObject(pub1);
        assertEquals(pub1, client.getObject(pub1));

        // Try to update sub attributes and relationship
        sub.setSource(Source.OTHER);
        sub.setSubmissionStatus(SubmissionStatus.CANCELLED);
        sub.setPublication(pub2);
        client.updateObject(sub);
        assertEquals(sub, client.getObject(sub, "publication"));

        // Try to remove the relationship
        sub.setPublication(null);
        client.updateObject(sub);
        assertEquals(sub, client.getObject(sub, "publication"));
    }

    @Test
    public void testUpdateObjectMultipleRelationships() throws IOException {
        Repository rep1 = new Repository();
        rep1.setDescription("one");

        Repository rep2 = new Repository();
        rep2.setDescription("two");

        client.createObject(rep1);
        client.createObject(rep2);

        Submission sub = new Submission();
        sub.setSubmitterName("Bob");

        client.createObject(sub);

        // Add multiple value relationship
        sub.setRepositories(Arrays.asList(rep1, rep2));
        client.updateObject(sub);
        assertEquals(sub, client.getObject(sub, "repositories"));

        // Delete one value of relationship
        sub.setRepositories(Arrays.asList(rep2));
        client.updateObject(sub);
        assertEquals(sub, client.getObject(sub, "repositories"));

        // Try to delete the relationship
        sub.setRepositories(null);
        client.updateObject(sub);
        assertEquals(sub, client.getObject(sub, "repositories"));
    }

    @Test
    public void testSelectObjects() throws IOException {
        String pmid = "" + UUID.randomUUID();

        Journal journal = new Journal();
        journal.setJournalName("The ministry of silly walks");

        client.createObject(journal);

        List<Publication> pubs = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Publication pub = new Publication();

            pub.setIssue("Number: " + i);
            pub.setTitle("Title: " + i);
            pub.setPmid(pmid);
            pub.setJournal(journal);

            client.createObject(pub);
            pubs.add(pub);
        }

        String filter = RSQL.equals("pmid", pmid);
        PassClientSelector<Publication> selector = new PassClientSelector<>(Publication.class, 0, 100, filter, "id");
        selector.setInclude("journal");
        PassClientResult<Publication> result = client.selectObjects(selector);

        assertEquals(pubs.size(), result.getTotal());
        assertIterableEquals(pubs, result.getObjects());

        // Test selecting with an offset
        selector = new PassClientSelector<>(Publication.class, 5, 100, filter, "id");
        selector.setInclude("journal");
        result = client.selectObjects(selector);

        assertEquals(pubs.size(), result.getTotal());
        assertIterableEquals(pubs.subList(5, pubs.size()), result.getObjects());

        // Test using a stream which will make multiple calls. Do not include journal.
        selector = new PassClientSelector<>(Publication.class, 0, 2, filter, "id");
        pubs.forEach(p -> p.setJournal(new Journal(journal.getId())));
        assertIterableEquals(pubs, client.streamObjects(selector).collect(Collectors.toList()));

        // Test searching on a relationship. Do not include journal.
        filter = RSQL.equals("journal.id", journal.getId());
        selector = new PassClientSelector<>(Publication.class, 0, 100, filter, "id");
        assertIterableEquals(pubs, client.streamObjects(selector).collect(Collectors.toList()));
    }

    @Test
    public void testSelectUserObjects_Success_HasMember() throws IOException {
        User pi = new User();
        pi.setAffiliation(Collections.singleton("affil"));
        pi.setDisplayName("John");
        pi.setEmail("johndoe@example.com");
        pi.setFirstName("John");
        pi.setLastName("Doe");
        pi.setLocatorIds(List.of("locator-a", "locator-b"));
        pi.setOrcidId("11xx-xxxx-xxxx-xxxx");
        pi.setRoles(Arrays.asList(UserRole.SUBMITTER));
        pi.setUsername("johndoe1");

        client.createObject(pi);

        PassClientSelector<User> selector = new PassClientSelector<>(User.class);
        selector.setFilter(RSQL.hasMember("locatorIds", "locator-b"));
        PassClientResult<User> result = client.selectObjects(selector);

        assertEquals(1, result.getTotal());
        User selectedUser = result.getObjects().get(0);
        assertEquals(pi, selectedUser);
    }

    @Test
    public void testSelectUserObjects_Success_HasNoMember() throws IOException {
        User pi = new User();
        pi.setAffiliation(Collections.singleton("affil"));
        pi.setDisplayName("John2");
        pi.setEmail("johndoe2@example.com");
        pi.setFirstName("John2");
        pi.setLastName("Doe2");
        pi.setLocatorIds(List.of("locator-a", "locator-foobar"));
        pi.setOrcidId("22xx-xxxx-xxxx-xxxx");
        pi.setRoles(Arrays.asList(UserRole.SUBMITTER));
        pi.setUsername("johndoe2");

        client.createObject(pi);

        PassClientSelector<User> selector = new PassClientSelector<>(User.class);
        selector.setFilter(RSQL.hasNoMember("locatorIds", "locator-foobar"));
        PassClientResult<User> result = client.selectObjects(selector);

        assertTrue(result.getObjects().size() > 0);
        assertTrue(result.getObjects().stream().noneMatch(user -> Objects.equals(user.getUsername(), "johndoe2")));
    }

    @Test
    public void testSelectDepositObjects_Success_WithEqualsSubmissionDateFilter() throws IOException {
        Submission submission = new Submission();
        submission.setSubmittedDate(dt("2023-09-01T02:01:20.300Z"));

        client.createObject(submission);

        Deposit deposit = new Deposit();
        deposit.setSubmission(submission);

        client.createObject(deposit);

        PassClientSelector<Deposit> selector = new PassClientSelector<>(Deposit.class);
        selector.setFilter(RSQL.equals("submission.submittedDate", "2023-09-01T02:01:20.300Z"));
        PassClientResult<Deposit> result = client.selectObjects(selector);

        assertEquals(1, result.getObjects().size());
        Deposit foundDeposit = result.getObjects().get(0);
        assertEquals(deposit.getId(), foundDeposit.getId());
        assertEquals(submission.getId(), foundDeposit.getSubmission().getId());
    }

    @Test
    public void testSelectDepositObjects_Success_WithGteSubmissionDateFoundFilter() throws IOException {
        Submission submission = new Submission();
        submission.setSubmittedDate(dt("2023-09-02T02:01:20.300Z"));

        client.createObject(submission);

        Deposit deposit = new Deposit();
        deposit.setDepositStatusRef("gte-found");
        deposit.setSubmission(submission);

        client.createObject(deposit);

        PassClientSelector<Deposit> selector = new PassClientSelector<>(Deposit.class);
        selector.setFilter(
            RSQL.and(
                RSQL.gte("submission.submittedDate", "2023-09-01T02:01:20.300Z"),
                RSQL.equals("depositStatusRef", "gte-found")
            )
        );
        PassClientResult<Deposit> result = client.selectObjects(selector);

        assertEquals(1, result.getObjects().size());
        Deposit foundDeposit = result.getObjects().get(0);
        assertEquals(deposit.getId(), foundDeposit.getId());
        assertEquals(submission.getId(), foundDeposit.getSubmission().getId());
    }

    @Test
    public void testSelectDepositObjects_Success_WithGteSubmissionDateFilter() throws IOException {
        Submission submission = new Submission();
        submission.setSubmittedDate(dt("2023-09-01T01:01:20.300Z"));

        client.createObject(submission);

        Deposit deposit = new Deposit();
        deposit.setDepositStatusRef("gte-notfound");
        deposit.setSubmission(submission);

        client.createObject(deposit);

        PassClientSelector<Deposit> selector = new PassClientSelector<>(Deposit.class);
        selector.setFilter(
            RSQL.and(
                RSQL.gte("submission.submittedDate", "2023-09-02T01:01:20.300Z"),
                RSQL.equals("depositStatusRef", "gte-notfound")
            )
        );
        PassClientResult<Deposit> result = client.selectObjects(selector);

        assertEquals(0, result.getObjects().size());
    }

    @Test
    public void testSelectDepositObjects_Success_WithLteSubmissionDateFoundFilter() throws IOException {
        Submission submission = new Submission();
        submission.setSubmittedDate(dt("2023-09-02T01:01:20.300Z"));

        client.createObject(submission);

        Deposit deposit = new Deposit();
        deposit.setDepositStatusRef("lte-found");
        deposit.setSubmission(submission);

        client.createObject(deposit);

        PassClientSelector<Deposit> selector = new PassClientSelector<>(Deposit.class);
        selector.setFilter(
            RSQL.and(
                RSQL.lte("submission.submittedDate", "2023-09-03T02:01:20.300Z"),
                RSQL.equals("depositStatusRef", "lte-found")
            )
        );
        PassClientResult<Deposit> result = client.selectObjects(selector);

        assertEquals(1, result.getObjects().size());
        Deposit foundDeposit = result.getObjects().get(0);
        assertEquals(deposit.getId(), foundDeposit.getId());
        assertEquals(submission.getId(), foundDeposit.getSubmission().getId());
    }

    @Test
    public void testSelectDepositObjects_Success_WithLteSubmissionDateFilter() throws IOException {
        Submission submission = new Submission();
        submission.setSubmittedDate(dt("2023-09-02T02:01:20.300Z"));

        client.createObject(submission);

        Deposit deposit = new Deposit();
        deposit.setDepositStatusRef("lte-notfound");
        deposit.setSubmission(submission);

        client.createObject(deposit);

        PassClientSelector<Deposit> selector = new PassClientSelector<>(Deposit.class);
        selector.setFilter(RSQL.lte("submission.submittedDate", "2023-09-01T02:01:20.300Z"));
        selector.setFilter(
            RSQL.and(
                RSQL.lte("submission.submittedDate", "2023-09-01T02:01:20.300Z"),
                RSQL.equals("depositStatusRef", "lte-notfound")
            )
        );
        PassClientResult<Deposit> result = client.selectObjects(selector);

        assertEquals(0, result.getObjects().size());
    }

    private static ZonedDateTime dt(String s) {
        return ZonedDateTime.parse(s, ModelUtil.dateTimeFormatter());
    }

    @Test
    public void testAllObjects() {
        User pi = new User();
        pi.setAffiliation(Collections.singleton("affil"));
        pi.setDisplayName("Farmer Bob");
        pi.setEmail("farmerbob@example.com");
        pi.setFirstName("Bob");
        pi.setLastName("Bobberson");
        pi.setLocatorIds(Collections.singletonList("locator1"));
        pi.setMiddleName("Bobbit");
        pi.setOrcidId("23xx-xxxx-xxxx-xxxx");
        pi.setRoles(Arrays.asList(UserRole.SUBMITTER));
        pi.setUsername("farmerbob1");

        User copi = new User();
        copi.setAffiliation(Collections.singleton("barn"));
        copi.setDisplayName("Bessie The Cow");
        copi.setEmail("bessie@example.com");
        copi.setFirstName("Bessie");
        copi.setLastName("Cow");
        copi.setLocatorIds(Collections.singletonList("locator2"));
        copi.setMiddleName("The");
        copi.setOrcidId("12xx-xxxx-xxxx-xxxx");
        copi.setRoles(Arrays.asList(UserRole.SUBMITTER));
        copi.setUsername("bessie1");

        User preparer = new User();
        copi.setAffiliation(Collections.singleton("dairy"));
        copi.setDisplayName("Darren Dairy");
        copi.setEmail("darren@example.com");
        copi.setFirstName("Darren");
        copi.setLastName("Dairy");
        copi.setLocatorIds(Collections.singletonList("locator4"));
        copi.setOrcidId("15xx-xxxx-xxxx-xxxx");
        copi.setRoles(Arrays.asList(UserRole.SUBMITTER));
        copi.setUsername("darren1");

        Repository repository = new Repository();

        repository.setAgreementText("I agree to everything.");
        repository.setDescription("Repository description");
        repository.setFormSchema("form schema");
        repository.setIntegrationType(IntegrationType.FULL);
        repository.setName("Barn repository");
        repository.setRepositoryKey("barn");
        repository.setSchemas(Arrays.asList(URI.create("http://example.com/schema")));
        repository.setUrl(URI.create("http://example.com/barn.html"));

        Policy policy = new Policy();

        policy.setDescription("This is a policy description");
        policy.setInstitution(URI.create("https://jhu.edu"));
        policy.setPolicyUrl(URI.create("http://example.com/policy/oa.html"));
        policy.setRepositories(Arrays.asList(repository));
        policy.setTitle("Policy title");

        Funder primary = new Funder();

        primary.setLocalKey("bovine");
        primary.setName("Bovines R Us");
        primary.setPolicy(policy);
        primary.setUrl(URI.create("http://example.com/bovine"));

        Funder direct = new Funder();

        direct.setLocalKey("icecream");
        direct.setName("Icecream is great");
        direct.setPolicy(policy);
        direct.setUrl(URI.create("http://example.com/ice"));

        Grant grant = new Grant();

        grant.setAwardDate(dt("2010-01-10T02:01:20.300Z"));
        grant.setAwardNumber("moo42");
        grant.setAwardStatus(AwardStatus.ACTIVE);
        grant.setCoPis(Arrays.asList(copi));
        grant.setDirectFunder(direct);
        grant.setPrimaryFunder(primary);
        grant.setEndDate(dt("2015-12-10T02:04:20.300Z"));
        grant.setLocalKey("moo:42");
        grant.setPi(pi);
        grant.setProjectName("Moo Thru revival");
        grant.setStartDate(dt("2011-02-13T01:05:20.300Z"));

        Journal journal = new Journal();

        journal.setIssns(Arrays.asList("issn1"));
        journal.setJournalName("Ice Cream International");
        journal.setPmcParticipation(PmcParticipation.A);

        Publication publication = new Publication();

        publication.setDoi("doi");
        publication.setIssue("3");
        publication.setJournal(journal);
        publication.setPmid("pmid");
        publication.setPublicationAbstract("Let x be...");
        publication.setTitle("This is a huge title");
        publication.setVolume("1 liter");

        Submission submission = new Submission();
        submission.setAggregatedDepositStatus(AggregatedDepositStatus.ACCEPTED);
        submission.setEffectivePolicies(Arrays.asList(policy));
        submission.setGrants(Arrays.asList(grant));
        submission.setMetadata("metadata");
        submission.setPreparers(Arrays.asList(preparer));
        submission.setPublication(publication);
        submission.setSource(Source.PASS);
        submission.setSubmissionStatus(null);
        submission.setSubmitted(true);
        submission.setSubmittedDate(dt("2012-12-10T02:01:20.300Z"));
        submission.setSubmitter(pi);
        submission.setSubmitterEmail(URI.create("mailto:" + pi.getEmail()));
        submission.setSubmitterName(pi.getDisplayName());

        SubmissionEvent event = new SubmissionEvent();
        event.setComment("This is a comment.");
        event.setEventType(EventType.SUBMITTED);
        event.setLink(URI.create("http://example.com/link"));
        event.setPerformedBy(pi);
        event.setPerformedDate(dt("2010-12-10T02:01:20.300Z"));
        event.setPerformerRole(PerformerRole.SUBMITTER);
        event.setSubmission(submission);

        RepositoryCopy rc = new RepositoryCopy();
        rc.setAccessUrl(URI.create("http://example.com/repo/item"));
        rc.setCopyStatus(CopyStatus.ACCEPTED);
        rc.setExternalIds(Arrays.asList("rc1"));
        rc.setPublication(publication);
        rc.setRepository(repository);

        File file = new File();

        file.setDescription("This is a file");
        file.setFileRole(FileRole.MANUSCRIPT);
        file.setMimeType("application/pdf");
        file.setName("ms.pdf");
        file.setSubmission(submission);
        file.setUri(URI.create("http://example.com/ms.pdf"));

        Deposit deposit = new Deposit();

        deposit.setDepositStatus(DepositStatus.ACCEPTED);
        deposit.setRepository(repository);
        deposit.setRepositoryCopy(rc);

        // Check that all the objects can be created.
        // Order such that relationship targets are created first.
        List<PassEntity> objects = Arrays.asList(pi, copi, preparer, repository, policy, primary, direct, grant,
                journal, publication, submission, event, rc, file, deposit);

        objects.forEach(o -> {
            try {
                client.createObject(o);
                assertNotNull(o.getId());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        // Check that objects can be retrieved.
        // For equality test, relationship targets must only have id

        policy.setRepositories(Arrays.asList(new Repository(repository.getId())));
        primary.setPolicy(new Policy(policy.getId()));
        direct.setPolicy(new Policy(policy.getId()));
        grant.setCoPis(Arrays.asList(new User(copi.getId())));
        grant.setPi(new User(pi.getId()));
        grant.setDirectFunder(new Funder(direct.getId()));
        grant.setPrimaryFunder(new Funder(primary.getId()));
        publication.setJournal(new Journal(journal.getId()));
        submission.setGrants(Arrays.asList(new Grant(grant.getId())));
        submission.setEffectivePolicies(Arrays.asList(new Policy(policy.getId())));
        submission.setPreparers(Arrays.asList(new User(preparer.getId())));
        submission.setPublication(new Publication(publication.getId()));
        submission.setSubmitter(new User(pi.getId()));
        event.setPerformedBy(new User(pi.getId()));
        event.setSubmission(new Submission(submission.getId()));
        rc.setPublication(new Publication(publication.getId()));
        rc.setRepository(new Repository(repository.getId()));
        file.setSubmission(new Submission(submission.getId()));
        deposit.setRepository(new Repository(repository.getId()));
        deposit.setRepositoryCopy(new RepositoryCopy(rc.getId()));

        objects.forEach(o -> {
            try {
                assertEquals(o, client.getObject(o));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testUploadDownloadFile() throws IOException {
        File file = new File();

        String data = "What's in a name?";
        file.setName("rose.txt");

        URI data_uri = client.uploadBinary(file.getName(), data.getBytes(StandardCharsets.UTF_8));

        assertNotNull(data_uri);

        file.setUri(data_uri);

        client.createObject(file);

        InputStream is = client.downloadFile(file);

        String test_data = new String(is.readAllBytes(), StandardCharsets.UTF_8);

        assertEquals(data, test_data);
    }
}
