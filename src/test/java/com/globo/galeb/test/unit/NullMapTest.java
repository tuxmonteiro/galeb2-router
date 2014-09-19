package com.globo.galeb.test.unit;

import static org.junit.Assert.*;

import org.junit.Test;

import com.globo.galeb.core.bus.NullMap;

public class NullMapTest {

    private NullMap nullMap = new NullMap();

    @Test
    public void AddReturnFalse() {
        assertFalse(nullMap.add());
    }

    @Test
    public void DelReturnFalse() {
        assertFalse(nullMap.del());
    }

    @Test
    public void ResetReturnFalse() {
        assertFalse(nullMap.reset());
    }

    @Test
    public void ChangeReturnFalse() {
        assertFalse(nullMap.change());
    }

}
