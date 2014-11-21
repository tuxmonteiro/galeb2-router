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

import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.entity.impl.frontend.NullRule;
import com.globo.galeb.entity.impl.frontend.Rule;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;

/**
 * Class RulesCriterion.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 10, 2014.
 * @param <T> the generic type
 */
public class RulesCriterion implements ICriterion<Rule> {

    /** The log. */
    private final SafeLogger log = new SafeLogger();

    /** The map. */
    private Map<String, Rule> map = null;

    /** The request match. */
    private RequestMatch requestMatch;

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#setLog(org.vertx.java.core.logging.Logger)
     */
    @Override
    public ICriterion<Rule> setLog(final Logger logger) {
        log.setLogger(logger);
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#given(java.util.Map)
     */
    @Override
    public ICriterion<Rule> given(final Map<String, Rule> map) {
        this.map = map;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#when(java.lang.Object)
     */
    @Override
    public ICriterion<Rule> when(final Object param) {
        if (param instanceof HttpServerRequest) {
            requestMatch = new RequestMatch((HttpServerRequest)param);
        } else {
            log.warn(String.format("Param is instance of %s.class. Expected %s.class",
                    param.getClass().getSimpleName(), HttpServerRequest.class.getSimpleName()));
        }
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.criteria.ICriterion#thenGetResult()
     */
    @Override
    public Rule thenGetResult() {
        Rule ruleDefault = null;
        for (Rule rule: Rule.sortRules(map.values())) {
            if (ruleDefault==null && rule.isRuleDefault()) {
                ruleDefault = rule;
            }
            if (rule.isMatchWith(requestMatch)) {
                return rule;
            }
        }
        if (ruleDefault!=null) {
            return ruleDefault;
        }
        return new NullRule();
    }
}
