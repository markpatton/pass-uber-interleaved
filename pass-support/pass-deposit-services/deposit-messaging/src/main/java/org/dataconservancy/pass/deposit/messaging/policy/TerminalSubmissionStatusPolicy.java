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
package org.dataconservancy.pass.deposit.messaging.policy;

import org.dataconservancy.pass.deposit.messaging.status.StatusEvaluator;
import org.eclipse.pass.support.client.model.AggregatedDepositStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Component
public class TerminalSubmissionStatusPolicy implements Policy<AggregatedDepositStatus> {

    private StatusEvaluator<AggregatedDepositStatus> submissionStatusEvaluator;

    @Autowired
    public TerminalSubmissionStatusPolicy(
        StatusEvaluator<AggregatedDepositStatus> submissionStatusEvaluator) {
        this.submissionStatusEvaluator = submissionStatusEvaluator;
    }

    /**
     * Return {@code true} if the supplied {@code status} is considered <em>terminal</em>.
     * <p>
     * <strong>N.B.</strong> {@code null} <em>is not</em> considered terminal.
     * </p>
     *
     * @param status the Submission status
     * @return true if the status is <em>terminal</em>
     */
    @Override
    public boolean test(AggregatedDepositStatus status) {
        return status != null && submissionStatusEvaluator.isTerminal(status);
    }

}
