package com.globo.galeb.criteria;

import java.util.Map;

import org.vertx.java.core.logging.Logger;

public interface ICriterion<T> {

    public static enum CriterionAction {
        RESET_REQUIRED
    }

    public ICriterion<T> setLog(Logger log);

    public ICriterion<T> given(Map<String, T> map);

    public ICriterion<T> when(Object param);

    public T thenResult();

}
