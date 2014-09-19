package com.globo.galeb.core.bus;

import com.globo.galeb.core.Farm;

public class FarmMap extends MessageToMap<Farm> {

    public FarmMap() {
        super();
    }

    @Override
    public boolean add() {
        boolean isOk = false;
        Farm farm = map.get("farm");

        return isOk;
    }

    @Override
    public boolean del() {
        boolean isOk = false;
        Farm farm = map.get("farm");
        return isOk;
    }

    @Override
    public boolean reset() {
        // TODO
        return false;
    }

    @Override
    public boolean change() {
        // TODO
        return false;
    }

}
