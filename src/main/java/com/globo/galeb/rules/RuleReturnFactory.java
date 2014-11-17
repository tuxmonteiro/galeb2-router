package com.globo.galeb.rules;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.Farm;
import com.globo.galeb.core.HttpCode;
import com.globo.galeb.core.entity.IJsonable;

public class RuleReturnFactory {

    private final Farm farm;

    public RuleReturnFactory(final Farm farm) {
        this.farm = farm;
    }

    public IRuleReturn getRuleReturn(JsonObject json) {
        JsonObject properties = json.getObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject());
        String ruleReturnStr = properties.getString(Rule.RETURNTYPE_FIELDNAME, HttpCode.class.getSimpleName());
        String ruleReturnIdStr = properties.getString(Rule.RETURNID_FIELDNAME, IJsonable.UNDEF);

        switch (ruleReturnStr) {
            case "HttpCode":
                return new HttpCode(Integer.parseInt(ruleReturnIdStr));
            case "BackendPool":
                return farm.getBackendPoolById(ruleReturnIdStr);
            default:
                break;
        }
        return null;
    }
}
