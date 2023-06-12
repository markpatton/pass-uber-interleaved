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
package org.eclipse.pass.notification.app.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.github.jknack.handlebars.EscapingStrategy;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.helper.ConditionalHelpers;
import org.eclipse.pass.client.PassClient;
import org.eclipse.pass.client.PassClientDefault;
import org.eclipse.pass.notification.dispatch.DispatchService;
import org.eclipse.pass.notification.dispatch.impl.email.CompositeResolver;
import org.eclipse.pass.notification.dispatch.impl.email.EmailComposer;
import org.eclipse.pass.notification.dispatch.impl.email.EmailDispatchImpl;
import org.eclipse.pass.notification.dispatch.impl.email.HandlebarsParameterizer;
import org.eclipse.pass.notification.dispatch.impl.email.InlineTemplateResolver;
import org.eclipse.pass.notification.dispatch.impl.email.Parameterizer;
import org.eclipse.pass.notification.dispatch.impl.email.SimpleWhitelist;
import org.eclipse.pass.notification.dispatch.impl.email.SpringUriTemplateResolver;
import org.eclipse.pass.notification.dispatch.impl.email.TemplateParameterizer;
import org.eclipse.pass.notification.dispatch.impl.email.TemplateResolver;
import org.eclipse.pass.notification.impl.Composer;
import org.eclipse.pass.notification.impl.DefaultNotificationService;
import org.eclipse.pass.notification.impl.LinkValidator;
import org.eclipse.pass.notification.impl.RecipientAnalyzer;
import org.eclipse.pass.notification.impl.SubmissionLinkAnalyzer;
import org.eclipse.pass.notification.impl.UserTokenGenerator;
import org.eclipse.pass.notification.model.config.Mode;
import org.eclipse.pass.notification.model.config.NotificationConfig;
import org.eclipse.pass.notification.model.config.RecipientConfig;
import org.eclipse.pass.notification.model.config.smtp.SmtpServerConfig;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;

