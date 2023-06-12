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

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.InvalidPathException;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.pass.notification.config.NotificationTemplateName;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Slf4j
@Component
public class SpringUriTemplateResolver implements TemplateResolver {

    @Override
    public InputStream resolve(NotificationTemplateName name, String template) {

        try {
            URI.create(template);
        } catch (Exception e) {
            // not a URI.
            return null;
        }

        int semiColonIdx = template.indexOf(":");

        try {
            if (template.startsWith("classpath:") || template.startsWith("classpath*:")) {
                return new ClassPathResource(template.substring(semiColonIdx + 1)).getInputStream();
            }

            if (template.startsWith("file:") || template.startsWith("http:") || template.startsWith("https:")) {
                // todo: handle authenticated endpoints; cf. deposit services for impl
                return new UrlResource(template).getInputStream();
            }
        } catch (IOException e) {
            String msg = format("Error resolving template name '%s', '%s' as a Spring Resource: %s",
                    name, template, e.getMessage());
            throw new RuntimeException(msg, e);
        }

        // assume file

        try {
            return new FileSystemResource(template).getInputStream();
        } catch (IOException | InvalidPathException e) {
            log.debug("Unable to resolve template named '{}' (value: '{}') as a Spring Resource.", name, template);
        }

        return null;

    }
}
