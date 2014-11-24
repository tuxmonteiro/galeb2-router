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

import java.util.Map;

import org.vertx.java.core.logging.Logger;

import com.globo.galeb.collection.IndexedMap;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.criteria.LoadBalanceCriterionFactory;
import com.globo.galeb.logger.SafeLogger;
import com.globo.galeb.request.RequestData;

/**
 * Class LoadBalanceCriterion.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 20, 2014.
 */
public class LoadBalanceCriterion<T> implements ICriterion<T> {

    /** The Constant LOADBALANCE_POLICY_DEFAULT. */
    public static final String LOADBALANCE_POLICY_DEFAULT    = RandomCriterion.class.getSimpleName()
                                                                .replaceAll(LoadBalanceCriterionFactory.CLASS_SUFFIX, "");

    /** The Constant LOADBALANCE_POLICY_FIELDNAME. */
    public static final String LOADBALANCE_POLICY_FIELDNAME  = "loadBalancePolicy";


    /** The log. */
    private SafeLogger             log                  = new SafeLogger();

    /** The map. */
    private Map<String, T>         map                  = null;

    /** The load balance name. */
    private String                 loadBalanceName      = "";

    /** The load balance criterion. */
    private ICriterion<T>          loadBalanceCriterion = null;

    /** The param. */
    private Object                 param                = null;

    /** The reset. */
    private boolean reset = false;

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#setLog(org.vertx.java.core.logging.Logger)
     */
    @Override
    public ICriterion<T> setLog(Logger log) {
        this.log.setLogger(log);
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#given(java.util.Map)
     */
    @Override
    public ICriterion<T> given(Map<String, T> map) {
        if (map!=null) {
            this.map = map;
        } else {
            this.map = new IndexedMap<>();
        }
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#when(java.lang.Object)
     */
    @Override
    public ICriterion<T> when(Object aParam) {

        if (aParam instanceof RequestData) {
            this.loadBalanceName = ((RequestData) aParam).getProperties()
                                                         .getString(LOADBALANCE_POLICY_FIELDNAME,LOADBALANCE_POLICY_DEFAULT);
            this.param = aParam;
        }

        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#thenGetResult()
     */
    @SuppressWarnings("unchecked")
    @Override
    public T thenGetResult() {
        if (loadBalanceCriterion==null) {
            if (reset) {
                LoadBalanceCriterionFactory.reset(loadBalanceName);
                reset = false;
            }
            loadBalanceCriterion = (ICriterion<T>) LoadBalanceCriterionFactory.create(loadBalanceName);
        }
        return (T) loadBalanceCriterion.given(map).when(param).thenGetResult();
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#action(com.globo.galeb.criteria.ICriterion.CriterionAction)
     */
    @Override
    public ICriterion<T> action(ICriterion.CriterionAction criterionAction) {
        switch (criterionAction) {
            case RESET_REQUIRED:
                loadBalanceCriterion = null;
                reset = true;
                break;

            default:
                break;
        }
        return this;
    }

}
