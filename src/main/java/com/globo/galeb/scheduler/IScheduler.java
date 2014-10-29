/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.globo.galeb.scheduler;

import org.vertx.java.core.Handler;

/**
 * Interface IScheduler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 22, 2014.
 */
public interface IScheduler {

    /**
     * Start scheduler.
     *
     * @return IScheduler
     */
    public IScheduler start();

    /**
     * Sets the handler.
     *
     * @param schedulerHandler the scheduler handler
     * @return IScheduler
     */
    public IScheduler setHandler(ISchedulerHandler schedulerHandler);

    /**
     * Sets the scheduler period (ms).
     *
     * @param period the period
     * @return IScheduler
     */
    public IScheduler setPeriod(Long period);

    /**
     * Cancel the scheduler.
     *
     * @return IScheduler
     */
    public IScheduler cancel();

    /**
     * Cancel handler.
     *
     * @param cancelHandler the cancel handler
     * @return this
     */
    public IScheduler cancelHandler(Handler<Void> cancelHandler);

    /**
     * Cancel failed handler.
     *
     * @param cancelFailedHandler the cancel failed handler
     * @return this
     */
    public IScheduler cancelFailedHandler(Handler<Void> cancelFailedHandler);

}
