/*
 * Copyright (c) 2014 Globo.com - ATeam
 * All rights reserved.
 *
 * This source is subject to the Apache License, Version 2.0.
 * Please see the LICENSE file for more information.
 *
 * Authors: See AUTHORS file
 *
 * THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
 * PARTICULAR PURPOSE.
 */
package com.globo.galeb.loadbalance.impl;

import static com.globo.galeb.consistenthash.HashAlgorithm.HashType.*;

import java.util.List;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.consistenthash.ConsistentHash;
import com.globo.galeb.consistenthash.HashAlgorithm;
import com.globo.galeb.core.Backend;
import com.globo.galeb.core.RequestData;
import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.loadbalance.ILoadBalancePolicy;

public class HashPolicy implements ILoadBalancePolicy {

    public static final String DEFAULT_HASH_ALGORITHM     = SIP24.toString();
    public static final String HASH_ALGORITHM_FIELDNAME   = "hashAlgorithm";

    private ConsistentHash<Backend> consistentHash = null;
    private String                  lastHashType   = null;

    @Override
    public Backend getChoice(final List<Backend> backends, final RequestData requestData) {

        String sourceIp = requestData.getRemoteAddress();
        JsonObject properties = requestData.getProperties();
        String hashType = properties.getString(HASH_ALGORITHM_FIELDNAME, DEFAULT_HASH_ALGORITHM);
        boolean transientState = properties.getBoolean(Virtualhost.TRANSIENT_STATE_FIELDNAME, false);

        int numberOfReplicas = 1;

        if (lastHashType == null || consistentHash == null) {
            lastHashType = hashType;
            transientState = false;
            consistentHash = new ConsistentHash<Backend>(
                    new HashAlgorithm(hashType), numberOfReplicas, backends);
        }

        if (!lastHashType.equals(hashType)) {
            consistentHash.rebuild(new HashAlgorithm(hashType), numberOfReplicas, backends);
            lastHashType = hashType;
        } else if (transientState) {
            consistentHash.rebuild(null, null, backends);
        }

        return consistentHash.get(sourceIp);
    }

    @Override
    public boolean isDefault() {
        return false;
    }

    @Override
    public String toString() {
        return HashPolicy.class.getSimpleName();
    }

}
