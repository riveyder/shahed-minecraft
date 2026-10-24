package io.github.riveyder.shahed.item;

import io.github.riveyder.shahed.control.DroneSessions;
import io.github.riveyder.shahed.entity.DroneEntity;
import io.github.riveyder.shahed.registry.ModItems;
import java.util.Optional;
import java.util.UUID;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** A binding stores both identities; a copied controller never grants another player ownership. */
public final class ControllerItem extends Item {
    private static final String DRONE_KEY = "ShahedDrone";
    private static final String OWNER_KEY = "ShahedOwner";
    private static final String DESTROYED_KEY = "ShahedDestroyed";

    public ControllerItem(Properties properties) {
        super(properties);
    }

    public static void registerInteractions() {
        UseEntityCallback.EVENT.register((player, level, hand, target, hit) -> {
            ItemStack stack = player.getItemInHand(hand);
            if(target instanceof DroneEntity aircraft && (stack.is(ModItems.PAYLOAD)||stack.isEmpty()&&player.isShiftKeyDown())){
                if(!(player instanceof ServerPlayer pilot))return InteractionResult.SUCCESS;
                if(stack.is(ModItems.PAYLOAD)){
                    boolean ok=aircraft.attachPayload(pilot,stack);
                    message(pilot,Component.translatable(ok?"fleet.payload.attached":"fleet.payload.attach_denied"));
                    return ok?InteractionResult.SUCCESS:InteractionResult.FAIL;
                }
                if(aircraft.isRaceSafe()){
                    boolean ok=aircraft.rearm(pilot);message(pilot,Component.translatable(ok?"fleet.armed":"fleet.arm_denied"));
                    return ok?InteractionResult.SUCCESS:InteractionResult.FAIL;
                }
            }
            if (!(target instanceof DroneEntity drone) || !stack.is(ModItems.CONTROLLER)) {
                return InteractionResult.PASS;
            }
            if (player instanceof ServerPlayer serverPlayer) {
                if (DroneSessions.isControlling(serverPlayer)) {
                    return InteractionResult.FAIL;
                }
                if(player.isShiftKeyDown()) {clearBinding(stack);message(serverPlayer,Component.translatable("message.shahed.controller.cleared"));return InteractionResult.SUCCESS;}
                return bind(serverPlayer, drone, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
            }
            return InteractionResult.SUCCESS;
        });
    }

    public static boolean bind(ServerPlayer player, DroneEntity drone, ItemStack stack) {
        if (!stack.is(ModItems.CONTROLLER) || drone.isRemoved() || drone.isDestroyed()
                || !player.isAlive() || player.isSpectator() || player.serverLevel()!=drone.level() || DroneSessions.isControlling(player)
                || !drone.getOwnerUuid().filter(player.getUUID()::equals).isPresent()) {
            message(player, Component.translatable("message.shahed.controller.not_owner"));
            return false;
        }
        { var tag=stack.getOrCreateTag();
            tag.remove(DESTROYED_KEY);
            tag.putString(DRONE_KEY, drone.getUUID().toString());
            tag.putString(OWNER_KEY, player.getUUID().toString());
        }
        message(player, Component.translatable("message.shahed.controller.bound", shortId(drone.getUUID())));
        return true;
    }

    public static void clearBinding(ItemStack stack) {
        { var tag=stack.getOrCreateTag();tag.remove(DRONE_KEY);tag.remove(OWNER_KEY);tag.remove(DESTROYED_KEY);}
    }

    /** Only a confirmed server destruction marks inventory controllers; unloaded entities are never guessed dead. */
    public static void markDestroyed(ServerPlayer player,UUID droneId) {
        var inventory=player.getInventory();
        for(int i=0;i<inventory.getContainerSize();i++) markDestroyed(inventory.getItem(i),droneId);
        // Equipment storage may be separate from the main inventory in newer Minecraft versions.
        markDestroyed(player.getOffhandItem(),droneId);
    }
    private static void markDestroyed(ItemStack stack,UUID droneId) {
        if(getBoundDrone(stack).filter(droneId::equals).isPresent())
            stack.getOrCreateTag().putBoolean(DESTROYED_KEY,true);
    }

    public static Optional<UUID> getBoundDrone(ItemStack stack) {
        return readUuid(stack, DRONE_KEY);
    }

    public static Optional<UUID> getBoundOwner(ItemStack stack) {
        return readUuid(stack, OWNER_KEY);
    }

    private static Optional<UUID> readUuid(ItemStack stack, String key) {
        if (!stack.is(ModItems.CONTROLLER)) {
            return Optional.empty();
        }
        return java.util.Optional.ofNullable(stack.getTag()).map(tag->tag.getString(key))
                .flatMap(value -> {
                    try {
                        return Optional.of(UUID.fromString(value));
                    } catch (IllegalArgumentException exception) {
                        return Optional.empty();
                    }
                });
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return new net.minecraft.world.InteractionResultHolder<>(useController(level,player,hand),player.getItemInHand(hand));
    }
    private InteractionResult useController(Level level,Player player,InteractionHand hand) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        if (DroneSessions.isControlling(serverPlayer)) {
            DroneSessions.end(serverPlayer, "exit");
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if(player.isShiftKeyDown()) {
            clearBinding(stack);message(serverPlayer,Component.translatable("message.shahed.controller.cleared"));return InteractionResult.SUCCESS;
        }
        if(getBoundOwner(stack).filter(id->!id.equals(player.getUUID())).isPresent()) {
            message(serverPlayer,Component.translatable("message.shahed.controller.wrong_owner"));return InteractionResult.FAIL;
        }
        if(stack.hasTag()&&stack.getTag().getBoolean(DESTROYED_KEY)) {
            message(serverPlayer,Component.translatable("message.shahed.controller.destroyed"));return InteractionResult.FAIL;
        }
        Optional<UUID> droneUuid = getBoundDrone(stack);
        if (droneUuid.isEmpty()) {
            message(serverPlayer, Component.translatable("message.shahed.controller.not_bound"));
            return InteractionResult.FAIL;
        }
        if (!(serverPlayer.serverLevel().getEntity(droneUuid.get()) instanceof DroneEntity drone)) {
            message(serverPlayer, Component.translatable("message.shahed.controller.drone_unavailable"));
            return InteractionResult.FAIL;
        }
        return DroneSessions.connect(serverPlayer, drone, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return context.getPlayer() == null ? InteractionResult.FAIL
                : useController(context.getLevel(), context.getPlayer(), context.getHand());
    }

    private static String shortId(UUID uuid) {
        return uuid.toString().substring(0, 8);
    }

    private static void message(ServerPlayer player, Component component) {
        if (player.connection != null) {
            player.displayClientMessage(component,true);
        }
    }
}
