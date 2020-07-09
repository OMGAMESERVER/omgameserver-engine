package com.omgameserver.engine.lua;

import org.luaj.vm2.lib.ResourceFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

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
