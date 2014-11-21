package com.globo.galeb.bus;

import com.globo.galeb.entity.impl.frontend.Rule;
import com.globo.galeb.entity.impl.frontend.RuleFactory;
import com.globo.galeb.entity.impl.frontend.Virtualhost;

public class RuleMap extends MessageToMap<Virtualhost> {

    @Override
    public boolean add() {
        boolean isOk = false;

        if ("".equals(parentId)) {
            log.error(String.format("[%s] Inaccessible ParentId: %s", verticleId, entity.encode()));
            return false;
        }
        if (farm.getEntityById(parentId)==null) {
            log.warn(String.format("[%s] Rule not created, because Virtualhost %s not exist", verticleId, parentId));
            return false;
        } else {
            final Virtualhost virtualhost = farm.getEntityById(parentId);

            if (virtualhost!=null) {
                Rule rule = new RuleFactory().setLogger(log).createRule(entity);
                rule.setFarm(farm).start();
                isOk  = virtualhost.addEntity(rule);
                log.info(String.format("[%s] Rule %s (%s) added", verticleId, entityId, parentId));
            } else {
                log.warn(String.format("[%s] Rule %s (%s) already exist", verticleId, entityId, parentId));
            }

        }
        return isOk;
    }

    @Override
    public boolean del() {
        boolean isOk = false;
        boolean hasUriBaseOnly = ("/"+messageBus.getUriBase()).equals(messageBus.getUri()) ||
                messageBus.getUri().endsWith("/");

        if (!hasUriBaseOnly) {

            if ("".equals(parentId)) {
                log.error(String.format("[%s] Inaccessible ParentId: %s", verticleId, entity.encode()));
                return false;
            }

            if ("".equals(entityId)) {
                log.warn(String.format("[%s] Rule UNDEF", verticleId));
                return false;
            } else if (farm.getEntityById(parentId)==null) {
                log.warn(String.format("[%s] Rule not removed. Virtualhost %s not exist", verticleId, parentId));
                return false;
            }
            final Virtualhost virtualhost = farm.getEntityById(parentId);
            isOk = virtualhost!=null;

            if (isOk) {
                isOk = virtualhost.removeEntity(entity);
                log.info(String.format("[%s] Rule %s (%s) removed", verticleId, entityId, parentId));
            } else {
                log.warn(String.format("[%s] Rule not removed. Rule %s (%s) not exist", verticleId, entityId, parentId));
            }

            return isOk;

        } else {
            for (Virtualhost virtualhost: farm.getEntities().values()) {
                virtualhost.clearEntities();
            }
            log.info(String.format("[%s] All Rules removed", verticleId));
            return true;
        }
    }

    @Override
    public boolean reset() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean change() {
        // TODO Auto-generated method stub
        return false;
    }

}
