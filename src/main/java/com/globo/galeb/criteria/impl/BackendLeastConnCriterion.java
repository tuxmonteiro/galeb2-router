package com.globo.galeb.criteria.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import com.globo.galeb.core.Backend;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.logging.Logger;

public class BackendLeastConnCriterion<T> implements ICriterion<T> {

    private final SafeLogger log            = new SafeLogger();
    private List<T>          backends       = new ArrayList<T>();

    @Override
    public ICriterion<T> setLog(final Logger logger) {
        log.setLogger(logger);
        return this;
    }

    @Override
    public ICriterion<T> given(final Map<String, T> map) {
        if (map!=null) {
            this.backends = (List<T>) map.values();
        }
        return this;
    }

    @Override
    public ICriterion<T> when(final Object param) {
        return this;
    }

    @Override
    public T thenResult() {

        if (backends.isEmpty()) {
            return null;
        }

        Collections.sort(backends, new Comparator<T>() {

            @Override
            public int compare(Object o1, Object o2) {
                if (o1 instanceof Backend) {
                    Backend backend1 = (Backend) o1;
                    Backend backend2 = (Backend) o2;

                    return backend1.getActiveConnections() - backend2.getActiveConnections();
                } else {
                    log.error(String.format("%s support only %s class",
                            this.getClass().getName(), Backend.class.getName()));
                }
                return 0;
            }

        });

        return backends.get(0);
    }

}
