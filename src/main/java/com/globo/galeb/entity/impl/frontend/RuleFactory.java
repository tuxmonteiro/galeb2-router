package com.globo.galeb.entity.impl.frontend;

import static org.apache.commons.lang3.exception.ExceptionUtils.getStackTrace;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.vertx.java.core.json.JsonObject;
import com.globo.galeb.entity.IJsonable;
import com.globo.galeb.logger.SafeLogger;

public class RuleFactory {

    public static final String CLASS_PACKAGE     = Rule.class.getPackage().getName();
    public static final String DEFAULT_RULETYPE  = NullRule.class.getSimpleName();

    private SafeLogger log = null;

    public Rule createRule(JsonObject json) {

        JsonObject properties = json.getObject(IJsonable.PROPERTIES_FIELDNAME, new JsonObject());
        String ruleType = properties.getString(Rule.RULETYPE_FIELDNAME, "");

        if ("".equals(ruleType)) {
            return new NullRule();
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String ruleFullName = CLASS_PACKAGE+"."+ruleType;

        try {

            @SuppressWarnings("unchecked")
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

            if (log==null) {
                log = new SafeLogger();
            }
            log.debug(getStackTrace(e));

            return new NullRule();
        }
    }

    public RuleFactory setLogger(final SafeLogger logger) {
        log = logger;
        return this;
    }

}
