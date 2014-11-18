package com.globo.galeb.criteria.impl;

import java.util.Map;

import org.vertx.java.core.logging.Logger;

import com.globo.galeb.collection.IndexedMap;
import com.globo.galeb.core.IBackend;
import com.globo.galeb.criteria.ICriterion;
import com.globo.galeb.criteria.LoadBalanceCriterionFactory;
import com.globo.galeb.logger.SafeLogger;

public class LoadBalanceCriterion implements ICriterion<IBackend> {

    private SafeLogger             log                  = new SafeLogger();
    private Map<String, IBackend>  map                  = null;
    private String                 loadBalanceName      = "";
    private ICriterion<IBackend>   loadBalanceCriterion = null;
    private Object                 param                = null;

    @Override
    public ICriterion<IBackend> setLog(Logger log) {
        this.log.setLogger(log);
        return this;
    }

    @Override
    public ICriterion<IBackend> given(Map<String, IBackend> map) {
        if (map!=null) {
            this.map = map;
        } else {
            this.map = new IndexedMap<>();
        }
        return this;
    }

    @Override
    public ICriterion<IBackend> when(Object aParam) {

        if (aParam instanceof String) {
            this.loadBalanceName = aParam.toString();
            return this;
        }

        if (aParam instanceof CriterionAction) {
            CriterionAction command = (CriterionAction)param;
            switch (command) {
                case RESET_REQUIRED:
                    loadBalanceCriterion = null;
                    break;

                default:
                    break;
            }
            return this;
        }

        this.param = aParam;
        return this;
    }

    @Override
    public IBackend thenGetResult() {
        if (loadBalanceCriterion==null) {
            loadBalanceCriterion = LoadBalanceCriterionFactory.create(loadBalanceName);
        }
        return loadBalanceCriterion.given(map).when(param).thenGetResult();
    }

}
