package com.globo.galeb.core.bus;

import com.globo.galeb.core.Backend;
import com.globo.galeb.core.Virtualhost;

public class BackendMap extends MessageToMap<Virtualhost> {

    public BackendMap() {
        super();
    }

    @Override
    public boolean add() {
        boolean isOk = false;

        if ("".equals(parentId)) {
            log.error(String.format("[%s] Inaccessible ParentId: %s", verticleId, entity.encode()));
            return false;
        }
        if (!map.containsKey(parentId)) {
            log.warn(String.format("[%s] Backend not created, because Virtualhost %s not exist", verticleId, parentId));
            return false;
        } else {
            boolean status = entity.getBoolean(Backend.propertyElegibleFieldName, true);

            final Virtualhost vhost = map.get(parentId);
            if (vhost.addBackend(entity, status)) {
                log.info(String.format("[%s] Backend %s (%s) added", verticleId, entityId, parentId));
                isOk = true;
            } else {
                log.warn(String.format("[%s] Backend %s (%s) already exist", verticleId, entityId, parentId));
                isOk = false;
            }
        }
        return isOk;
    }

    @Override
    public boolean del() {
        boolean isOk = false;
        if ("".equals(parentId)) {
            log.error(String.format("[%s] Inaccessible ParentId: %s", verticleId, entity.encode()));
            return false;
        }
        boolean status = entity.getBoolean(Backend.propertyElegibleFieldName, true);

        if ("".equals(entityId)) {
            log.warn(String.format("[%s] Backend UNDEF", verticleId));
            return false;
        } else if (!map.containsKey(parentId)) {
            log.warn(String.format("[%s] Backend not removed. Virtualhost %s not exist", verticleId, parentId));
            return false;
        }
        final Virtualhost virtualhostObj = map.get(parentId);
        if (virtualhostObj!=null && virtualhostObj.removeBackend(entityId, status)) {
            log.info(String.format("[%s] Backend %s (%s) removed", verticleId, entityId, parentId));
            isOk = true;
        } else {
            log.warn(String.format("[%s] Backend not removed. Backend %s (%s) not exist", verticleId, entityId, parentId));
            isOk = false;
        }

        return isOk;
    }

    @Override
    public boolean reset()  {
        // TODO
        return false;
    }

    @Override
    public boolean change()  {
        // TODO
        return false;
    }

}
