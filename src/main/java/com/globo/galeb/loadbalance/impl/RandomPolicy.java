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

import java.util.ArrayList;
import java.util.List;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;

/**
 * Class RandomPolicy.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class RandomPolicy implements ILoadBalancePolicy {

    /* (non-Javadoc)
     * @see com.globo.galeb.loadbalance.ILoadBalancePolicy#getChoice(java.util.List, com.globo.galeb.core.RequestData)
     */
    @Override
    public Backend getChoice(final List<Backend> backends, final RequestData requestData) {

        if (backends!=null && !backends.isEmpty() && backends instanceof ArrayList<?>) {
            return ((ArrayList<Backend>)backends).get(getIntRandom(backends.size()));
        } else {
            return null;
        }
    }

    /**
     * Gets the int random.
     *
     * @param size the size
     * @return the int random
     */
    private int getIntRandom(int size) {
        return (int) (Math.random() * (size - Float.MIN_VALUE));
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return RandomPolicy.class.getSimpleName();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.loadbalance.ILoadBalancePolicy#isDefault()
     */
    @Override
    public boolean isDefault() {
        return false;
    }
}
