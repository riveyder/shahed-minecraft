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
                        .then(Commands.literal("race")
                                .then(Commands.literal("add").executes(c->{int count=io.github.riveyder.shahed.control.DroneRacing.add(c.getSource().getPlayerOrException());c.getSource().sendSuccess(()->Component.literal("Контрольных точек: "+count),false);return count;}))
                                .then(Commands.literal("clear").executes(c->{io.github.riveyder.shahed.control.DroneRacing.clear(c.getSource().getLevel());return 1;}))
                                .then(Commands.literal("start").executes(c->io.github.riveyder.shahed.control.DroneRacing.start(c.getSource().getPlayerOrException())?1:0)))
                        .then(Commands.literal("viewers").then(Commands.argument("allowed",com.mojang.brigadier.arguments.BoolArgumentType.bool())
                                .executes(context->viewers(context.getSource(),com.mojang.brigadier.arguments.BoolArgumentType.getBool(context,"allowed")))))
                        .then(Commands.literal("watch").then(Commands.argument("owner",net.minecraft.commands.arguments.EntityArgument.player())
                                .executes(context->watch(context.getSource(),net.minecraft.commands.arguments.EntityArgument.getPlayer(context,"owner")))))
                        .then(Commands.literal("training").executes(context -> spawnDrone(context.getSource(),true)))
                        .then(Commands.literal("paint").then(Commands.argument("color",com.mojang.brigadier.arguments.IntegerArgumentType.integer(0,3))
                                .executes(context->paint(context.getSource(),com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context,"color")))))
                        .then(Commands.literal("spawn").executes(context -> spawnDrone(context.getSource(),false)))));
    }

    private static int giveKit(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireCreative(source, player)) {
            return 0;
        }

        for (Item item : new Item[]{ModItems.CONTROLLER, ModItems.FPV_GOGGLES, ModItems.DRONE,ModItems.TRAINING_DRONE}) {
            ItemStack stack = new ItemStack(item);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
        }

        source.sendSuccess(() -> Component.translatable("commands.shahed.kit"), false);
        return 1;
    }

    private static int spawnDrone(CommandSourceStack source,boolean training) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!requireCreative(source, player)) {
            return 0;
        }

        double yaw = Math.toRadians(player.getYRot());
        Vec3 position = player.position().add(-Math.sin(yaw) * 3.5, 0.0, Math.cos(yaw) * 3.5);
        if (!DroneEntity.spawn(source.getLevel(), player, position,training)) {
            source.sendFailure(Component.translatable("commands.shahed.spawn_blocked"));
            return 0;
        }

        source.sendSuccess(() -> Component.translatable("commands.shahed.spawned"), false);
        return 1;
    }

    private static int viewers(CommandSourceStack source,boolean allowed) throws CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        var id=io.github.riveyder.shahed.item.ControllerItem.getBoundDrone(player.getMainHandItem());
        if(id.isEmpty()||!(player.level().getEntity(id.get()) instanceof DroneEntity drone)
                ||!drone.getOwnerUuid().filter(player.getUUID()::equals).isPresent()) return 0;
        drone.allowSpectators(allowed);
        source.sendSuccess(()->Component.literal(allowed?"Просмотр камеры разрешён.":"Просмотр камеры запрещён."),false);
        return 1;
    }
    private static int watch(CommandSourceStack source,ServerPlayer owner) throws CommandSyntaxException {
        var id=io.github.riveyder.shahed.control.DroneSessions.controlledDrone(owner);
        if(id.isEmpty()||!(owner.level().getEntity(id.get()) instanceof DroneEntity drone)
                ||!drone.getOwnerUuid().filter(owner.getUUID()::equals).isPresent()) return 0;
        return io.github.riveyder.shahed.control.DroneSessions.spectate(source.getPlayerOrException(),drone)?1:0;
    }

    private static int paint(CommandSourceStack source,int color) throws CommandSyntaxException {
        ServerPlayer player=source.getPlayerOrException();
        var id=io.github.riveyder.shahed.item.ControllerItem.getBoundDrone(player.getMainHandItem());
        if(id.isEmpty() || !(player.level().getEntity(id.get()) instanceof DroneEntity drone)
                || !drone.getOwnerUuid().filter(player.getUUID()::equals).isPresent()) return 0;
        drone.setPaint(color);
        source.sendSuccess(()->Component.literal("Окраска обновлена. КРИВЫЕ РУКИ AEROSPACE"),false);
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
