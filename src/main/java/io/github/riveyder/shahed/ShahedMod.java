package io.github.riveyder.shahed;

import io.github.riveyder.shahed.command.DroneCommands;
import io.github.riveyder.shahed.registry.ModEntities;
import io.github.riveyder.shahed.registry.ModItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ShahedMod implements ModInitializer {
    public static final String MOD_ID = "shahed";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModEntities.initialize();
        ModItems.initialize();
        DroneCommands.register();
        LOGGER.info("SHAHED.exe stage 1 initialized: equipment and static drone prototype.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
