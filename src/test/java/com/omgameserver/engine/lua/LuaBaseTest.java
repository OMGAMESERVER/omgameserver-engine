package com.omgameserver.engine.lua;

import com.omgameserver.engine.BaseServiceTest;

import java.net.UnknownHostException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
public class LuaBaseTest extends BaseServiceTest {

    protected LuaGlobals luaGlobals;

    @Override
    protected void createComponents() throws UnknownHostException {
        super.createComponents();
        luaGlobals = new LuaGlobals();
        luaGlobals.getGlobals().set("testing", new LuaTesting(dispatcher));
    }
}
