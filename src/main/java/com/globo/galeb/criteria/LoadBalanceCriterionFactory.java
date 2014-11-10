package com.globo.galeb.criteria;

import com.globo.galeb.core.Backend;
import com.globo.galeb.criteria.impl.RandomCriterion;

public class LoadBalanceCriterionFactory {

    public static String CLASS_SUFFIX        = "Criterion";
    public static String CLASS_PACKAGE       = LoadBalanceCriterionFactory.class.getPackage().getName()+".impl.";
    public static String DEFAULT_LOADBALANCE = RandomCriterion.class.getSimpleName().replaceFirst(CLASS_SUFFIX, "");

    @SuppressWarnings("unchecked")
    public static ICriterion<Backend> create(String loadBalanceName) {
        if (loadBalanceName==null || "".equals(loadBalanceName)) {
            return create(DEFAULT_LOADBALANCE);
        }

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        String loadBalanceFullName = CLASS_PACKAGE+loadBalanceName+CLASS_SUFFIX;

        try {
            Class<ICriterion<Backend>> clazz = (Class<ICriterion<Backend>>) loader.loadClass(loadBalanceFullName);
            ICriterion<Backend> instance = clazz.newInstance();
            return (ICriterion<Backend>) instance;

        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            // Load default
            return create(DEFAULT_LOADBALANCE);
        }
    }

}
