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
package com.globo.galeb.scheduler.impl;

import com.globo.galeb.scheduler.IScheduler;
import com.globo.galeb.scheduler.ISchedulerHandler;


/**
 * Class NullScheduler.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 22, 2014.
 */
public class NullScheduler implements IScheduler {

    /* (non-Javadoc)
     * @see com.globo.galeb.scheduler.IScheduler#start()
     */
    @Override
    public IScheduler start() {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.scheduler.IScheduler#setHandler(com.globo.galeb.scheduler.ISchedulerHandler)
     */
    @Override
    public IScheduler setHandler(ISchedulerHandler schedulerHandler) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.scheduler.IScheduler#setPeriod(java.lang.Long)
     */
    @Override
    public IScheduler setPeriod(Long period) {
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.scheduler.IScheduler#cancel()
     */
    @Override
    public IScheduler cancel() {
        return this;
    }

}
