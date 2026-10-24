package io.github.riveyder.shahed.control;

import com.mojang.serialization.MapCodec;
import io.github.riveyder.shahed.ShahedMod;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Real contact fire with persisted age, no spreading, no portal creation and a finite lifetime. */
public final class CrashFireBlock extends BaseFireBlock {
    public static final IntegerProperty AGE=IntegerProperty.create("age",0,15);
    public static final MapCodec<CrashFireBlock> CODEC=simpleCodec(CrashFireBlock::new);
    public static final CrashFireBlock INSTANCE=Registry.register(BuiltInRegistries.BLOCK,ShahedMod.id("crash_fire"),
            new CrashFireBlock(BlockBehaviour.Properties.of().setId(ResourceKey.create(Registries.BLOCK,ShahedMod.id("crash_fire")))
                    .noCollision().instabreak().noLootTable().replaceable().lightLevel(s->15)));
    public CrashFireBlock(BlockBehaviour.Properties properties) {super(properties,1);registerDefaultState(stateDefinition.any().setValue(AGE,0));}
    @Override protected MapCodec<CrashFireBlock> codec() {return CODEC;}
    @Override protected boolean canBurn(BlockState state) {return false;}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) {builder.add(AGE);}
    @Override protected void onPlace(BlockState state,Level level,BlockPos pos,BlockState old,boolean moved) {
        if(!level.isClientSide())level.scheduleTick(pos,this,40);
    }
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random) {
        if(state.getValue(AGE)>=14||level.isRainingAt(pos)||!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)) {
            level.removeBlock(pos,false);return;
        }
        level.setBlock(pos,state.setValue(AGE,state.getValue(AGE)+1),Block.UPDATE_CLIENTS);
        level.scheduleTick(pos,this,40);
    }
    public static void initialize() { }
}
