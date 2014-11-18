package com.globo.galeb.criteria;

import com.globo.galeb.core.IBackend;
import com.globo.galeb.criteria.impl.RandomCriterion;

public class LoadBalanceCriterionFactory {

    public static final String CLASS_SUFFIX        = "Criterion";
    public static final String CLASS_PACKAGE       = LoadBalanceCriterionFactory.class.getPackage().getName()+".impl.";
    public static final String DEFAULT_LOADBALANCE = RandomCriterion.class.getSimpleName().replaceFirst(CLASS_SUFFIX, "");

    @SuppressWarnings("unchecked")
    public static ICriterion<IBackend> create(String loadBalanceName) {
        if (loadBalanceName==null || "".equals(loadBalanceName)) {
            return create(DEFAULT_LOADBALANCE);
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String loadBalanceFullName = CLASS_PACKAGE+loadBalanceName+CLASS_SUFFIX;

        try {
            Class<ICriterion<IBackend>> clazz = (Class<ICriterion<IBackend>>) loader.loadClass(loadBalanceFullName);
            ICriterion<IBackend> instance = clazz.newInstance();
            return (ICriterion<IBackend>) instance;

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            // Load default
            return create(DEFAULT_LOADBALANCE);
        }
    }

}
