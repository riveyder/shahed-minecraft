package io.github.riveyder.shahed.compat.client;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.resources.ResourceLocation;
public final class HudElementRegistry {
 public static void addLast(ResourceLocation id,java.util.function.BiConsumer<GuiGraphicsExtractor,Float> render){HudRenderCallback.EVENT.register((g,t)->render.accept(new GuiGraphicsExtractor(g),t.getGameTimeDeltaPartialTick(false)));}
}
