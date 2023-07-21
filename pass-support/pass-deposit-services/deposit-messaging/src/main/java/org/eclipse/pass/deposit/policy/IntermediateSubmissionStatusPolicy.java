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
package org.eclipse.pass.deposit.policy;

import org.eclipse.pass.deposit.status.StatusEvaluator;
import org.eclipse.pass.support.client.model.AggregatedDepositStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Component
public class IntermediateSubmissionStatusPolicy implements Policy<AggregatedDepositStatus> {

    private StatusEvaluator<AggregatedDepositStatus> submissionStatusEvaluator;

    @Autowired
    public IntermediateSubmissionStatusPolicy(
        StatusEvaluator<AggregatedDepositStatus> submissionStatusEvaluator) {
        this.submissionStatusEvaluator = submissionStatusEvaluator;
    }

    /**
     * Return {@code true} if the supplied {@code status} is considered <em>intermediate</em>.
     * <p>
     * <strong>N.B.</strong> {@code null} <em>is</em> considered intermediate.
     * </p>
     *
     * @param status the Submission status
     * @return true if the status is <em>intermediate</em>
     */
    @Override
    public boolean test(AggregatedDepositStatus status) {
        return status == null || !submissionStatusEvaluator.isTerminal(status);
    }

}