/**
 * Primary Spring Boot configuration class for Notification Services
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Configuration
public class SpringBootNotificationConfig {

    private static final Logger LOG = LoggerFactory.getLogger(SpringBootNotificationConfig.class);

    @Value("${pass.fedora.user}")
    private String fedoraUser;

    @Value("${pass.fedora.password}")
    private String fedoraPass;

    @Value("${pass.fedora.baseurl}")
    private String fedoraBaseUrl;

    @Value("${pass.elasticsearch.url}")
    private String esUrl;

    @Value("${pass.elasticsearch.limit}")
    private int esLimit;

    @Value("${pass.notification.http.agent}")
    private String passHttpAgent;

    @Value("${pass.notification.configuration}")
    private Resource notificationConfiguration;

    @Value("${pass.notification.mailer.debug}")
    private boolean mailerDebug;

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ObjectMapper springEnvObjectMapper(Environment env) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();

        SpringEnvStringDeserializer envDeserializer = new SpringEnvStringDeserializer(env);
        SpringEnvModeDeserializer enumDeserializer = new SpringEnvModeDeserializer(env);

        module.addDeserializer(String.class, envDeserializer);

        module.addDeserializer(Mode.class, enumDeserializer);
        mapper.registerModule(module);

        return mapper;
    }

    @Bean
    public NotificationConfig notificationConfig(ObjectMapper springEnvObjectMapper) throws IOException {
        return springEnvObjectMapper
                .readValue(notificationConfiguration.getInputStream(), NotificationConfig.class);
    }

    @Bean
    public PassClientDefault passClient() {

        // PassClientDefault can't be injected with configuration; requires system properties be set.
        // If a system property is already set, allow it to override what is resolved by the Spring environment.
        if (!System.getProperties().containsKey("pass.fedora.user")) {
            System.setProperty("pass.fedora.user", fedoraUser);
        }

        if (!System.getProperties().containsKey("pass.fedora.password")) {
            System.setProperty("pass.fedora.password", fedoraPass);
        }

        if (!System.getProperties().containsKey("pass.fedora.baseurl")) {
            System.setProperty("pass.fedora.baseurl", fedoraBaseUrl);
        }

        if (!System.getProperties().containsKey("pass.elasticsearch.url")) {
            System.setProperty("pass.elasticsearch.url", esUrl);
        }

        if (!System.getProperties().containsKey("pass.elasticsearch.limit")) {
            System.setProperty("pass.elasticsearch.limit", String.valueOf(esLimit));
        }

        if (!System.getProperties().containsKey("http.agent")) {
            System.setProperty("http.agent", passHttpAgent);
        }

        return new PassClientDefault();
    }

    @Bean
    public EmailComposer emailComposer(PassClient passClient, SimpleWhitelist whitelist) {
        return new EmailComposer(passClient, whitelist);
    }

    @Bean
    public Parameterizer parameterizer(NotificationConfig config,
                                       TemplateResolver compositeResolver,
                                       TemplateParameterizer templateParameterizer) {
        return new Parameterizer(config, compositeResolver, templateParameterizer);
    }

    @Bean
    public EmailDispatchImpl emailDispatchService(Parameterizer parameterizer,
                                                  Mailer mailer,
                                                  EmailComposer emailComposer) {
        return new EmailDispatchImpl(parameterizer, mailer, emailComposer);
    }

    @Bean
    public InlineTemplateResolver inlineTemplateResolver() {
        return new InlineTemplateResolver();
    }

    @Bean
    public SpringUriTemplateResolver springUriTemplateResolver() {
        return new SpringUriTemplateResolver();
    }

    @Bean
    public CompositeResolver compositeResolver(InlineTemplateResolver inlineTemplateResolver,
                                               SpringUriTemplateResolver springUriTemplateResolver) {
        return new CompositeResolver(Arrays.asList(springUriTemplateResolver, inlineTemplateResolver));
    }

    @Bean
    public HandlebarsParameterizer handlebarsParameterizer(Handlebars handlebars, ObjectMapper objectMapper) {
        return new HandlebarsParameterizer(handlebars, objectMapper);
    }

    @Bean
    public Handlebars handlebars() {
        Handlebars handlebars = new Handlebars();
        handlebars.registerHelper("eq", ConditionalHelpers.eq);
        return handlebars.with(EscapingStrategy.NOOP);
    }

    @Bean
    public Mailer mailer(NotificationConfig config) {
        SmtpServerConfig smtpConfig = config.getSmtpConfig();
        Objects.requireNonNull(smtpConfig,
                               "Missing SMTP server configuration from '" + notificationConfiguration + "'");
        MailerBuilder.MailerRegularBuilder builder = MailerBuilder
                .withSMTPServerHost(smtpConfig.getHost())
                .withSMTPServerPort(Integer.parseInt(smtpConfig.getPort()))
                .withTransportStrategy(TransportStrategy.valueOf(smtpConfig.getSmtpTransport().toUpperCase()))
                .withDebugLogging(mailerDebug);

        if (smtpConfig.getSmtpUser() != null && smtpConfig.getSmtpUser().trim().length() > 0) {
            builder = builder.withSMTPServerUsername(smtpConfig.getSmtpUser())
                    .withSMTPServerPassword(smtpConfig.getSmtpPassword());
        }

        return builder.buildMailer();
    }

    @Bean
    public SimpleWhitelist simpleWhitelist(NotificationConfig config) {
        RecipientConfig recipientConfig = config.getRecipientConfigs().stream()
                .filter(rc -> config.getMode() == rc.getMode()).findAny()
                .orElseThrow(() ->
                        new RuntimeException("Missing recipient configuration for Mode '" + config.getMode() + "'"));

        return new SimpleWhitelist(recipientConfig);
    }

    @Bean
    public RecipientAnalyzer recipientAnalyzer(Function<Collection<String>, Collection<String>> simpleWhitelist) {
        return new RecipientAnalyzer();
    }

    @Bean
    public UserTokenGenerator userTokenGenerator(NotificationConfig config) {
        return new UserTokenGenerator(config);
    }

    @Bean
    public SubmissionLinkAnalyzer submissionLinkAnalyzer(UserTokenGenerator generator) {
        return new SubmissionLinkAnalyzer(generator);
    }

    @Bean
    public LinkValidator linkValidator(NotificationConfig config) {
        return new LinkValidator(config);
    }

    @Bean
    public Composer composer(NotificationConfig notificationConfig, RecipientAnalyzer recipientAnalyzer,
            SubmissionLinkAnalyzer sla, LinkValidator lv, ObjectMapper objectMapper) {
        return new Composer(notificationConfig, recipientAnalyzer, sla, lv, objectMapper);
    }

    @Bean
    public DefaultNotificationService notificationService(
            PassClient passClient,
            Composer composer,
            DispatchService dispatchService) {
        return new DefaultNotificationService(passClient, dispatchService, composer);
    }
}
