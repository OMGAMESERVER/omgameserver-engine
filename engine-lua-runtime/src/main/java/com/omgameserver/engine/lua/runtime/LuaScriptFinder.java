package com.omgameserver.engine.lua.runtime;

import org.luaj.vm2.lib.ResourceFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * @author Kirill Byvshev (k@byv.sh)
 * @since 1.0.0
 */
class LuaScriptFinder implements ResourceFinder {
    static private final Logger logger = LoggerFactory.getLogger(LuaScriptFinder.class);

    @Override
    public InputStream findResource(String filename) {
        InputStream stream = getClass().getResourceAsStream("/" + filename);
        if (stream != null) {
            if (logger.isInfoEnabled()) {
                logger.info("Lua file {} loaded", filename);
            }
            return stream;
        } else {
            logger.error("Failed to find resource {}", filename);
            return null;
        }
    }
}
