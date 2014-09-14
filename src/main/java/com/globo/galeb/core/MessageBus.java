package com.globo.galeb.core;

import org.vertx.java.core.json.DecodeException;
import org.vertx.java.core.json.JsonObject;

public class MessageBus {

    public static final String virtualhostFieldName = "virtualhost";
    public static final String backendFieldName     = "backend";
    public static final String uriFieldName         = "uri";

    private String virtualhostStr = "{}";
    private String backendStr = "{}";
    private String uriStr = "";
    private String properties = "{}";
    private String messageBus = "{}";

    public MessageBus() {
        this("{}");
    }

    public MessageBus(String message) {
        try {
            JsonObject json = new JsonObject(message);
            setVirtualhost(json.getString(virtualhostFieldName,"{}"));
            setBackend(json.getString(backendFieldName,"{}"));
            setUri(json.getString(uriFieldName, ""));
            make();
        } catch (DecodeException ignore) {
            // ignore
        }
    }

    public JsonObject getVirtualhost() {
        return new JsonObject(virtualhostStr);
    }

    public String getVirtualhostId() {
        return getVirtualhost().getString(Serializable.jsonIdFieldName, "");
    }

    public JsonObject getVirtualhostProperties() {
        return new JsonObject(getVirtualhost().getString(Serializable.jsonPropertiesFieldName, "{}"));
    }

    public MessageBus setVirtualhost(String virtualhostStr) {
        if (virtualhostStr!=null) {
            try {
                new JsonObject(virtualhostStr);
                this.virtualhostStr = virtualhostStr;
            } catch (DecodeException e) {
                this.virtualhostStr = "{}";
            }
        } else {
            this.virtualhostStr = "{}";
        }
        return this;
    }

    public MessageBus setVirtualhost(JsonObject virtualhostJson) {
        if (virtualhostJson!=null) {
            this.virtualhostStr = virtualhostJson.encode();
        } else {
            this.virtualhostStr = "{}";
        }
        return this;
    }

    public JsonObject getBackend() {
        JsonObject json = new JsonObject();
        try {
            json.mergeIn(new JsonObject(backendStr));
        } catch (DecodeException ignore) {
           // ignore
        }
        return json;
    }

    public String getBackendId() {
        return getBackend().getString(Serializable.jsonIdFieldName, "");
    }

    public JsonObject getBackendProperties() {
        return new JsonObject(getBackend().getString(Serializable.jsonPropertiesFieldName, "{}"));
    }

    public MessageBus setBackend(String backendStr) {
        if (backendStr!=null) {
            try {
                new JsonObject(backendStr);
                this.backendStr = backendStr;
            } catch (DecodeException e) {
                this.backendStr = "{}";
            }
        } else {
            this.backendStr = "{}";
        }
        return this;
    }

    public MessageBus setBackend(JsonObject backendJson) {
        if (backendStr!=null) {
            this.backendStr = backendJson.encode();
        } else {
            this.backendStr = "{}";
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

        JsonObject messageJson = new JsonObject()
                                        .putString(virtualhostFieldName,
                                                getVirtualhost()
                                                    .putObject(Serializable.jsonPropertiesFieldName,
                                                            new JsonObject(properties))
                                                    .encode())
                                        .putString(backendFieldName, backendStr)
                                        .putString(uriFieldName, uriStr);

        messageBus = messageJson.toString();
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
