package com.globo.galeb.criteria.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.logging.Logger;

public class RoundRobinCriterion<T> implements ICriterion<T> {

    private final SafeLogger log            = new SafeLogger();
    private List<T>          collection     = Collections.synchronizedList(new LinkedList<T>());
    private List<T>          originals      = new LinkedList<T>();

    @Override
    public ICriterion<T> setLog(final Logger logger) {
        log.setLogger(logger);
        return this;
    }

    @Override
    public ICriterion<T> given(final Map<String, T> map) {
        if (map!=null) {
            if (!collection.isEmpty()) {
                this.collection.clear();
            }
            this.originals = (List<T>) map.values();
            this.collection.addAll(originals);
        } else {
            log.error(String.format("%s: map is null", this.getClass().getName()));
        }
        return this;
    }

    @Override
    public ICriterion<T> when(final Object param) {
        return this;
    }

    @Override
    public T thenResult() {
        if (originals.isEmpty()) {
            return null;
        }

        if (collection.isEmpty()) {
            collection.addAll(originals);
        }

        return ((LinkedList<T>) collection).poll();
    }

}
