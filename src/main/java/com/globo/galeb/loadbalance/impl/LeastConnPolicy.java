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
package com.globo.galeb.loadbalance.impl;

import java.util.List;

import com.globo.galeb.core.entity.impl.backend.Backend;
import com.globo.galeb.core.entity.impl.backend.IBackend;
import com.globo.galeb.core.request.RequestData;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;

/**
 * Class LeastConnPolicy.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class LeastConnPolicy implements ILoadBalancePolicy {

    /** The least connections finder. */
    private LeastConnectionsFinder leastConnectionsFinder = null;

    /** last time it was reseted */
    private long lastReset = System.currentTimeMillis();

    /* (non-Javadoc)
     * @see com.globo.galeb.loadbalance.ILoadBalancePolicy#getChoice(java.util.List, com.globo.galeb.core.RequestData)
     */
    @Override
    public IBackend getChoice(final List<Backend> backends, final RequestData requestData) {

//        JsonObject properties = requestData.getProperties();
//        long timeout = properties.getLong(ILoadBalancePolicy.CACHE_TIMEOUT_FIELDNAME, 2000L);

        long now = System.currentTimeMillis();

        if (leastConnectionsFinder == null) {
            lastReset = now;
            leastConnectionsFinder = new LeastConnectionsFinder(backends);
        }

//        if (transientState) {
//            leastConnectionsFinder.rebuild(backends);
//            lastReset = now;
//        } else if ((lastReset + timeout) < now) {
//            leastConnectionsFinder.update();
//            lastReset = now;
//        }

        return leastConnectionsFinder.get();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.loadbalance.ILoadBalancePolicy#isDefault()
     */
    @Override
    public boolean isDefault() {
        return false;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return LeastConnPolicy.class.getSimpleName();
    }

}
