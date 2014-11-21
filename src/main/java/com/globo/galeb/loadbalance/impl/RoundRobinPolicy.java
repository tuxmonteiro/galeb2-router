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
 * Class RoundRobinPolicy.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RoundRobinPolicy implements ILoadBalancePolicy {

    /** The last round robin position. */
    private int pos = -1;

    /* (non-Javadoc)
     * @see com.globo.galeb.loadbalance.ILoadBalancePolicy#getChoice(java.util.List, com.globo.galeb.core.RequestData)
     */
    @Override
    public IBackend getChoice(final List<Backend> backends, final RequestData requestData) {

        int size = backends.size();
        pos = pos+1>=size ? 0 : pos+1;

       return backends.get(pos);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return RoundRobinPolicy.class.getSimpleName();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.loadbalance.ILoadBalancePolicy#isDefault()
     */
    @Override
    public boolean isDefault() {
        return false;
    }
}
