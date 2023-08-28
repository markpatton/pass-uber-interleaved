/*
 * Copyright 2017 Johns Hopkins University
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

package org.eclipse.pass.loader.journal.nih;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.InputStream;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.pass.support.client.PassClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main method for nih loader executable.
 *
 * @author apb@jhu.edu
 */
public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private Main () {
        //never called
    }

    /**
     * Entry point for the command line tool.
     *
     * @param args the command line arguments
     * @throws Exception on error
     */
    public static void main(String[] args) throws Exception {

        LogUtil.adjustLogLevels();

        PassClient client = PassClient.newInstance();

        final JournalFinder finder = new BatchJournalFinder(client);

        try (final LoaderEngine loader = new LoaderEngine(client, finder)) {

            if (System.getProperty("dryRun", null) != null) {
                loader.setDryRun(true);
            }

            final String pmcUrl = System.getProperty("pmc", null);
            if (StringUtils.isNotEmpty(pmcUrl)) {
                LOG.info("Loading pcm data");

                final NihTypeAReader reader = new NihTypeAReader();

                try (InputStream file = new URL(pmcUrl).openStream()) {
                    loader.load(reader.readJournals(file, UTF_8), reader.hasPmcParticipation());
                }
            }

            final String medlineUrl = System.getProperty("medline", null);
            if (StringUtils.isNotEmpty(medlineUrl)) {
                LOG.info("Loading medline data");

                final MedlineReader reader = new MedlineReader();

                try (InputStream file = new URL(medlineUrl).openStream()) {
                    loader.load(reader.readJournals(file, UTF_8), reader.hasPmcParticipation());
                }
            }
        }
        LOG.info("done!");
    }
}
