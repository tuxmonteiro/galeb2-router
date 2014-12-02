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
package com.globo.galeb.entity.impl.frontend;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.criteria.impl.RequestMatch;
import com.globo.galeb.entity.Entity;
import com.globo.galeb.rulereturn.IRuleReturn;
import com.globo.galeb.rulereturn.RuleReturnFactory;


/**
 * Class Rule.
 *
 * @author See AUTHORS file.
 * @version 1.0.0, Nov 6, 2014.
 */
public abstract class Rule extends Entity {

    /** The Constant RULETYPE_FIELDNAME. */
    public static final String RULETYPE_FIELDNAME   = "ruleType";

    /** The Constant RETURNTYPE_FIELDNAME. */
    public static final String RETURNTYPE_FIELDNAME = "returnType";

    /** The Constant ORDERNUM_FIELDNAME. */
    public static final String ORDERNUM_FIELDNAME   = "orderNum";

    /** The Constant MATCH_FIELDNAME. */
    public static final String MATCH_FIELDNAME      = "match";

    /** The Constant RETURNID_FIELDNAME. */
    public static final String RETURNID_FIELDNAME   = "returnId";

    /** The Constant DEFAULT_FIELDNAME. */
    public static final String DEFAULT_FIELDNAME    = "default";

    /** The rule return. */
    private IRuleReturn   ruleReturn    = null;

    /** The priority order. */
    private Integer       priorityOrder = 999;

    /** The match. */
    protected Object      match         = new Object();

    /** The rule is the default. */
    protected boolean     ruleDefault     = false;

    /**
     * Instantiates a new rule.
     */
    public Rule() {
        this(UNDEF);
    }

    /**
     * Instantiates a new rule.
     *
     * @param id the id
     */
    public Rule(String id) {
        this(new JsonObject().putString(ID_FIELDNAME, id));
    }

    /**
     * Instantiates a new rule.
     *
     * @param json the json
     */
    public Rule(JsonObject json) {
        super(json);
        ruleDefault = properties.getBoolean(DEFAULT_FIELDNAME, false);
        priorityOrder = properties.getInteger(ORDERNUM_FIELDNAME, 999);
        match = properties.getString(MATCH_FIELDNAME, UNDEF);
        entityType = Rule.class.getSimpleName().toLowerCase();
    }

    /**
     * Gets the rule return.
     *
     * @return the rule return
     */
    public IRuleReturn getRuleReturn() {
        return ruleReturn;
    }

    /**
     * Sets the rule return.
     *
     * @param ruleReturn the rule return
     * @return this
     */
    public Rule setRuleReturn(IRuleReturn ruleReturn) {
        this.ruleReturn = ruleReturn;
        return this;
    }

    /**
     * Gets the priority order.
     *
     * @return the priority order
     */
    public Integer getPriorityOrder() {
        return priorityOrder;
    }

    /**
     * Sets the priority order.
     *
     * @param priorityOrder the priority order
     * @return this
     */
    public Rule setPriorityOrder(Integer priorityOrder) {
        this.priorityOrder = priorityOrder;
        return this;
    }

    /**
     * Gets the match.
     *
     * @return the match
     */
    public Object getMatch() {
        return this.match;
    }

    /**
     * Sets the match.
     *
     * @param match the match
     * @return this
     */
    public Rule setMatch(Object match) {
        this.match = match;
        return this;
    }

    /**
     * Checks if is rule default.
     *
     * @return true, if is rule default
     */
    public boolean isRuleDefault() {
        return ruleDefault;
    }

    /**
     * Sets the rule default.
     *
     * @param ruleDefault the rule default
     * @return the rule
     */
    public Rule setRuleDefault(boolean ruleDefault) {
        this.ruleDefault = ruleDefault;
        return this;
    }

    /* (non-Javadoc)
     * @see com.globo.galeb.core.entity.Entity#start()
     */
    @Override
    public void start() {
        super.start();
        ruleReturn = new RuleReturnFactory(farm).getRuleReturn(idObj);
    }

    /**
     * Checks if is match with.
     *
     * @param requestMatch the request match
     * @return true, if is match with
     */
    public abstract boolean isMatchWith(RequestMatch requestMatch);

}
