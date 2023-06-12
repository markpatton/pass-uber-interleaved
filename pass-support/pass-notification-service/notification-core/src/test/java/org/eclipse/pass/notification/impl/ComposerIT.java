/*
 *
 *  * Copyright 2018 Johns Hopkins University
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.eclipse.pass.notification.impl;

import static java.lang.String.join;
import static java.nio.charset.Charset.forName;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.io.IOUtils.resourceToString;
import static org.eclipse.pass.notification.impl.Composer.getRecipientConfig;
import static org.eclipse.pass.notification.impl.Links.deserialize;
import static org.eclipse.pass.notification.model.Link.Rels.SUBMISSION_REVIEW_INVITE;
import static org.eclipse.pass.notification.model.Notification.Param.CC;
import static org.eclipse.pass.notification.model.Notification.Param.EVENT_METADATA;
import static org.eclipse.pass.notification.model.Notification.Param.FROM;
import static org.eclipse.pass.notification.model.Notification.Param.LINKS;
import static org.eclipse.pass.notification.model.Notification.Param.RESOURCE_METADATA;
import static org.eclipse.pass.notification.model.Notification.Param.SUBJECT;
import static org.eclipse.pass.notification.model.Notification.Param.TO;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.pass.support.client.PassClient;
import org.eclipse.pass.support.client.model.Submission;
import org.eclipse.pass.support.client.model.SubmissionEvent;
import org.eclipse.pass.notification.NotificationApp;
import org.eclipse.pass.notification.model.Link;
import org.eclipse.pass.notification.model.Notification;
import org.eclipse.pass.notification.model.config.Mode;
import org.eclipse.pass.notification.model.config.NotificationConfig;
import org.eclipse.pass.notification.model.config.RecipientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = NotificationApp.class)
public class ComposerIT {

    private String submissionId = "http://example.org/submission/1";

    private String eventId = "http://example.org/event/1";

    private String preparer_1 = "mailto:preparer_one@example.org";

    private String preparer_2 = "mailto:preparer_two@example.org";

    private Collection<String> preparers = Arrays.asList(preparer_1, preparer_2);

    private String submitter = "mailto:submitter@example.org";

    private Submission submission;

    private SubmissionEvent submissionEvent;

    private URI eventLink = URI.create("http://example.org/eventLink");

    @Autowired
    private NotificationConfig config;

    @Autowired
    private Composer composer;

    @Autowired
    private PassClient passClient;

    @Autowired
    @Qualifier("objectMapper")
    private ObjectMapper mapper;

    @Before
    public void setUp() throws Exception {
        assertNotNull(composer.getRecipientAnalyzer());
        assertNotNull(composer.getRecipientConfig());

        submission = mock(Submission.class);
        submissionEvent = mock(SubmissionEvent.class);

        when(submissionEvent.getId()).thenReturn(URI.create(eventId));
        when(submissionEvent.getSubmission()).thenReturn(URI.create(submissionId));
        when(submissionEvent.getLink()).thenReturn(eventLink);
        when(submission.getId()).thenReturn(URI.create(submissionId));
        when(submission.getPreparers()).thenReturn(preparers.stream().map(URI::create).collect(Collectors.toList()));
        when(submission.getSubmitter()).thenReturn(URI.create(submitter));
    }

    /**
     * When the Submission's Submitter URI is null, the Submission's Submitter Email URI should be used instead.
     */
    @Test
    public void testNullSubmissionSubmitterUri() {
        submission = mock(Submission.class);

        when(submission.getId()).thenReturn(URI.create(submissionId));
        when(submission.getSubmitter()).thenReturn(null);
        when(submission.getSubmitterEmail()).thenReturn(URI.create(submitter));
        when(submissionEvent.getEventType()).thenReturn(SubmissionEvent.EventType.APPROVAL_REQUESTED);

        assertEquals(singleton(submitter),
                composer.apply(submission, submissionEvent).getRecipients());

        verify(submission).getSubmitter();
        verify(submission).getSubmitterEmail();
    }

    /**
     * When the Submission's Submitter URI is not null, it should take precedence over the use of the Submission's
     * Submitter Email URI.
     */
    @Test
    public void testNonNullSubmissionSubmitterUri() {
        assertNotNull(submission.getSubmitter());
        assertNull(submission.getSubmitterEmail());
        when(submissionEvent.getEventType()).thenReturn(SubmissionEvent.EventType.APPROVAL_REQUESTED);

        assertEquals(singleton(submitter),
                composer.apply(submission, submissionEvent).getRecipients());

        // once above, and once in the tested code
        verify(submission, times(2)).getSubmitter();
        // one time - above, but never in the tested code since the submitter uri has precedence (and was non-null)
        verify(submission, times(1)).getSubmitterEmail();
    }

    /**
     * When the Submission's Submitter URI and the Submitter Email URI are
     * null, a runtime exception should be thrown.
     */
    @Test
    public void testNullSubmissionSubmitterUriAndNullEmailUri() {
        when(submission.getId()).thenReturn(URI.create(submissionId));
        when(submission.getSubmitter()).thenReturn(null);
        when(submission.getSubmitterEmail()).thenReturn(null);
        when(submissionEvent.getEventType()).thenReturn(SubmissionEvent.EventType.APPROVAL_REQUESTED);

        try {
            composer.apply(submission, submissionEvent).getRecipients();
            fail("Expected a runtime exception to be thrown.");
        } catch (Exception expected) {
            assertTrue(expected instanceof RuntimeException);
        }

        verify(submission).getSubmitter();
        verify(submission).getSubmitterEmail();
    }

    /**
     * Insure the mode for the RecipientConfig matches the NotificationConfig mode
     */
    @Test
    public void testRecipientConfigForMode() {
        assertEquals(config.getMode(), composer.getRecipientConfig().getMode());
    }

    /**
     * Insure the proper from address is used for the specified mode
     */
    @Test
    public void testFromForMode() {
        RecipientConfig expectedRc = config.getRecipientConfigs().stream()
                .filter(Composer.RecipientConfigFilter.modeFilter(config)).findFirst()
                .orElseThrow(() -> new RuntimeException("Missing RecipientConfig for mode '" + config.getMode() + "'"));
        String expectedFromAddress = expectedRc.getFromAddress();

        assertEquals(expectedFromAddress, composer.getRecipientConfig().getFromAddress());
    }

    @Test
    @DirtiesContext
    public void testFromForEachMode() {
        // make a unique from address and recipient config for each possible mode
        HashMap<Mode, RecipientConfig> rcs = new HashMap<>();
        Arrays.stream(Mode.values()).forEach(m -> {
            RecipientConfig rc = new RecipientConfig();
            rc.setMode(m);
            rc.setFromAddress(UUID.randomUUID().toString());
            rcs.put(m, rc);
        });

        config.setRecipientConfigs(rcs.values());

        Arrays.stream(Mode.values()).forEach(mode -> {
            config.setMode(mode);
            assertEquals(mode, new Composer(config, mapper).getRecipientConfig().getMode());
            assertEquals(
                rcs.get(mode).getFromAddress(), new Composer(config, mapper).getRecipientConfig().getFromAddress()
            );
        });
    }

    /**
     * Insure the proper global CC addresses are used for the specified mode
     */
    @Test
    @DirtiesContext
    public void testGlobalCCForEachMode() {
        // make a unique global address and recipient config for each possible mode
        HashMap<Mode, RecipientConfig> rcs = new HashMap<>();
        Arrays.stream(Mode.values()).forEach(m -> {
            RecipientConfig rc = new RecipientConfig();
            rc.setMode(m);
            rc.setGlobalCc(singleton(UUID.randomUUID().toString()));
            rcs.put(m, rc);
        });

        config.setRecipientConfigs(rcs.values());

        Arrays.stream(Mode.values()).forEach(mode -> {
            config.setMode(mode);
            assertEquals(mode, new Composer(config, mapper).getRecipientConfig().getMode());
            assertEquals(rcs.get(mode).getGlobalCc(), new Composer(config, mapper).getRecipientConfig().getGlobalCc());
        });
    }

    /**
     * Insure that event types are properly mapped to notification types
     *  APPROVAL_REQUESTED_NEWUSER -> SUBMISSION_APPROVAL_INVITE
     *  APPROVAL_REQUESTED -> SUBMISSION_APPROVAL_REQUESTED
     *  CHANGES_REQUESTED -> SUBMISSION_CHANGES_REQUESTED
     *  SUBMITTED -> SUBMISSION_SUBMISSION_SUBMITTED
     *  CANCELLED -> SUBMISSION_SUBMISSION_CANCELLED
     */
    @Test
    public void testEventMappingToNotificationType() {
        HashMap<SubmissionEvent.EventType, Notification.Type> expectedMapping =
                new HashMap<SubmissionEvent.EventType, Notification.Type>() {
                    {
                        put(SubmissionEvent.EventType.APPROVAL_REQUESTED_NEWUSER,
                                Notification.Type.SUBMISSION_APPROVAL_INVITE);
                        put(SubmissionEvent.EventType.APPROVAL_REQUESTED,
                                Notification.Type.SUBMISSION_APPROVAL_REQUESTED);
                        put(SubmissionEvent.EventType.CHANGES_REQUESTED,
                                Notification.Type.SUBMISSION_CHANGES_REQUESTED);
                        put(SubmissionEvent.EventType.SUBMITTED,
                                Notification.Type.SUBMISSION_SUBMISSION_SUBMITTED);
                        put(SubmissionEvent.EventType.CANCELLED,
                                Notification.Type.SUBMISSION_SUBMISSION_CANCELLED);
                    }
                };

        Arrays.stream(SubmissionEvent.EventType.values()).forEach(eventType -> {
            SubmissionEvent event = new SubmissionEvent();
            event.setEventType(eventType);
            event.setId(URI.create(eventId));
            event.setSubmission(URI.create(submissionId));
            event.setPerformedBy(URI.create(submitter));
            event.setLink(eventLink);

            Submission submission = new Submission();
            submission.setId(URI.create(submissionId));
            submission.setPreparers(preparers.stream().map(URI::create).collect(Collectors.toList()));
            submission.setSubmitter(URI.create(submitter));

            if (SubmissionEvent.EventType.APPROVAL_REQUESTED_NEWUSER.equals(eventType)) {
                // from submitter URIs to submitter email+name
                submission.setSubmitter(null);
                submission.setSubmitterEmail(URI.create("mailto:nobody@example.org"));
                submission.setSubmitterName("moo!");
            }

            assertEquals(expectedMapping.get(eventType), composer.apply(submission, event).getType());
        });
    }

    /**
     * Insure that the {@link Notification#getParameters() parameters} are properly populated
     */
    @Test
    public void testNotificationParametersModel() throws IOException {
        String to = "mailto:emetsger@mail.local.domain";
        String from = "mailto:preparer@mail.local.domain";

        Submission submission = new Submission();

        submission.setSubmitter(null);

        submission.setSubmitterEmail(URI.create(to));
        URI preparerUri = URI.create(from);
        submission.setPreparers(singletonList(preparerUri));
        String metadata = resourceToString(join("/","", packageAsPath(), "submission-metadata.json"), forName("UTF-8"));
        submission.setMetadata(metadata);
        submission = passClient.createAndReadResource(submission, Submission.class);

        SubmissionEvent event = new SubmissionEvent();
        event.setSubmission(submission.getId());
        event.setEventType(SubmissionEvent.EventType.APPROVAL_REQUESTED_NEWUSER);
        event.setLink(eventLink);
        event.setPerformedBy(preparerUri);
        event.setComment("Please see if this submission meets your approval.");
        event = passClient.createAndReadResource(event, SubmissionEvent.class);

        Notification n = composer.apply(submission, event);
        Map<Notification.Param, String> params = n.getParameters();

        assertEquals(Composer.resourceMetadata(submission, mapper), params.get(RESOURCE_METADATA));
        // todo: Params map contains URIs of recipients at this point, they've not been resolved to email addresses
        // todo: Recipient URIs aren't resolved until Dispatch
        assertEquals(to, params.get(TO));
        assertEquals(getRecipientConfig(config).getFromAddress(), params.get(FROM));
        assertEquals(join(",", getRecipientConfig(config).getGlobalCc()), params.get(CC));
        assertEquals(Composer.eventMetadata(event, mapper), params.get(EVENT_METADATA));
        JsonNode eventMdNode = mapper.readTree(params.get(EVENT_METADATA));
        assertEquals(event.getId().toString(), Composer.field("id", eventMdNode).get());
        // todo: remove Subject?  Unset at this point, since templates haven't been resolved or parameterized
        assertNull(params.get(SUBJECT));

        String serializedLinks = params.get(LINKS);
        assertNotNull(serializedLinks);
        List<Link> deserializedLinks = new ArrayList(deserialize(serializedLinks));
        assertEquals(1,  deserializedLinks.size());
        assertEquals(SUBMISSION_REVIEW_INVITE, deserializedLinks.get(0).getRel());
        assertTrue(deserializedLinks.get(0).getHref().toString().contains(eventLink.toString()));
        assertNotSame(eventLink, deserializedLinks.get(0).getHref());

    }

    private static String packageAsPath() {
        return ComposerIT.class.getPackage().getName().replace('.', '/');
    }
}
