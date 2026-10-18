package io.github.riveyder.shahed.client;

import io.github.riveyder.shahed.client.model.DroneModel;
import io.github.riveyder.shahed.client.renderer.DroneRenderer;
import io.github.riveyder.shahed.registry.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.renderer.entity.EntityRenderers;

/** Client entry point: nothing in the common source set loads a rendering class. */
public final class ShahedClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModelLayerRegistry.registerModelLayer(DroneModel.LAYER, DroneModel::createBodyLayer);
        EntityRenderers.register(ModEntities.DRONE, DroneRenderer::new);
        FpvController.initialize();
        DroneAudio.initialize();
        ReplayController.initialize();
        ServerSettingsClient.initialize();
    }
}
