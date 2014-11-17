package com.globo.galeb.rules;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.vertx.java.core.json.JsonObject;

import com.globo.galeb.core.entity.IJsonable;

public class RuleFactory {

    public static  String CLASS_PACKAGE     = Rule.class.getPackage().getName();
    public static  String DEFAULT_RULETYPE  = NullRule.class.getSimpleName();

    @SuppressWarnings("unchecked")
    public static Rule createRule(JsonObject json) {

        JsonObject properties = json.getObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject());
        String ruleType = properties.getString(Rule.RULETYPE_FIELDNAME, "");

        if ("".equals(ruleType)) {
            return null;
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String ruleFullName = CLASS_PACKAGE+"."+ruleType;

        try {

            Class<Rule> clazz = (Class<Rule>) loader.loadClass(ruleFullName);
            Constructor<Rule> classWithConstructor = clazz.getConstructor(JsonObject.class);
            Rule instance = classWithConstructor.newInstance(json);

            return instance;

        } catch (ClassNotFoundException |
                InstantiationException |
                IllegalAccessException |
                NoSuchMethodException |
                SecurityException |
                IllegalArgumentException |
                InvocationTargetException e) {

            e.printStackTrace();
            return null;
        }
    }

}
