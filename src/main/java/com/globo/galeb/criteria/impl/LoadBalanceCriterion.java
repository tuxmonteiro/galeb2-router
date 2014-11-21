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
import com.globo.galeb.core.entity.impl.backend.IBackend;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.criteria.LoadBalanceCriterionFactory;
import com.globo.galeb.logger.SafeLogger;

/**
 * Class LoadBalanceCriterion.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 20, 2014.
 */
public class LoadBalanceCriterion implements ICriterion<IBackend> {

    /** The log. */
    private SafeLogger             log                  = new SafeLogger();

    /** The map. */
    private Map<String, IBackend>  map                  = null;

    /** The load balance name. */
    private String                 loadBalanceName      = "";

    /** The load balance criterion. */
    private ICriterion<IBackend>   loadBalanceCriterion = null;

    /** The param. */
    private Object                 param                = null;

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#setLog(org.vertx.java.core.logging.Logger)
     */
    @Override
    public ICriterion<IBackend> setLog(Logger log) {
        this.log.setLogger(log);
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#given(java.util.Map)
     */
    @Override
    public ICriterion<IBackend> given(Map<String, IBackend> map) {
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
    public ICriterion<IBackend> when(Object aParam) {

        if (aParam instanceof String) {
            this.loadBalanceName = aParam.toString();
            return this;
        }

        if (aParam instanceof CriterionAction) {
            CriterionAction command = (CriterionAction)param;
            switch (command) {
                case RESET_REQUIRED:
                    loadBalanceCriterion = null;
                    break;

                default:
                    break;
            }
            return this;
        }

        this.param = aParam;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#thenGetResult()
     */
    @Override
    public IBackend thenGetResult() {
        if (loadBalanceCriterion==null) {
            loadBalanceCriterion = LoadBalanceCriterionFactory.create(loadBalanceName);
        }
        return loadBalanceCriterion.given(map).when(param).thenGetResult();
    }

}
