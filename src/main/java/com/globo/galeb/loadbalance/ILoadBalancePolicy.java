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
package com.globo.galeb.loadbalance;

import java.util.List;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.IBackend;
import com.globo.galeb.core.RequestData;

/**
 * Interface ILoadBalancePolicy.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public interface ILoadBalancePolicy {

    /** The Constant CACHE_TIMEOUT_FIELDNAME. */
    public static final String CACHE_TIMEOUT_FIELDNAME = "cacheTimeout";

    /**
     * Gets the choice.
     *
     * @param backends backends collection
     * @param requestData the request data
     * @return the backend chosen
     */
    public IBackend getChoice(final List<Backend> backends, final RequestData requestData);

    /**
     * Checks if is default.
     *
     * @return true, if is default
     */
    public boolean isDefault();

    /**
     * Loadbalance string name
     *
     * @return the loadbalance string name
     */
    @Override
    public String toString();

}
