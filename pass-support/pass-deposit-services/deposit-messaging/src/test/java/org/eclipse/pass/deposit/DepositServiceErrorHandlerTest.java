/*
 * Copyright 2019 Johns Hopkins University
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
package org.eclipse.pass.deposit;

import static org.eclipse.pass.deposit.DepositMessagingTestUtil.randomId;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.function.Predicate;

import org.eclipse.pass.deposit.cri.CriticalRepositoryInteraction;
import org.eclipse.pass.support.client.model.AggregatedDepositStatus;
import org.eclipse.pass.support.client.model.Deposit;
import org.eclipse.pass.support.client.model.DepositStatus;
import org.eclipse.pass.support.client.model.Submission;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DepositServiceErrorHandlerTest {

    private CriticalRepositoryInteraction cri;

    private DepositServiceErrorHandler underTest;

    @BeforeEach
    public void setUp() throws Exception {
        cri = mock(CriticalRepositoryInteraction.class);
        underTest = new DepositServiceErrorHandler(cri);
    }

    /**
     * Error handler should mark the deposit as failed when handling a DepositServiceRuntimeException containing a
     * Deposit
     */
    @Test
    @SuppressWarnings("unchecked")
    public void handleDepositServiceReWithDeposit() {
        Deposit depositResource = new Deposit();
        depositResource.setId(randomId());
        DepositServiceRuntimeException dsreWithDeposit =
            new DepositServiceRuntimeException("handleDepositServiceReWithDeposit", depositResource);

        CriticalRepositoryInteraction.CriticalResult<?,?> cr = mock(CriticalRepositoryInteraction.CriticalResult.class);
        when(cr.success()).thenReturn(true);
        when(cri.performCritical(
            eq(depositResource.getId()), eq(Deposit.class), any(), any(Predicate.class), any()))
            .thenAnswer(inv -> {
                depositResource.setDepositStatus(DepositStatus.FAILED);
                return cr;
            });

        underTest.handleError(dsreWithDeposit);

        assertEquals(DepositStatus.FAILED, depositResource.getDepositStatus());
        verify(cri).performCritical(eq(depositResource.getId()), eq(Deposit.class), any(), any(Predicate.class), any());
    }

    /**
     * Error handler should mark the submission as failed when handling a DepositServiceRuntimeException containing a
     * Submission
     */
    @Test
    @SuppressWarnings("unchecked")
    public void handleDepositServiceReWithSubmission() {
        Submission submissionResource = new Submission();
        submissionResource.setId(randomId());
        DepositServiceRuntimeException depositServiceReWithSubmission =
            new DepositServiceRuntimeException("handleDepositServiceReWithSubmission", submissionResource);

        CriticalRepositoryInteraction.CriticalResult<?,?> cr = mock(CriticalRepositoryInteraction.CriticalResult.class);
        when(cr.success()).thenReturn(true);
        when(cri.performCritical(
            eq(submissionResource.getId()), eq(Submission.class), any(), any(Predicate.class), any()))
            .thenAnswer(inv -> {
                submissionResource.setAggregatedDepositStatus(AggregatedDepositStatus.FAILED);
                return cr;
            });

        underTest.handleError(depositServiceReWithSubmission);

        assertEquals(AggregatedDepositStatus.FAILED, submissionResource.getAggregatedDepositStatus());
        verify(cri).performCritical(eq(submissionResource.getId()), eq(Submission.class), any(), any(Predicate.class),
                                    any());
    }

    /**
     * The error handler doesn't do anything but log a message when handling a Throwable with a non-DSRE cause
     */
    @Test
    public void handleThrowable() {
        Throwable t = new Throwable("handleThrowable");

        underTest.handleError(t);

        verifyNoInteractions(cri);
    }

    /**
     * The error handler doesn't do anything but log a message when handling a RuntimeException with a non-DSRE cause
     */
    @Test
    public void handleRuntimeException() {
        RuntimeException re = new RuntimeException("handleRuntimeException");

        underTest.handleError(re);

        verifyNoInteractions(cri);
    }

    /**
     * The error handler should examine the cause of a RuntimeException with a DSRE cause and fail the underlying
     * resource
     */
    @Test
    @SuppressWarnings("unchecked")
    public void handleRteWithDsreCause() {
        Deposit depositResource = new Deposit();
        depositResource.setId(randomId());
        DepositServiceRuntimeException dsreWithDeposit =
            new DepositServiceRuntimeException("handleRteWithDsreCause", depositResource);
        RuntimeException re = new RuntimeException("handleRteWithDsreCause", dsreWithDeposit);

        CriticalRepositoryInteraction.CriticalResult<?,?> cr = mock(CriticalRepositoryInteraction.CriticalResult.class);
        when(cr.success()).thenReturn(true);
        when(cri.performCritical(eq(depositResource.getId()), eq(Deposit.class), any(), any(Predicate.class), any()))
            .thenAnswer(inv -> {
                depositResource.setDepositStatus(DepositStatus.FAILED);
                return cr;
            });

        underTest.handleError(re);

        assertEquals(DepositStatus.FAILED, depositResource.getDepositStatus());
        verify(cri).performCritical(eq(depositResource.getId()), eq(Deposit.class), any(), any(Predicate.class), any());
    }
}