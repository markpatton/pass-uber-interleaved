/*
 * Copyright 2018 Johns Hopkins University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.eclipse.pass.notification.dispatch.email;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;
import javax.mail.Message.RecipientType;

import org.apache.commons.io.IOUtils;
import org.eclipse.pass.notification.model.NotificationTemplateName;
import org.eclipse.pass.notification.model.NotificationParam;
import org.eclipse.pass.support.client.PassClient;
import org.eclipse.pass.support.client.model.User;
import org.eclipse.pass.notification.dispatch.DispatchException;
import org.eclipse.pass.notification.model.Notification;
import org.eclipse.pass.notification.config.NotificationConfig;
import org.eclipse.pass.notification.model.NotificationTemplate;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.Recipient;
import org.simplejavamail.mailer.Mailer;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class EmailDispatchImplTest {

//    private Notification notification;
//
//    private NotificationConfig config;
//
//    private NotificationTemplate templateProto;
//
//    private CompositeResolver templateResolver;
//
//    private HandlebarsParameterizer templateParameterizer;
//
//    private Mailer mailer;
//
//    private PassClient passClient;
//
//    private String mailtoUri = "mailto:John%20Doe%3Cjohndoe%40example.org%3E";
//
//    private User user;
//
//    private String userUri = "https://pass.jhu.edu/fcrepo/users/123456";
//
//    private String userEmail = "user@bar.com";
//
//    private String from = "pass-noreply@jhu.edu";
//
//    private String cc = "pass-prod-cc@jhu.edu,pass-admin@jhu.edu";
//
//    private String metadata = "" +
//            "[\n" +
//            "  {\n" +
//            "    \"a sample metadata\": \"blob\",\n" +
//            "    \"for\": \"a Submission\"\n" +
//            "  }\n" +
//            "]";
//
//    private EmailDispatchImpl underTest;
//
//    @Before
//    @SuppressWarnings("unchecked")
//    public void setUp() throws Exception {
//        notification = mock(Notification.class);
//        config = mock(NotificationConfig.class);
//        templateProto = mock(NotificationTemplate.class);
//        templateResolver = mock(CompositeResolver.class);
//        passClient = mock(PassClient.class);
//        user = mock(User.class);
//        mailer = mock(Mailer.class);
//        templateParameterizer = mock(HandlebarsParameterizer.class);
//
//        when(config.getTemplates()).thenReturn(Collections.singletonList(templateProto));
//        when(templateProto.getTemplates()).thenReturn(new HashMap<NotificationParam, String>() {
//            {
//                put(NotificationParam.SUBJECT, "A Subject");
//                put(Name.BODY, "A Body");
//                put(Name.FOOTER, "A Footer");
//            }
//        });
//        when(user.getId()).thenReturn(URI.create(userUri));
//        when(user.getEmail()).thenReturn(userEmail);
//        when(passClient.readResource(URI.create(userUri), User.class)).thenReturn(user);
//
//        Parameterizer parameterizer = new Parameterizer(config, templateResolver, templateParameterizer);
//
//        // mock a whitelist that accepts all recipients by simply returning the collection of recipients it was provided
//        Function<Collection<String>, Collection<String>> whitelist = mock(Function.class);
//        when(whitelist.apply(any())).thenAnswer(inv -> inv.getArgument(0));
//        EmailComposer composer = new EmailComposer(passClient, whitelist);
//
//        underTest = new EmailDispatchImpl(parameterizer, mailer, composer);
//    }
//
//    @Test
//    public void simpleSuccess() throws IOException {
//        when(notification.getType()).thenReturn(Notification.Type.SUBMISSION_APPROVAL_INVITE);
//        when(notification.getParameters()).thenReturn(
//                new HashMap<NotificationParam, String>() {
//                    {
//                        put(NotificationParam.FROM, from);
//                        put(NotificationParam.TO, userUri);
//                        put(NotificationParam.CC, cc);
//                        put(NotificationParam.RESOURCE_METADATA, metadata);
//                    }
//                });
//        when(notification.getRecipients()).thenReturn(Collections.singleton(userUri));
//        when(notification.getSender()).thenReturn(from);
//        when(notification.getCc()).thenReturn(Collections.singleton(cc));
//
//        when(templateProto.getNotificationType()).thenReturn(Notification.Type.SUBMISSION_APPROVAL_INVITE);
//        when(templateResolver.resolve(any(), any())).thenAnswer(inv ->
//                IOUtils.toInputStream(inv.getArgument(1), "UTF-8"));
//
//        when(templateParameterizer.parameterize(any(), any(), any())).thenAnswer(inv -> {
//            NotificationTemplateName name = inv.getArgument(0);
//            switch (name) {
//                case SUBJECT:
//                    return "A Subject";
//                case FOOTER:
//                    return "A Footer";
//                case BODY:
//                    return "A Body";
//                default:
//            }
//
//            throw new RuntimeException("Unknown template name '" + name + "'");
//        });
//
//        underTest.dispatch(notification);
//
//        ArgumentCaptor<Email> emailCaptor = ArgumentCaptor.forClass(Email.class);
//
//        verify(mailer).sendMail(emailCaptor.capture());
//
//        Email email = emailCaptor.getValue();
//
//        // Verify the email was composed properly
//        assertEquals(from, email.getFromRecipient().getAddress());
//
//        assertTrue(email.getRecipients()
//                .stream()
//                .filter(recipient -> recipient.getType() == RecipientType.TO)
//                .anyMatch(recipient -> recipient.getAddress().equals(userEmail)));
//
//        email.getRecipients()
//            .stream()
//            .filter(recipient -> recipient.getType() == RecipientType.CC)
//            .map(Recipient::getAddress)
//            .forEach(address ->
//                    assertTrue("Missing expected CC address '" + address + "' from CC recipients",
//                            cc.contains(address)));
//
//        assertEquals("A Subject", email.getSubject());
//        assertEquals(String.join("\n\n", "A Body", "A Footer"), email.getPlainText());
//    }
//
//    /**
//     * A nice DispatchException should be thrown if the To field of the email is missing or empty
//     */
//    @Test
//    public void emptyToAddress() {
//        Notification notification = mock(Notification.class);
//        Parameterizer p = mock(Parameterizer.class);
//        EmailComposer c = mock(EmailComposer.class);
//        Email e = mock(Email.class);
//
//        when(notification.getResourceUri()).thenReturn(URI.create(UUID.randomUUID().toString()));
//        when(notification.getEventUri()).thenReturn(URI.create(UUID.randomUUID().toString()));
//
//        when(p.resolveAndParameterize(any(), any())).thenReturn(Collections.emptyMap());
//        when(c.compose(any(), any())).thenReturn(e);
//        when(e.getRecipients()).thenReturn(Collections.emptyList());
//
//        try {
//            underTest = new EmailDispatchImpl(p, mailer, c);
//            underTest.dispatch(notification);
//            fail("Expected Dispatch Exception");
//        } catch (DispatchException expected) {
//            assertTrue(expected.getMessage().contains("dispatch email with an empty To: address"));
//        }
//
//        verifyZeroInteractions(mailer);
//    }

}