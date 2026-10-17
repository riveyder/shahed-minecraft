package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.control.DroneConfig;
import io.github.riveyder.shahed.registry.ModEntities;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class DestructionGameTests {
    @GameTest
    public void destructionIsIdempotentAndDefaultExplosionPreservesBlocks(GameTestHelper helper) {
        var drone=helper.spawn(ModEntities.DRONE,3,40,3);
        var block=helper.absolutePos(new BlockPos(3,39,3));
        helper.getLevel().setBlockAndUpdate(block,Blocks.GLASS.defaultBlockState());
        try {
            helper.assertFalse(DroneConfig.DEFAULTS.destroyBlocks(),"Default must preserve blocks");
            helper.assertTrue(drone.destroy(),"First destruction must be accepted");
            helper.assertTrue(drone.isDestroyed() && drone.isRemoved(),"Server must mark and remove wreck");
            helper.assertFalse(drone.destroy(),"Duplicate destruction must not repeat effects");
            helper.assertFalse(drone.hurtServer(helper.getLevel(),drone.damageSources().generic(),100),
                    "Late damage must not repeat explosion");
            helper.assertTrue(helper.getLevel().getBlockState(block).is(Blocks.GLASS),
                    "Default explosion must preserve fragile nearby blocks");
            helper.assertTrue(new DroneConfig.Settings(true,Float.NaN).sanitized().explosionPower()==2,
                    "Non-finite power must use default");
            helper.succeed();
        } finally { drone.discard(); helper.getLevel().setBlockAndUpdate(block,Blocks.AIR.defaultBlockState()); }
    }

    @GameTest
    public void hardLandingDestroysButGentleContactSurvives(GameTestHelper helper) {
        var hard=helper.spawn(ModEntities.DRONE,3,41,3);
        io.github.riveyder.shahed.entity.DroneEntity soft=null;
        for(int x=-1;x<=7;x++) for(int z=-1;z<=7;z++) helper.setBlock(x,40,z,Blocks.STONE);
        try {
            // Bottom is one block above the floor; use a short fall with a fast impact.
            hard.setDeltaMovement(new Vec3(0,-.8,0));
            for(int i=0;i<3 && !hard.isRemoved();i++) hard.tick();
            helper.assertTrue(hard.isDestroyed(),"High impact must destroy the drone");
            soft=helper.spawn(ModEntities.DRONE,3,41,3);
            soft.setDeltaMovement(new Vec3(0,-.04,0));
            soft.tick();
            helper.assertFalse(soft.isRemoved(),"Gentle ground contact must survive");
            helper.succeed();
        } finally { hard.discard();if(soft!=null) soft.discard(); }
    }
}
