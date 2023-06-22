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
package org.eclipse.pass.deposit.messaging.status;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.pass.support.client.model.DepositStatus;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public class DepositStatusEvaluatorTest {

    private DepositStatusEvaluator underTest;

    @Before
    public void setUp() throws Exception {
        underTest = new DepositStatusEvaluator();
    }

    @Test
    public void testIsTerminal() throws Exception {
        assertTrue(underTest.isTerminal(DepositStatus.ACCEPTED));
        assertTrue(underTest.isTerminal(DepositStatus.REJECTED));
        assertFalse(underTest.isTerminal(DepositStatus.SUBMITTED));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullStatus() throws Exception {
        underTest.isTerminal(null);
    }

}