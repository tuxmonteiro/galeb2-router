package com.globo.galeb.core.bus;

import java.util.HashMap;
import java.util.Map;

import org.vertx.java.core.Vertx;
import org.vertx.java.core.logging.Logger;

import com.globo.galeb.core.SafeJsonObject;

public abstract class MessageToMap<T> {

    protected String uriBase        = "";
    protected SafeJsonObject entity = new SafeJsonObject();
    protected String entityId       = "";
    protected String parentId       = "";

    protected Map<String, T> map    = new HashMap<>();
    protected MessageBus messageBus = new MessageBus().setUri("/null");
    protected Logger log            = null;
    protected String verticleId     = "";
    protected Vertx vertx           = null;

    public MessageToMap<T> setVerticleId(String id) {
        this.verticleId = id;
        return this;
    }

    public MessageToMap<T> setLogger(final Logger log) {
        this.log = log;
        return this;
    }

    public MessageToMap<T> setMessageBus(final MessageBus messageBus) {
        this.messageBus = messageBus;

        if (messageBus!=null) {
            this.uriBase  = messageBus.getUriBase();
            this.entity   = messageBus.getEntity();
            this.entityId = messageBus.getEntityId();
            this.parentId = messageBus.getParentId();
        }

        return this;
    }

    public MessageToMap<T> setMap(final Map<String, T> map) {
        if (map!=null) {
            this.map = map;
        }
        return this;
    }

    public MessageToMap<T> setVertx(final Vertx vertx) {
        this.vertx = vertx;
        return this;
    }

    public boolean add() {
        return false;
    }

    public boolean del() {
        return false;
    }

    public boolean reset() {
        return false;
    }

    public boolean change() {
        return false;
    }

}
