package com.globo.galeb.core;

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
        SafeJsonObject json = new SafeJsonObject(message);
        setEntity(json.getString(entityFieldName,"{}"));
        setParentId(json.getString(parentIdFieldName, ""));
        setUri(json.getString(uriFieldName, ""));
        make();
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

    public SafeJsonObject getEntity() {
        return new SafeJsonObject(entityStr);
    }

    public String getEntityId() {
        return getEntity().getString(IJsonable.jsonIdFieldName, "");
    }

    public SafeJsonObject getEntityProperties() {
        return new SafeJsonObject(getEntity().getString(IJsonable.jsonPropertiesFieldName, "{}"));
    }

    public MessageBus setEntity(String entityStr) {
        this.entityStr = new SafeJsonObject(entityStr).encode();
        return this;
    }

    public MessageBus setEntity(SafeJsonObject entityJson) {
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

        messageBus = new SafeJsonObject()
                            .putString(uriFieldName, uriStr)
                            .putString(parentIdFieldName, parentId)
                            .putString(entityFieldName,
                                    getEntity()
                                        .putObject(IJsonable.jsonPropertiesFieldName,
                                                new SafeJsonObject(properties))
                                        .encode())
                            .encode();
        return this;
    }

    @Override
    public String toString() {
        return messageBus;
    }

    public SafeJsonObject toJson() {
        return new SafeJsonObject(messageBus);
    }

}
