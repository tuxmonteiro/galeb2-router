package com.globo.galeb.core;

import org.vertx.java.core.json.JsonObject;

public interface IJsonable {

    public static final String jsonIdFieldName         = "id";
    public static final String jsonPropertiesFieldName = "properties";

    public static final String jsonCreatedAtFieldName  = "_created_at";
    public static final String jsonModifiedAtFieldName = "_modified_at";
    public static final String jsonLinksFieldName      = "_links";
    public static final String jsonLinksRelFieldName   = "_rel";
    public static final String jsonLinksHrefFieldName  = "_href";

    public JsonObject toJson();

}
