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
package com.globo.galeb.criteria.impl;

import static com.globo.galeb.consistenthash.HashAlgorithm.HashType.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.globo.galeb.consistenthash.ConsistentHash;
import com.globo.galeb.consistenthash.HashAlgorithm;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.request.RequestData;

import org.vertx.java.core.json.JsonObject;


/**
 * Class IPHashCriterion.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 9, 2014.
 * @param <T> the generic type
 */
public class IPHashCriterion<T> implements ICriterion<T> {

    /** The Constant DEFAULT_HASH_ALGORITHM. */
    public static final String DEFAULT_HASH_ALGORITHM     = SIP24.toString();

    /** The Constant HASH_ALGORITHM_FIELDNAME. */
    public static final String HASH_ALGORITHM_FIELDNAME   = "hashAlgorithm";


    /** The log. */
    @SuppressWarnings("unused")
    private SafeLogger        log            = null;

    /** The collection. */
    private List<T>           collection     = new ArrayList<T>();

    /** The request data. */
    private RequestData       requestData    = new RequestData();

    /** The consistent hash. */
    private ConsistentHash<T> consistentHash = null;

    /** The hash type. */
    private String            hashType       = "";

    /** The source ip. */
    private String            sourceIp       = "";

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#setLog(org.vertx.java.core.logging.Logger)
     */
    @Override
    public ICriterion<T> setLog(final SafeLogger logger) {
        log = logger;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#given(java.util.Map)
     */
    @Override
    public ICriterion<T> given(final Map<String, T> map) {
        if (map!=null) {
            int lastCollectionSize = collection.size();
            this.collection     = new ArrayList<T>(map.values());
            if (collection.size()!=lastCollectionSize) {
                consistentHash = null;
            }
        }
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#when(java.lang.Object)
     */
    @Override
    public ICriterion<T> when(final Object param) {
        if (param instanceof RequestData) {
            requestData = (RequestData) param;
            JsonObject requestDataProperties = ((RequestData) param).getProperties();
            String lastHashType = hashType;
            this.hashType = requestDataProperties.getString(HASH_ALGORITHM_FIELDNAME, DEFAULT_HASH_ALGORITHM);
            if (!hashType.equals(lastHashType)) {
                consistentHash = null;
            }
            this.sourceIp = requestData.getRemoteAddress();
        }
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#thenGetResult()
     */
    @Override
    public T thenGetResult() {

        if (collection.isEmpty() || "".equals(sourceIp)) {
            return null;
        }

        int numberOfReplicas = 1;

        if (consistentHash == null) {
            consistentHash = new ConsistentHash<T>(new HashAlgorithm(hashType),
                                                    numberOfReplicas, collection);
        }

        return consistentHash.get(sourceIp);
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#action(com.globo.galeb.criteria.ICriterion.CriterionAction)
     */
    @Override
    public ICriterion<T> action(ICriterion.CriterionAction criterionAction) {
        return this;
    }

}
