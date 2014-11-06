package com.globo.galeb.criteria.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.globo.galeb.consistenthash.ConsistentHash;
import com.globo.galeb.consistenthash.HashAlgorithm;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.loadbalance.impl.HashPolicy;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;

public class ConsistentHashCriterion<T> implements ICriterion<T> {

    private final SafeLogger  log            = new SafeLogger();
    private List<T>           collection     = new ArrayList<T>();
    private RequestData       requestData    = new RequestData();
    private ConsistentHash<T> consistentHash = null;

    @Override
    public ICriterion<T> setLog(final Logger logger) {
        log.setLogger(logger);
        return this;
    }

    @Override
    public ICriterion<T> given(final Map<String, T> map) {
        if (map!=null) {
            this.consistentHash = null;
            this.collection     = (List<T>) map.values();
        }
        return this;
    }

    @Override
    public ICriterion<T> when(final Object param) {
        if (param instanceof RequestData) {
            requestData = (RequestData) param;
        } else if (param instanceof CriterionAction) {
            CriterionAction command = (CriterionAction)param;
            switch (command) {
                case RESET_REQUIRED:
                    consistentHash = null;
                    break;

                default:
                    break;
            }
        }
        return this;
    }

    @Override
    public T getResult() {

        if (collection.isEmpty()) {
            return null;
        }

        String sourceIp = requestData.getRemoteAddress();
        JsonObject properties = requestData.getProperties();
        if ("".equals(sourceIp)||properties==null) {
            return null;
        }
        String hashType = properties.getString(HashPolicy.HASH_ALGORITHM_FIELDNAME,
                                               HashPolicy.DEFAULT_HASH_ALGORITHM);

        int numberOfReplicas = 1;

        if (consistentHash == null) {
            consistentHash = new ConsistentHash<T>(new HashAlgorithm(hashType),
                                                    numberOfReplicas, collection);
        }

        return consistentHash.get(sourceIp);
    }

}
