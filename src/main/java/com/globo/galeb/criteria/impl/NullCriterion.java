package com.globo.galeb.criteria.impl;

import java.util.Map;

//import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.logging.Logger;

public class NullCriterion<T> implements ICriterion<T> {

    private final SafeLogger log = new SafeLogger();

    @Override
    public ICriterion<T> setLog(final Logger logger) {
        log.setLogger(logger);
        return this;
    }

    @Override
    public ICriterion<T> given(final Map<String, T> map) {
        return this;
    }

    @Override
    public ICriterion<T> when(final Object param) {
        return this;
    }

    @Override
    public T getResult() {
        log.debug(this.getClass().getName());
        return null;
    }
}
