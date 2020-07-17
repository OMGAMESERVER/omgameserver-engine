package com.omgameserver.engine.lua;

import org.luaj.vm2.Globals;

import java.net.UnknownHostException;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaBaseTest extends BaseServiceTest {

    protected LuaGlobalsFactory luaGlobalsFactory;

    @Override
    protected void createComponents(String mainScript) throws UnknownHostException {
        super.createComponents(mainScript);
        luaGlobalsFactory = new LuaTestingGlobalsFactory();
    }

    class LuaTestingGlobalsFactory extends LuaGlobalsFactory {

        @Override
        Globals createGlobals() {
            Globals globals = super.createGlobals();
            globals.set("testing", new LuaTesting(coreDispatcher));
            return globals;
        }
    }
}
