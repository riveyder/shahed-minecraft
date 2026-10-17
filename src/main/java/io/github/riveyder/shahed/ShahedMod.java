package io.github.riveyder.shahed;

import io.github.riveyder.shahed.command.DroneCommands;
import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.item.ControllerItem;
import io.github.riveyder.shahed.network.DroneNetworking;
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
        DroneNetworking.register();
        DroneSessions.register();
        ControllerItem.registerInteractions();
        DroneCommands.register();
        LOGGER.info("SHAHED.exe initialized: server-authoritative FPV flight and equipment.");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
