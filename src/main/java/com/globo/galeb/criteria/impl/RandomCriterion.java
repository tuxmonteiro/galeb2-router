package com.globo.galeb.criteria.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.logging.Logger;

public class RandomCriterion<T> implements ICriterion<T> {

    private final SafeLogger log            = new SafeLogger();
    private List<T>          collection     = new ArrayList<T>();

    @Override
    public ICriterion<T> setLog(final Logger logger) {
        log.setLogger(logger);
        return this;
    }

    @Override
    public ICriterion<T> given(final Map<String, T> map) {
        if (map!=null) {
            this.collection = (List<T>) map.values();
        }
        return this;
    }

    @Override
    public ICriterion<T> when(final Object param) {
        return this;
    }

    @Override
    public T thenResult() {
        if (collection.isEmpty()) {
            return null;
        }
        return collection.get(getIntRandom());
    }

    private int getIntRandom() {
        if (collection.isEmpty()) {
            return 0;
        }
        return (int) (Math.random() * (collection.size() - Float.MIN_VALUE));
    }
}
