/*
 *
 *  * Copyright 2018 Johns Hopkins University
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.eclipse.pass.deposit.cri;

import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.eclipse.pass.support.client.model.PassEntity;

/**
 * Insures that a series of interactions with the repository for a given {@code PassEntity} are not interleaved within
 * the scope of the running JVM.  This means that code executed within a {@code CriticalRepositoryInteraction} should
 * never observe changes to the same {@code PassEntity} resulting from another thread.
 * <p>
 * Interactions with the repository are <em>not</em> atomic or transactional.  {@code CriticalRepositoryInteraction}
 * does what it can to insure safe, concurrent, interaction with repository resources, but repository resources can be
 * modified at any time by agents outside the scope of the running JVM.
 * </p>
 * <p>
 * Clients of this interface must understand that while the boilerplate for interacting with the repository is provided
 * by an implementation, there are no atomicity or transactional guarantees provided.
 * </p>
 * <strong>Example usage</strong>
 * The following example demonstrates how the critical path of building the {@code DepositSubmission} model is insulated
 * from other threads that may be wanting to modify the same {@code Submission}.
 *   <ol>
 *     <li>The URI of the {@code Submission} is provided to the {@code CriticalInteraction}, which will retrieve
 *         the most recent state of resource from the repository</li>
 *     <li>The pre-condition insures that the {@code Submission} retrieved from the repository has the required state
 *         for the critical path (the building of the {@code DepositSubmission})</li>
 *     <li>The post-condition insures that the critical code path succeeded (and is used by the {@link CriticalResult}
 *         to record success or failure of the operation).  Note the post-condition can operate on the just the
 *         {@code PassEntity}, or the {@code PassEntity} <em>and</em> the {@code Object} returned by the critical
 *         path</li>
 *     <li>When the critical path executes, the caller is insured to receive a {@code Submission} that met the
 *         pre-condition.  The critical path can return an object that is <em>not</em> a {@code PassEntity}, in this
 *         case, a {@code DepositSubmission}</li>
 *     <li>Immediately after the execution of the critical path, the {@code PassEntity} will be submitted to the
 *         repository for update.</li>
 *     <li><em>After</em> the resource has been successfully updated (and re-read from the repository), the post
 *         condition is executed to determine the overall success or failure of the {@code CriticalInteraction}</li>
 *   </ol>
 * <pre>
 * CriticalResult&lt;DepositSubmission, Submission&gt; result =
 *  critical.performCritical(submission.getId(), Submission.class,
 *      // pre-condition which is supplied the resource retrieved from submission.getId()
 *      (submission) -&gt; {
 *          return !AggregatedDepositStatus.isTerminalStatus(submission.getAggregatedDepositStatus());
 *      },
 *      // post-condition which is supplied the resource resulting from updating the repository with the modified
 *      // resource resulting from the critical path; that is to say, critical path executes, a round trip to the
 *      // repository occurs, and the result of the round trip is supplied to the post-condition
 *      (submission) -&gt; {
 *          return submission.getAggregatedDepositStatus() == IN_PROGRESS;
 *      }
 *      // critical path
 *      (submission) -&gt; {
 *          DepositSubmission ds = null;
 *          try {
 *              ds = fcrepoModelBuilder.build(submission.getId().toString());
 *          } catch (InvalidModel invalidModel) {
 *              throw new RuntimeException(invalidModel.getMessage(), invalidModel);
 *          }
 *          submission.setAggregatedDepositStatus(IN_PROGRESS);
 *          return ds;
 *  });
 * </pre>
 *
 * @author Elliot Metsger (emetsger@jhu.edu)
 */
public interface CriticalRepositoryInteraction {

