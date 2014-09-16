package com.globo.galeb.core;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;

public class MessageBus {

    public static final String entityFieldName      = "entity";
    public static final String parentIdFieldName    = "parentId";
    public static final String uriFieldName         = "uri";

    private String entityStr  = "{}";
    private String parentId   = "";
    private String uriStr     = "";
    private String properties = "{}";
    private String messageBus = "{}";

    public MessageBus() {
        this("{}");
    }

    public MessageBus(String message) {
        try {
            JsonObject json = new JsonObject(message);
            setEntity(json.getString(entityFieldName,"{}"));
            setParentId(json.getString(parentIdFieldName, ""));
            setUri(json.getString(uriFieldName, ""));
            make();
        } catch (DecodeException ignore) {
            // ignore
        }
    }

    public String getParentId() {
        return parentId;
    }

    public MessageBus setParentId(String parentId) {
        if (parentId!=null) {
            this.parentId = parentId;
        }
        return this;
    }

    public JsonObject getEntity() {
        return new JsonObject(entityStr);
    }

    public String getEntityId() {
        return getEntity().getString(IJsonable.jsonIdFieldName, "");
    }

    public JsonObject getEntityProperties() {
        return new JsonObject(getEntity().getString(IJsonable.jsonPropertiesFieldName, "{}"));
    }

    public MessageBus setEntity(String entityStr) {
        if (entityStr!=null) {
            try {
                new JsonObject(entityStr);
                this.entityStr = entityStr;
            } catch (DecodeException e) {
                this.entityStr = "{}";
            }
        } else {
            this.entityStr = "{}";
        }
        return this;
    }

    public MessageBus setEntity(JsonObject entityJson) {
        if (entityJson!=null) {
            this.entityStr = entityJson.encode();
        } else {
            this.entityStr = "{}";
        }
        return this;
    }

    public String getUri() {
        return uriStr;
    }

    public MessageBus setUri(String uriStr) {
        if (uriStr!=null) {
            this.uriStr = uriStr;
        }
        return this;
    }

    public String getUriBase() {
        String[] uriStrArray = uriStr.split("/");
        return uriStrArray.length > 0 ? uriStrArray[1] : "";
    }

    public MessageBus make() {

        messageBus = new JsonObject()
                            .putString(uriFieldName, uriStr)
                            .putString(parentIdFieldName, parentId)
                            .putString(entityFieldName,
                                    getEntity()
                                        .putObject(IJsonable.jsonPropertiesFieldName,
                                                new JsonObject(properties))
                                        .encode())
                            .encode();
        return this;
    }

    @Override
    public String toString() {
        return messageBus;
    }

    public JsonObject toJson() {
        return new JsonObject(messageBus);
    }

}
