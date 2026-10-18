package io.github.riveyder.shahed.control;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/** Shared gate for item and command deployment. Existing flights are not interrupted by policy changes. */
public final class DroneDeployment {
    public static String denial(Player player) {
        if(!player.isAlive()||player.isSpectator()||!player.mayBuild())return "message.shahed.deployment.invalid_player";
        var policy=DroneConfig.get();
        if(!policy.deploymentEnabled())return "message.shahed.deployment.disabled";
        if(policy.deploymentAdminOnly()&&(!(player instanceof ServerPlayer p)||!DroneSettingsAuthority.canEdit(p)))
            return "message.shahed.deployment.admin_only";
        return null;
    }
    private DroneDeployment() {}
}
