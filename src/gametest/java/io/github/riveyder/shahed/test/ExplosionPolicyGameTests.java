package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.control.*;
import io.github.riveyder.shahed.registry.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.Vec3;

public final class ExplosionPolicyGameTests {
    private static BlockPos protectedBlock;
    static {PlayerBlockBreakEvents.BEFORE.register((level,player,pos,state,entity)->!pos.equals(protectedBlock));}
    @GameTest
    public void terrainToggleAndLimitControlActualVanillaDestruction(GameTestHelper h) {
        var owner=(ServerPlayer)h.makeMockServerPlayer(GameType.CREATIVE);
        for(boolean enabled:new boolean[]{false,true}) {
            for(int x=0;x<7;x++) for(int z=0;z<7;z++) h.setBlock(x,90,z,Blocks.GLASS);
            Vec3 center=Vec3.atCenterOf(h.absolutePos(new BlockPos(3,91,3)));
            try(var effects=new CrashEffectsProbe()) {
                DroneExplosions.explode(h.getLevel(),null,owner,center,new DroneConfig.Settings(enabled,8,false,0,true,true,5));
                int missing=0;for(int x=0;x<7;x++) for(int z=0;z<7;z++)
                    if(h.getLevel().getBlockState(h.absolutePos(new BlockPos(x,90,z))).isAir()) missing++;
                h.assertTrue(enabled?missing>0&&missing<=5:missing==0,"Actual broken blocks must follow toggle and hard limit: "+missing);
                h.assertTrue(effects.explosions==1&&effects.effects.size()==1&&effects.effects.getFirst().particles()&&effects.effects.getFirst().sound(),"AV event must not depend on terrain toggle");
            }
        }
        h.succeed();
    }
    @GameTest
    public void protectionGameruleAndTrainingPreventTerrainDamage(GameTestHelper h) {
        var owner=(ServerPlayer)h.makeMockServerPlayer(GameType.CREATIVE);
        var pos=h.absolutePos(new BlockPos(3,90,3));var center=Vec3.atCenterOf(pos);
        var rules=h.getLevel().getGameRules();boolean old=rules.get(GameRules.TNT_EXPLODES);
        try {
            h.setBlock(3,90,3,Blocks.GLASS);protectedBlock=pos;
            DroneExplosions.explode(h.getLevel(),null,owner,center,new DroneConfig.Settings(true,8));
            h.assertBlockPresent(Blocks.GLASS,3,90,3);
            protectedBlock=null;rules.set(GameRules.TNT_EXPLODES,false,h.getLevel().getServer());
            DroneExplosions.explode(h.getLevel(),null,owner,center,new DroneConfig.Settings(true,8));
            h.assertBlockPresent(Blocks.GLASS,3,90,3);
            rules.set(GameRules.TNT_EXPLODES,true,h.getLevel().getServer());
            var drone=h.spawn(ModEntities.DRONE,3,95,3);drone.setTraining(true);
            try {DroneExplosions.explode(h.getLevel(),drone,owner,center,new DroneConfig.Settings(true,12));
                h.assertBlockPresent(Blocks.GLASS,3,90,3);} finally {drone.discard();}
            h.succeed();
        } finally {protectedBlock=null;rules.set(GameRules.TNT_EXPLODES,old,h.getLevel().getServer());}
    }
    @GameTest
    public void entityDamageParticlesAndSoundAreIndependent(GameTestHelper h) {
        var target=h.spawn(net.minecraft.world.entity.EntityTypes.COW,3,90,3);target.setNoAi(true);
        try {
            float health=target.getHealth();
            try(var effects=new CrashEffectsProbe()) {
                DroneExplosions.explode(h.getLevel(),null,null,target.position().add(1,0,0),new DroneConfig.Settings(false,8,false,1,false,false,0));
                h.assertTrue(target.getHealth()==health&&target.getDeltaMovement().lengthSqr()==0,"Damage toggle must disable damage and knockback");
                h.assertTrue(effects.blastSounds==0&&effects.smokeParticles==0&&effects.explosionParticles==0,"Effects must obey independent toggles");
                h.assertTrue(effects.effects.isEmpty(),"Disabled AV must not emit a cinematic event");
            }
            DroneExplosions.explode(h.getLevel(),null,null,target.position().add(1,0,0),new DroneConfig.Settings(false,8,true,1,false,false,0));
            h.assertTrue(target.getHealth()<health,"Enabled vanilla entity damage must apply");h.succeed();
        } finally {target.discard();}
    }
}
