package com.globo.galeb.core.bus;

import org.vertx.java.core.json.JsonObject;

public interface ICallbackConnectionCounter {

    public void callbackGlobalConnectionsInfo(JsonObject body);

    public void setRegistered(boolean registered);

    public boolean isRegistered();

}
