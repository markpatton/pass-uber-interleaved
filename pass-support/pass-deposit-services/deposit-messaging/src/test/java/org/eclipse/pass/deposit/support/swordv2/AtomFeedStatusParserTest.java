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
package org.eclipse.pass.deposit.support.swordv2;

import static org.eclipse.pass.deposit.status.SwordDspaceDepositStatus.SWORD_STATE_ARCHIVED;
import static org.eclipse.pass.deposit.status.SwordDspaceDepositStatus.SWORD_STATE_INPROGRESS;
import static org.eclipse.pass.deposit.status.SwordDspaceDepositStatus.SWORD_STATE_INREVIEW;
import static org.eclipse.pass.deposit.status.SwordDspaceDepositStatus.SWORD_STATE_WITHDRAWN;
import static org.eclipse.pass.deposit.support.swordv2.AtomResources.ARCHIVED_STATUS_RESOURCE;
import static org.eclipse.pass.deposit.support.swordv2.AtomResources.INPROGRESS_STATUS_RESOURCE;
import static org.eclipse.pass.deposit.support.swordv2.AtomResources.INREVIEW_STATUS_RESOURCE;
import static org.eclipse.pass.deposit.support.swordv2.AtomResources.MISSING_STATUS_RESOURCE;
import static org.eclipse.pass.deposit.support.swordv2.AtomResources.MULTIPLE_STATUS_RESOURCE;
import static org.eclipse.pass.deposit.support.swordv2.AtomResources.UNKNOWN_STATUS_RESOURCE;
import static org.eclipse.pass.deposit.support.swordv2.AtomResources.WITHDRAWN_STATUS_RESOURCE;
import static org.eclipse.pass.deposit.util.ResourceTestUtil.findByName;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.URI;

import org.apache.abdera.model.Document;
import org.apache.abdera.model.Feed;
import org.apache.abdera.parser.Parser;
import org.eclipse.pass.deposit.config.repository.RepositoryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class AtomFeedStatusParserTest {
    private Parser abderaParser;

    private RepositoryConfig repositoryConfig;

    private AtomFeedStatusResolver underTest;

    private ResourceResolver resourceResolver;

    @BeforeEach
    public void setUp() throws Exception {
        abderaParser = mock(Parser.class);
        repositoryConfig = mock(RepositoryConfig.class);
        resourceResolver = mock(ResourceResolver.class);
        underTest = new AtomFeedStatusResolver(abderaParser, resourceResolver);
    }

    // Test cases
    //   - Deposit with malformed status ref (can't, cause method requires URI)
    //   - Deposit with status ref that doesn't exist (test of AbderaClient)
    //   - Deposit with status ref that times out (test of AbderaClient)
    //   - Deposit status is mapped to a non-terminal status (should be left alone)
    //   - Deposit status is mapped to null
    //   - Deposit with AbderaClient that throws an exception
    //   - Parse a Document<Feed> with missing or incorrect Category
    //   - Parse a Document<Feed> with correct Category but unknown value


    /**
     * An Atom Statement containing a <sword:state> of http://dspace.org/state/archived should be parsed
     *
     * @throws Exception
     */
    @Test
    public void mapArchived() throws Exception {
        Document<Feed> feed = AtomTestUtil.parseFeed(findByName(ARCHIVED_STATUS_RESOURCE, AtomResources.class));
        assertEquals(SWORD_STATE_ARCHIVED.asUri(), AtomUtil.parseSwordState(feed));
    }

    @Test
    public void mapInProgress() throws Exception {
        Document<Feed> feed = AtomTestUtil.parseFeed(findByName(INPROGRESS_STATUS_RESOURCE, AtomResources.class));
        assertEquals(SWORD_STATE_INPROGRESS.asUri(), AtomUtil.parseSwordState(feed));
    }

    @Test
    public void mapInReview() throws Exception {
        Document<Feed> feed = AtomTestUtil.parseFeed(findByName(INREVIEW_STATUS_RESOURCE, AtomResources.class));
        assertEquals(SWORD_STATE_INREVIEW.asUri(), AtomUtil.parseSwordState(feed));
    }

    @Test
    public void mapMissing() throws Exception {
        Document<Feed> feed = AtomTestUtil.parseFeed(findByName(MISSING_STATUS_RESOURCE, AtomResources.class));
        assertEquals(null, AtomUtil.parseSwordState(feed));
    }

    @Test
    public void mapMultiple() throws Exception {
        Document<Feed> feed = AtomTestUtil.parseFeed(findByName(MULTIPLE_STATUS_RESOURCE, AtomResources.class));
        assertEquals(SWORD_STATE_ARCHIVED.asUri(), AtomUtil.parseSwordState(feed));
    }

    @Test
    public void mapUnknown() throws Exception {
        Document<Feed> feed = AtomTestUtil.parseFeed(findByName(UNKNOWN_STATUS_RESOURCE, AtomResources.class));
        assertEquals(URI.create("http://dspace.org/state/moo"), AtomUtil.parseSwordState(feed));
    }

    @Test
    public void mapWithdrawn() throws Exception {
        Document<Feed> feed = AtomTestUtil.parseFeed(findByName(WITHDRAWN_STATUS_RESOURCE, AtomResources.class));
        assertEquals(SWORD_STATE_WITHDRAWN.asUri(), AtomUtil.parseSwordState(feed));
    }

    @Test
    public void parseWithRuntimeException() throws Exception {
        URI uri = URI.create("file:testing");

        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenReturn(mock(InputStream.class));

        RuntimeException expected = new RuntimeException("Expected exception.");

        when(resourceResolver.resolve(eq(uri), any(RepositoryConfig.class))).thenReturn(resource);
        when(abderaParser.parse(any(InputStream.class))).thenThrow(expected);

        Exception e = assertThrows(RuntimeException.class, () -> {
            underTest.resolve(uri, repositoryConfig);
        });

        assertTrue(e.getMessage().contains("Expected exception."));
    }

//    @Test
//    public void parseWithParseException() throws Exception {
//        URI uri = findUriByName(ARCHIVED_STATUS_RESOURCE, AtomResources.class);
//
//        Resource resource = mock(Resource.class);
//        when(resource.getInputStream()).thenReturn(mock(InputStream.class));
//
//        ParseException expectedCause = new ParseException("Expected cause.");
//        expectedException.expect(RuntimeException.class);
//        expectedException.expectCause(is(expectedCause));
//        expectedException.expectMessage("Expected cause.");
//        expectedException.expectMessage("AtomStatusParser-archived.xml");
//
//        when(resourceResolver.resolve(eq(uri), any(RepositoryConfig.class))).thenReturn(resource);
//        when(abderaParser.parse(any(InputStream.class))).thenThrow(expectedCause);
//
//        underTest.resolve(uri, repositoryConfig);
//    }
}