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
package org.eclipse.pass.deposit.policy;

import org.eclipse.pass.deposit.status.StatusEvaluator;
import org.eclipse.pass.support.client.model.DepositStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Accepts {@code DepositStatus} that represents an <em>intermediate</em> deposit status.  A {@code null} {@code
 * DepositStatus} is considered <em>intermediate</em>.
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
@Component
public class IntermediateDepositStatusPolicy implements Policy<DepositStatus> {

    private StatusEvaluator<DepositStatus> statusEvaluator;

    @Autowired
    public IntermediateDepositStatusPolicy(StatusEvaluator<DepositStatus> statusEvaluator) {
        this.statusEvaluator = statusEvaluator;
    }

    @Override
    public boolean test(DepositStatus o) {
        return o == null || !statusEvaluator.isTerminal(o);
    }
}
