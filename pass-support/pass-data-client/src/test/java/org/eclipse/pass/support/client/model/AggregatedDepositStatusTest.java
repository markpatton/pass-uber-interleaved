/*
 * Copyright 2023 Johns Hopkins University
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
package org.eclipse.pass.support.client.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Russ Poetker (rpoetke1@jh.edu)
 */
public class AggregatedDepositStatusTest {
    private static Stream<Arguments> provideStatuses() {
        return Stream.of(
            Arguments.of(AggregatedDepositStatus.ACCEPTED, true),
            Arguments.of(AggregatedDepositStatus.REJECTED, true),
            Arguments.of(AggregatedDepositStatus.FAILED, false),
            Arguments.of(AggregatedDepositStatus.NOT_STARTED, false),
            Arguments.of(AggregatedDepositStatus.IN_PROGRESS, false),
            Arguments.of(null, false)
        );
    }

    @ParameterizedTest
    @MethodSource("provideStatuses")
    void testTerminalStatus(AggregatedDepositStatus aggregatedDepositStatus, boolean expectedTerminalStatus) {
        boolean terminalStatus = AggregatedDepositStatus.isTerminalStatus(aggregatedDepositStatus);
        assertEquals(expectedTerminalStatus, terminalStatus);
    }
}
