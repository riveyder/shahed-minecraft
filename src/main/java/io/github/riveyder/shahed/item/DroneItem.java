package io.github.riveyder.shahed.item;

import io.github.riveyder.shahed.entity.DroneEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.Vec3;

/** Places a static prototype beside the clicked block; it never starts a flight session. */
public final class DroneItem extends Item {
    public DroneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        if (!(context.getLevel() instanceof ServerLevel level)) {
            return player.getAbilities().instabuild ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }

        if (!player.getAbilities().instabuild) {
            player.sendOverlayMessage(Component.translatable("commands.shahed.creative_only"));
            return InteractionResult.FAIL;
        }

        BlockPos position = context.getClickedPos().relative(context.getClickedFace());
        if (!level.mayInteract(player, context.getClickedPos())
                || !player.mayUseItemAt(position, context.getClickedFace(), context.getItemInHand())
                || !DroneEntity.spawn(level, player, Vec3.atBottomCenterOf(position))) {
            player.sendOverlayMessage(Component.translatable("commands.shahed.spawn_blocked"));
            return InteractionResult.FAIL;
        }

        player.sendOverlayMessage(Component.translatable("commands.shahed.spawned"));
        return InteractionResult.SUCCESS;
    }
}
