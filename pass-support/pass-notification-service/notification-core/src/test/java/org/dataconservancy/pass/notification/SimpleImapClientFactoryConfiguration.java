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
package org.dataconservancy.pass.notification;

import java.util.Properties;
import javax.mail.MessagingException;
import javax.mail.Session;

import com.sun.mail.imap.IMAPStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Configuration
public class SimpleImapClientFactoryConfiguration {

    @Value("${mail.imap.user}")
    private String imapUser;

    @Value("${mail.imap.password}")
    private String imapPass;

    @Value("${mail.imap.host}")
    private String imapHost;

    @Value("${mail.imap.port}")
    private String imapPort;

    @Value("${mail.imap.ssl.enable}")
    private boolean useSsl;

    @Value("${mail.imap.ssl.trust}")
    private String sslTrust;

    @Value("${mail.imap.starttls.enable}")
    private boolean enableTlsIfSupported;

    @Value("${mail.imap.finalizecleanclose}")
    private boolean closeOnFinalize;

    @Value("${mail.imap.connectiontimeout}")
    private int connectTimeout;

    @Value("${mail.imap.timeout}")
    private int timeout;

    @Bean
    public Session mailSession() {
        return Session.getDefaultInstance(new Properties() {
            {
                put("mail.imap.host", imapHost);
                put("mail.imap.port", imapPort);
                put("mail.imap.ssl.enable", useSsl);
                put("mail.imap.ssl.trust", sslTrust);
                put("mail.imap.starttls.enable", enableTlsIfSupported);
                put("mail.imap.finalizecleanclose", closeOnFinalize);
                put("mail.imap.connectiontimeout", connectTimeout);
                put("mail.imap.timeout", timeout);
            }
        });
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public IMAPStore imapStore(Session session) {
        try {
            return (IMAPStore) session.getStore("imap");
        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }

}
