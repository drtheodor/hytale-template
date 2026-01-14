package com.example.hytaletemplate;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import javax.annotation.Nonnull;
import java.util.logging.Level;

public class HytaleTemplatePlugin extends JavaPlugin {

    public static HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public HytaleTemplatePlugin(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        LOGGER.at(Level.INFO).log("Hello from the template!");
    }
}
