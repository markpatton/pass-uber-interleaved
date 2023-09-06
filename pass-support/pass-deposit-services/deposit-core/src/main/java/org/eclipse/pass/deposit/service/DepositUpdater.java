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

import java.io.IOException;

import org.eclipse.pass.support.client.PassClient;
import org.eclipse.pass.support.client.PassClientSelector;
import org.eclipse.pass.support.client.RSQL;
import org.eclipse.pass.support.client.model.Deposit;
import org.eclipse.pass.support.client.model.DepositStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DepositUpdater {

    private static final Logger LOG = LoggerFactory.getLogger(DepositUpdater.class);

    private final PassClient passClient;
    private final DepositTaskHelper depositHelper;
    private final FailedDepositRetry failedDepositRetry;

    @Autowired
    public DepositUpdater(PassClient passClient, DepositTaskHelper depositHelper,
                          FailedDepositRetry failedDepositRetry) {
        this.passClient = passClient;
        this.depositHelper = depositHelper;
        this.failedDepositRetry = failedDepositRetry;
    }

    public void doUpdate() throws IOException {
        PassClientSelector<Deposit> sel = new PassClientSelector<>(Deposit.class);
        sel.setFilter(RSQL.in("depositStatus", DepositStatus.SUBMITTED.getValue(), DepositStatus.FAILED.getValue()));
        passClient.streamObjects(sel).forEach(deposit -> {
            try {
                if (deposit.getDepositStatus() == DepositStatus.FAILED) {
                    failedDepositRetry.retryFailedDeposit(deposit);
                } else {
                    depositHelper.processDepositStatus(deposit.getId());
                }
            } catch (Exception e) {
                LOG.warn("Failed to update {}: {}", deposit.getId(), e.getMessage(), e);
            }
        });
    }
}
