package com.globo.galeb.core.bus;

public class NullMap extends MapProcessor<Object> {

    public NullMap() {
        super();
    }

    @Override
    public boolean add() {
        if (log!=null) {
            log.warn(String.format("[%s] uriBase %s not supported", verticleId, uriBase));
        } else {
            System.err.println("Farm is Null and uriBase not supported");
        }
        return super.add();
    }

    @Override
    public boolean del() {
        if (log!=null) {
            log.warn(String.format("[%s] uriBase %s not supported", verticleId, uriBase));
        } else {
            System.err.println("Farm is Null and uriBase not supported");
        }
        return super.del();
    }

    @Override
    public boolean reset() {
        if (log!=null) {
            log.warn(String.format("[%s] uriBase %s not supported", verticleId, uriBase));
        } else {
            System.err.println("Farm is Null and uriBase not supported");
        }
        return super.reset();
    }

    @Override
    public boolean change() {
        if (log!=null) {
            log.warn(String.format("[%s] uriBase %s not supported", verticleId, uriBase));
        } else {
            System.err.println("Farm is Null and uriBase not supported");
        }
        return super.change();
    }

}
