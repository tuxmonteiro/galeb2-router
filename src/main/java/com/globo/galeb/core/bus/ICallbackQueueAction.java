package com.globo.galeb.core.bus;

public interface ICallbackQueueAction {

    void setVersion(long parseLong);

    boolean delFromMap(String body);

    boolean addToMap(String body);

}
