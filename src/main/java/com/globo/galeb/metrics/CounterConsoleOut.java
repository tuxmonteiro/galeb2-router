package com.globo.galeb.metrics;

public class CounterConsoleOut implements ICounter {

    @Override
    public void httpCode(String key, Integer code) {
        System.out.println(String.format("%s.httpCode%d:%d", key, code, 1));
    }

    @Override
    public void incrHttpCode(String key, Integer code) {
        incrHttpCode(key, code, 1.0);
    }

    @Override
    public void incrHttpCode(String key, Integer code, double sample) {
        String srtSample = sample > 0.0 && sample < 1.0 ? String.format("|@%f", sample) : "";
        System.out.println(String.format("%s.httpCode%d:%d%s", key, code, 1, srtSample));
    }

    @Override
    public void decrHttpCode(String key, Integer code) {
        decrHttpCode(key, code, 1.0);
    }

    @Override
    public void decrHttpCode(String key, Integer code, double sample) {
        String srtSample = sample > 0.0 && sample < 1.0 ? String.format("|@%f", sample) : "";
        System.out.println(String.format("%s.httpCode%d:%d%s", key, code, -1, srtSample));
    }

    @Override
    public void requestTime(String key, Long initialRequestTime) {
        Long requestTime = System.currentTimeMillis() - initialRequestTime;
        System.out.println(String.format("%s.requestTime:%d", key, requestTime));
    }

    @Override
    public void sendActiveSessions(String key, Long initialRequestTime) {
        System.out.println(String.format("%s.active:%d", key, 1));
    }

    @Override
    public String cleanupString(String aString, String strDefault) {
        return !"".equals(aString)?aString.replaceAll("[^\\w]", "_"):strDefault;
    }

}
