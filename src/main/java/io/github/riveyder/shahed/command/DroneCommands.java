package io.github.riveyder.shahed.command;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.registry.ModItems;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class DroneCommands {
    private DroneCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("shahed")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("kit").executes(context -> giveKit(context.getSource())))
                        .then(Commands.literal("spawn").executes(context -> spawnDrone(context.getSource())))));
    }

    private static int giveKit(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireCreative(source, player)) {
            return 0;
        }

        for (Item item : new Item[]{ModItems.CONTROLLER, ModItems.FPV_GOGGLES, ModItems.DRONE}) {
            ItemStack stack = new ItemStack(item);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }

        source.sendSuccess(() -> Component.translatable("commands.shahed.kit"), false);
        return 1;
    }

    private static int spawnDrone(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireCreative(source, player)) {
            return 0;
        }

        double yaw = Math.toRadians(player.getYRot());
        Vec3 position = player.position().add(-Math.sin(yaw) * 2.5, 0.0, Math.cos(yaw) * 2.5);
        if (!DroneEntity.spawn(source.getLevel(), player, position)) {
            source.sendFailure(Component.translatable("commands.shahed.spawn_blocked"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("commands.shahed.spawned"), false);
        return 1;
    }

    private static boolean requireCreative(CommandSourceStack source, ServerPlayer player) {
        if (!player.getAbilities().instabuild) {
            source.sendFailure(Component.translatable("commands.shahed.creative_only"));
            return false;
        }
        return true;
    }
}
