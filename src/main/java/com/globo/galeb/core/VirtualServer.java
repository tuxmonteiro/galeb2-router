package com.globo.galeb.core;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.entity.EntitiesMap;
import com.globo.galeb.criteria.impl.RulesCriterion;
import com.globo.galeb.rules.Rule;

public class VirtualServer extends EntitiesMap<Rule> {

    public VirtualServer() {
        this("UNDEF");
    }

    public VirtualServer(String id) {
        this(new JsonObject().putString(ID_FIELDNAME, id));
    }

    public VirtualServer(JsonObject json) {
        super(json);
        setCriterion(new RulesCriterion<Rule>().setLog(logger));
    }

    @Override
    public boolean addEntity(Rule entity) {
//        Collections.sort(getEntities().values());
        return super.addEntity(entity);
    }

}
