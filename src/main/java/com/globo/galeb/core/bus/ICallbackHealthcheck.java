package com.globo.galeb.core.bus;

import java.io.UnsupportedEncodingException;

public interface ICallbackHealthcheck {

    public void moveBackend(String backend, boolean status) throws UnsupportedEncodingException;

}
