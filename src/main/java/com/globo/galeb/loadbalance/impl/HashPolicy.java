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

import static com.globo.galeb.consistenthash.HashAlgorithm.HashType.*;

import java.util.List;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.consistenthash.ConsistentHash;
import com.globo.galeb.consistenthash.HashAlgorithm;
import com.globo.galeb.core.Backend;
import com.globo.galeb.core.IBackend;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;

/**
 * Class HashPolicy.
 *
 * @author: See AUTHORS file.
 * @version: 1.0.0, Oct 23, 2014.
 */
public class HashPolicy implements ILoadBalancePolicy {

    /** The Constant DEFAULT_HASH_ALGORITHM. */
    public static final String DEFAULT_HASH_ALGORITHM     = SIP24.toString();

    /** The Constant HASH_ALGORITHM_FIELDNAME. */
    public static final String HASH_ALGORITHM_FIELDNAME   = "hashAlgorithm";

    /** The consistent hash. */
    private ConsistentHash<Backend> consistentHash = null;

    /** The last hash type. */
    private String                  lastHashType   = null;

    /* (non-Javadoc)
     * @see com.globo.galeb.loadbalance.ILoadBalancePolicy#getChoice(java.util.List, com.globo.galeb.core.RequestData)
     */
    @Override
    public IBackend getChoice(final List<Backend> backends, final RequestData requestData) {

        String sourceIp = requestData.getRemoteAddress();
        JsonObject properties = requestData.getProperties();
        String hashType = properties.getString(HASH_ALGORITHM_FIELDNAME, DEFAULT_HASH_ALGORITHM);

        int numberOfReplicas = 1;

        if (lastHashType == null || consistentHash == null) {
            lastHashType = hashType;
            consistentHash = new ConsistentHash<Backend>(
                    new HashAlgorithm(hashType), numberOfReplicas, backends);
        }

        if (!lastHashType.equals(hashType)) {
            consistentHash.rebuild(new HashAlgorithm(hashType), numberOfReplicas, backends);
            lastHashType = hashType;
        }

        return consistentHash.get(sourceIp);
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
        return HashPolicy.class.getSimpleName();
    }

}