    /**
     * Execute a critical interaction with the repository, subject to {@code precondition}.  Success of the interaction
     * depends on the evaluation of {@code postcondition}.
     * <p>
     * See the other form of performCritical if the {@code postcondition} needs to evaluate both the {@code PassEntity}
     * resource and the return from the {@code critical} path.
     * </p>
     *
     * @param id the identifier of the {@code PassEntity} which is the subject of the {@code critical} path
     * @param clazz the concrete {@code Class} of the {@code PassEntity} represented by {@code uri}
     * @param precondition precondition that must evaluate to {@code true} for the {@code critical} path to execute
     * @param postcondition postcondition that must evaluate to {@code true} for the {@code CriticalResult} to be
     *                      considered successful
     * @param critical the critical interaction with the repository, which may return a result of type {@code R}
     * @param updatesEntity whether or not the critical function updates the entity
     * @param <T> the type of {@code PassEntity}
     * @param <R> the type of the result returned by {@code critical}
     * @return a {@code CriticalResult} recording the success or failure of the interaction, and any results.
     */
    <R, T extends PassEntity> CriticalResult<R, T> performCritical(
            String id, Class<T> clazz, Predicate<T> precondition, Predicate<T> postcondition, Function<T, R> critical,
            boolean updatesEntity);

    default <R, T extends PassEntity> CriticalResult<R, T> performCritical(
            String id, Class<T> clazz, Predicate<T> precondition, Predicate<T> postcondition, Function<T, R> critical) {
        return performCritical(id, clazz, precondition, postcondition, critical, true);
    }

    /**
     * Execute a critical interaction with the repository, subject to {@code precondition}.  Success of the interaction
     * depends on the evaluation of {@code postcondition}.
     * <p>
     * See the other form of performCritical if the {@code postcondition} only needs to evaluate the {@code PassEntity}
     * resource.
     * </p>
     *
     * @param id the identifier of the {@code PassEntity} which is the subject of the {@code critical} pathv
     * @param clazz the concrete {@code Class} of the {@code PassEntity} represented by {@code uri}
     * @param precondition precondition that must evaluate to {@code true} for the {@code critical} path to execute
     * @param postcondition postcondition that must evaluate to {@code true} for the {@code CriticalResult} to be
     *                      considered successful
     * @param critical the critical interaction with the repository, which may return a result of type {@code R}
     * @param updatesEntity whether or not the critical function updates the entity
     * @param <T> the type of {@code PassEntity}
     * @param <R> the type of the result returned by {@code critical}
     * @return a {@code CriticalResult} recording the success or failure of the interaction, and any results.
     */
    <R, T extends PassEntity> CriticalResult<R, T> performCritical(
            String id, Class<T> clazz, Predicate<T> precondition, BiPredicate<T, R> postcondition,
            Function<T, R> critical, boolean updatesEntity
    );

    default <R, T extends PassEntity> CriticalResult<R, T> performCritical(
            String id, Class<T> clazz, Predicate<T> precondition, BiPredicate<T, R> postcondition,
            Function<T, R> critical) {
        return performCritical(id, clazz, precondition, postcondition, critical, true);
    }

    /**
     * Encapsulates the result of a critical interaction with the repository.
     *
     * @param <R> the type of result
     * @param <T> the type of {@code PassEntity}
     */
    class CriticalResult<R, T> {
        private R result;
        private T resource;
        private boolean success;
        private Throwable t;

        public CriticalResult(R result, T resource, boolean success) {
            this(result, resource, success, null);
        }

        public CriticalResult(R result, T resource, boolean success, Throwable t) {
            this.result = result;
            this.resource = resource;
            this.success = success;
            this.t = t;
        }

        /**
         * The state of the {@code PassEntity} resource as it was when the interaction with the repository completed.
         * If the interaction with the repository completed without error, the {@code Optional} should not be empty.
         *
         * @return an {@code Optional} containing the state of the {@code PassEntity} at the end of the repository
         *         interaction
         */
        public Optional<T> resource() {
            return Optional.ofNullable(resource);
        }

        /**
         * The result of executing the critical code path.
         *
         * @return the result of executing the critical code path
         */
        public Optional<R> result() {
            return Optional.ofNullable(result);
        }

        /**
         * Result of evaluating the post condition of the critical path, {@code false} otherwise.
         *
         * @return result of evaluating the post condition of the critical path, {@code false} otherwise.
         */
        public boolean success() {
            return success;
        }

        /**
         * Captures any exception that may have occurred when executing the critical path.
         *
         * @return any exception that was caught when executing the critical path
         */
        public Optional<Throwable> throwable() {
            return Optional.ofNullable(t);
        }
    }
}
