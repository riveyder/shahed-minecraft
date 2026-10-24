package io.github.riveyder.shahed.compat;
import net.minecraft.resources.ResourceLocation;
public interface CustomPacketPayload { Type<? extends CustomPacketPayload> type(); record Type<T extends CustomPacketPayload>(ResourceLocation id){} }
