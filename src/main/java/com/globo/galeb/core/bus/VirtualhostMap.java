package com.globo.galeb.core.bus;

import com.globo.galeb.core.Virtualhost;

public class VirtualhostMap extends MessageToMap<Virtualhost> {

    public VirtualhostMap() {
        super();
    }

    @Override
    public boolean add() {
        boolean isOk = false;

        if ("".equals(entityId)) {
            log.error(String.format("[%s] Inaccessible Entity Id: %s", verticleId, entity.encode()));
            return false;
        }

        if (!map.containsKey(entityId)) {
            map.put(entityId, new Virtualhost(entity, vertx));

            log.info(String.format("[%s] %s added", verticleId, entityId));
            isOk = true;
        } else {
            isOk = false;
        }
        return isOk;
    }

    @Override
    public boolean del() {
        boolean isOk = false;
        if (map.containsKey(entityId)) {
            ((Virtualhost) map.get(entityId)).clearAll();
            map.remove(entityId);
            log.info(String.format("[%s] Virtualhost %s removed", verticleId, entityId));
            isOk = true;
        } else {
            log.warn(String.format("[%s] Virtualhost not removed. Virtualhost %s not exist", verticleId, entityId));
            isOk = false;
        }
        return isOk;
    }

    @Override
    public boolean reset() {
        // TODO
        return false;
    }

    @Override
    public boolean change() {
        // TODO
        return false;
    }

}
