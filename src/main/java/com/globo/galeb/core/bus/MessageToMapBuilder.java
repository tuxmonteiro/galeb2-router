package com.globo.galeb.core.bus;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.Farm;

public class MessageToMapBuilder {

    public static MapProcessor<?> getInstance(String message, Farm farm) {

        if (farm==null) {
            return new NullMap();
        }

        final MessageBus messageBus = new MessageBus(message);
        final Logger log = farm.getLogger();
        final Vertx vertx = farm.getVertx();
        final String verticleId = farm.getVerticleId();

        switch (messageBus.getUriBase()) {
            case "virtualhost":
                return new VirtualhostMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setMap(farm.getVirtualhostsToMap())
                    .setVerticleId(verticleId);
            case "backend":
                return new BackendMap()
                    .setMessageBus(messageBus)
                    .setLogger(log)
                    .setVertx(vertx)
                    .setMap(farm.getVirtualhostsToMap())
                    .setVerticleId(verticleId);
            default:
                break;
        }
        return new NullMap().setLogger(log).setVerticleId(verticleId);
    }

}
