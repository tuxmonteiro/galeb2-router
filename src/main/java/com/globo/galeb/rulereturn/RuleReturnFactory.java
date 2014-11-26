package com.globo.galeb.rulereturn;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.entity.impl.Farm;
import com.globo.galeb.entity.impl.frontend.Rule;

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
                if (IJsonable.UNDEF.equals(ruleReturnIdStr)) {
                    ruleReturnIdStr = "200";
                }
                return new HttpCode(Integer.parseInt(ruleReturnIdStr));
            case "BackendPool":
                return farm.getBackendPoolById(ruleReturnIdStr);
            default:
                break;
        }
        return null;
    }
}
