package com.globo.galeb.criteria.impl;

import java.util.Map;

//import com.globo.galeb.core.Virtualhost;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.logger.SafeLogger;

import org.vertx.java.core.http.HttpHeaders;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.logging.Logger;

public class HostHeaderCriterion<T> implements ICriterion<T> {

    private final SafeLogger log = new SafeLogger();
    private String host = "";
    private Map<String, T> map = null;

    @Override
    public ICriterion<T> setLog(final Logger logger) {
        log.setLogger(logger);
        return this;
    }

    @Override
    public ICriterion<T> given(final Map<String, T> map) {
        this.map = map;
        return this;
    }

    @Override
    public ICriterion<T> when(final Object param) {
        if (param instanceof HttpServerRequest) {
            host = new RequestMatch((HttpServerRequest)param).getHeader(HttpHeaders.HOST.toString());
        } else {
            log.warn(String.format("Param is instance of %s.class. Expected %s.class",
                    param.getClass().getSimpleName(), HttpServerRequest.class.getSimpleName()));
        }
        return this;
    }

    @Override
    public T thenResult() {
        if ("".equals(host)) {
            log.warn("Host UNDEF");
            return null;
        }
        String hostWithoutPort = host.split(":")[0];
        if (!map.containsKey(hostWithoutPort)) {
            log.warn(String.format("Host: %s UNDEF", hostWithoutPort));
            return null;
        }

        return map.get(hostWithoutPort);
    }
}
