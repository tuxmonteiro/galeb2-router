package com.globo.galeb.criteria.impl;

import com.globo.galeb.criteria.IWhenMatch;

public class NullMatch implements IWhenMatch {

    @Override
    public String getHeader(String header) {
        return header;
    }

    @Override
    public String getParam(String param) {
        return param;
    }

    @Override
    public boolean isNull() {
        return false;
    }
}
