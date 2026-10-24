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
        ModelLayerRegistry.registerModelLayer(io.github.riveyder.shahed.client.model.FleetModel.QUAD,io.github.riveyder.shahed.client.model.FleetModel::quadLayer);
        ModelLayerRegistry.registerModelLayer(io.github.riveyder.shahed.client.model.FleetModel.CONTACT,io.github.riveyder.shahed.client.model.FleetModel::contactLayer);
        ModelLayerRegistry.registerModelLayer(io.github.riveyder.shahed.client.model.FleetModel.JET,io.github.riveyder.shahed.client.model.FleetModel::jetLayer);
        ModelLayerRegistry.registerModelLayer(io.github.riveyder.shahed.client.model.PayloadModel.LAYER,io.github.riveyder.shahed.client.model.PayloadModel::layer);
        EntityRenderers.register(ModEntities.QUAD,c->new DroneRenderer(c,io.github.riveyder.shahed.entity.AircraftKind.QUAD));
        EntityRenderers.register(ModEntities.CONTACT_QUAD,c->new DroneRenderer(c,io.github.riveyder.shahed.entity.AircraftKind.CONTACT));
        EntityRenderers.register(ModEntities.JET,c->new DroneRenderer(c,io.github.riveyder.shahed.entity.AircraftKind.JET));
        EntityRenderers.register(ModEntities.PAYLOAD,io.github.riveyder.shahed.client.renderer.PayloadRenderer::new);
        FpvController.initialize();
        DroneAudio.initialize();
        MechanicalAudio.initialize();
        ReplayController.initialize();
        ServerSettingsClient.initialize();
        ExplosionEffects.initialize();
        RaceClient.initialize();
    }
}
