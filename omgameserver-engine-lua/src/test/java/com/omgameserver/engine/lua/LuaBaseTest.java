package com.omgameserver.engine.lua;

import java.net.UnknownHostException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaBaseTest extends BaseServiceTest {

    protected LuaGlobals luaGlobals;

    @Override
    protected void createComponents(String mainScript) throws UnknownHostException {
        super.createComponents(mainScript);
        luaGlobals = new LuaGlobals();
        luaGlobals.getGlobals().set("testing", new LuaTesting(coreDispatcher));
    }
}
