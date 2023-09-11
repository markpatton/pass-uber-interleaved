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
package org.eclipse.pass.deposit.service;

import static org.eclipse.pass.deposit.DepositMessagingTestUtil.randomId;
import static org.eclipse.pass.deposit.DepositMessagingTestUtil.randomIntermediateDepositStatus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.abdera.i18n.iri.IRI;
import org.eclipse.pass.deposit.assembler.Assembler;
import org.eclipse.pass.deposit.assembler.PackageStream;
import org.eclipse.pass.deposit.cri.CriticalPath;
import org.eclipse.pass.deposit.cri.CriticalRepositoryInteraction;
import org.eclipse.pass.deposit.model.Packager;
import org.eclipse.pass.deposit.transport.Transport;
import org.eclipse.pass.deposit.transport.TransportResponse;
import org.eclipse.pass.deposit.transport.TransportSession;
import org.eclipse.pass.deposit.transport.sword2.Sword2DepositReceiptResponse;
import org.eclipse.pass.support.client.PassClient;
import org.eclipse.pass.support.client.model.AggregatedDepositStatus;
import org.eclipse.pass.support.client.model.Deposit;
import org.eclipse.pass.support.client.model.Repository;
import org.eclipse.pass.support.client.model.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.swordapp.client.DepositReceipt;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SwordIdentifier;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DepositTaskTest {

    private DepositUtil.DepositWorkerContext dc;
    private PassClient passClient;
    private DepositTask depositTask;

    @BeforeEach
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        DepositUtil.DepositWorkerContext dwc = new DepositUtil.DepositWorkerContext();
        dc = Mockito.spy(dwc);
        passClient = mock(PassClient.class);
        CriticalRepositoryInteraction cri = new CriticalPath(passClient);
        depositTask = new DepositTask(dc, passClient, cri);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void j10sStatementUrlHack() throws Exception {
        String prefix = "http://moo";
        String replacement = "http://foo";

        URI dspaceItemUri = URI.create("dspace:" + randomId());
        SwordIdentifier dspaceItem = mock(SwordIdentifier.class);
        when(dspaceItem.getHref()).thenReturn(dspaceItemUri.toString());
        SwordIdentifier swordStatement = identifierFor(prefix);

        DepositReceipt dr = mock(DepositReceipt.class);
        Sword2DepositReceiptResponse tr = new Sword2DepositReceiptResponse(dr);
        when(dr.getStatusCode()).thenReturn(200);
        when(dr.getSplashPageLink()).thenReturn(dspaceItem);
        when(dr.getAtomStatementLink()).thenReturn(swordStatement);

        Deposit d = depositContext(dc, tr, passClient);

        depositTask.setSwordSleepTimeMs(1); // move things along...
        depositTask.setReplacementPrefix(replacement);
        depositTask.setPrefixToMatch(prefix);

        depositTask.executeDeposit();

        assertEquals(replacement, d.getDepositStatusRef());
    }

    @Test
    public void j10sStatementUrlHackWithNullValues() throws Exception {
        String prefix = "http://moo";
        String replacement = null;

        URI dspaceItemUri = URI.create("dspace:" + randomId());
        SwordIdentifier dspaceItem = mock(SwordIdentifier.class);
        when(dspaceItem.getHref()).thenReturn(dspaceItemUri.toString());
        SwordIdentifier swordStatement = identifierFor(prefix);

        DepositReceipt dr = mock(DepositReceipt.class);
        Sword2DepositReceiptResponse tr = new Sword2DepositReceiptResponse(dr);
        when(dr.getStatusCode()).thenReturn(200);
        when(dr.getSplashPageLink()).thenReturn(dspaceItem);
        when(dr.getAtomStatementLink()).thenReturn(swordStatement);

        Deposit d = depositContext(dc, tr, passClient);

        depositTask.setSwordSleepTimeMs(1); // move things along...
        depositTask.setReplacementPrefix(replacement);
        depositTask.setPrefixToMatch(prefix);

        depositTask.executeDeposit();

        assertEquals(prefix, d.getDepositStatusRef());
    }

    @Test
    public void j10sStatementUrlHackWithNonMatchingValues() throws Exception {
        String href = "http://baz";
        String prefix = "http://moo";
        String replacement = "http://foo";

        URI dspaceItemUri = URI.create("dspace:" + randomId());
        SwordIdentifier dspaceItem = mock(SwordIdentifier.class);
        when(dspaceItem.getHref()).thenReturn(dspaceItemUri.toString());
        SwordIdentifier swordStatement = identifierFor(href);

        DepositReceipt dr = mock(DepositReceipt.class);
        Sword2DepositReceiptResponse tr = new Sword2DepositReceiptResponse(dr);
        when(dr.getStatusCode()).thenReturn(200);
        when(dr.getSplashPageLink()).thenReturn(dspaceItem);
        when(dr.getAtomStatementLink()).thenReturn(swordStatement);

        Deposit d = depositContext(dc, tr, passClient);

        depositTask.setSwordSleepTimeMs(1); // move things along...
        depositTask.setReplacementPrefix(replacement);
        depositTask.setPrefixToMatch(prefix);

        depositTask.executeDeposit();

        assertEquals(href, d.getDepositStatusRef());
    }

    /**
     * Populates the supplied {@code depositContext} with a {@code Repository}, {@code Submission} and
     * {@code Deposit}.
     *
     * @param depositContext
     * @return
     */
    private static Deposit depositContext(DepositUtil.DepositWorkerContext depositContext, TransportResponse tr,
                                          PassClient passClient) throws IOException {
        Repository r = new Repository();
        r.setId(randomId());
        depositContext.repository(r);

        Submission s = new Submission();
        s.setId(randomId());
        s.setAggregatedDepositStatus(AggregatedDepositStatus.IN_PROGRESS);
        depositContext.submission(s);

        Deposit d = new Deposit();
        d.setId(randomId());
        d.setSubmission(s);
        d.setDepositStatus(randomIntermediateDepositStatus.get());
        depositContext.deposit(d);

        when(passClient.getObject(d)).thenReturn(d);
        when(passClient.getObject(Deposit.class, d.getId())).thenReturn(d);

        Assembler assembler = mock(Assembler.class);
        PackageStream stream = mock(PackageStream.class);
        Packager packager = mock(Packager.class);
        Transport transport = mock(Transport.class);
        TransportSession session = mock(TransportSession.class);

        Map<String, String> packagerConfig = new HashMap<>();
        when(packager.getAssembler()).thenReturn(assembler);
        when(packager.getConfiguration()).thenReturn(packagerConfig);
        when(assembler.assemble(any(), anyMap())).thenReturn(stream);
        when(packager.getTransport()).thenReturn(transport);
        when(transport.open(anyMap())).thenReturn(session);
        when(session.send(eq(stream), any())).thenReturn(tr);

        when(depositContext.packager()).thenReturn(packager);

        return d;
    }

    /**
     * Answers a mock {@link SwordIdentifier} that returns an {@code IRI} from the supplied {@code href}
     *
     * @param href
     * @return
     * @throws SWORDClientException
     */
    private static SwordIdentifier identifierFor(String href) throws SWORDClientException {
        SwordIdentifier identifier = mock(SwordIdentifier.class);
        when(identifier.getIRI()).thenReturn(new IRI(href));
        return identifier;
    }
}