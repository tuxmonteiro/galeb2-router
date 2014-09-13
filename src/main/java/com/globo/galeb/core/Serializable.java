package com.globo.galeb.core;

import org.vertx.java.core.json.JsonObject;

public interface Serializable {

    public static String jsonIdFieldName         = "id";
    public static String jsonCreatedAtFieldName  = "created_at";
    public static String jsonModifiedAtFieldName = "modified_at";
    public static String jsonPropertiesFieldName = "properties";

    public JsonObject toJson();

}
