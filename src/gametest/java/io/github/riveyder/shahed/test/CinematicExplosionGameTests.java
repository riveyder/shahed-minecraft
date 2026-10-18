package io.github.riveyder.shahed.test;

import io.github.riveyder.shahed.control.*;
import io.github.riveyder.shahed.network.ExplosionEffectPayload;
import io.github.riveyder.shahed.ShahedMod;
import io.netty.buffer.Unpooled;
import java.nio.*;
import java.util.*;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public final class CinematicExplosionGameTests {
    @GameTest(maxTicks=200)
    public void livePresetsProduceLargerBoundedCratersInSolidStone(GameTestHelper h) {
        var original=DroneConfig.get();var owner=(ServerPlayer)h.makeMockServerPlayer(GameType.CREATIVE);
        var center=h.absolutePos(new BlockPos(3,151,3));var blast=Vec3.atCenterOf(center).add(0,.51,0);
        int[] broken=new int[4];
        try {
            for(int mode=0;mode<4;mode++) {
                for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-9;y<=0;y++)
                    h.getLevel().setBlock(center.offset(x,y,z),Blocks.STONE.defaultBlockState(),2);
                h.getLevel().getRandom().setSeed(410); // identical vanilla ray randomization for all policies
                if(mode==0) new ServerExplosion(h.getLevel(),null,null,null,blast,12,false,Explosion.BlockInteraction.DESTROY).explode();
                else {
                    h.assertTrue(DroneConfig.update(original.preset(mode-1)),"Policy must be persisted and become live");
                    DroneExplosions.explode(h.getLevel(),null,owner,blast,DroneConfig.get());
                }
                for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-9;y<=0;y++)
                    if(h.getLevel().getBlockState(center.offset(x,y,z)).isAir())broken[mode]++;
            }
            ShahedMod.LOGGER.info("Stone crater benchmark: vanilla12={}, SAFE={}, NORMAL={}, CHAOS={}",broken[0],broken[1],broken[2],broken[3]);
            h.assertTrue(broken[1]==0,"SAFE must preserve terrain");
            h.assertTrue(!original.preset(0).entityDamage(),"SAFE must be audiovisual only");
            h.assertTrue(broken[2]>0&&broken[2]<=384,"NORMAL must break terrain within its cap");
            h.assertTrue(broken[3]>200&&broken[3]>broken[2]*2&&broken[3]>broken[0]*2&&broken[3]<=3072,
                    "CHAOS must be substantially larger than NORMAL and old vanilla power 12: "+Arrays.toString(broken));
        } finally {
            DroneConfig.update(original);
            for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-9;y<=0;y++)h.getLevel().setBlock(center.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
        }
        h.succeed();
    }
    @GameTest
    public void audiovisualPacketPreservesIdentityPositionAndIndependentFlags(GameTestHelper h) {
        var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        try {
            for(boolean particles:new boolean[]{false,true})for(boolean sound:new boolean[]{false,true}) {
                var p=new ExplosionEffectPayload(UUID.randomUUID(),new Vec3(123,64,-321),871234,particles,sound);
                ExplosionEffectPayload.CODEC.encode(b,p);
                h.assertTrue(p.equals(ExplosionEffectPayload.CODEC.decode(b))&&!b.isReadable(),"One compact event must round-trip without losing toggle or identity");
                try(var probe=new CrashEffectsProbe()) {
                    DroneExplosions.explode(h.getLevel(),null,null,Vec3.atCenterOf(h.absolutePos(new BlockPos(2,190,2))),
                            new DroneConfig.Settings(false,1,false,0,particles,sound,0));
                    h.assertTrue(probe.effects.size()==(particles||sound?1:0),"Only enabled AV should be dispatched");
                    if(!probe.effects.isEmpty())h.assertTrue(probe.effects.getFirst().particles()==particles&&probe.effects.getFirst().sound()==sound,"Server flags must reach event unchanged");
                }
            }
            h.assertFalse(new ExplosionEffectPayload(UUID.randomUUID(),new Vec3(Double.NaN,0,0),0,true,true).valid(),"Reject invalid effect coordinates");
        } finally {b.release();}h.succeed();
    }
    @GameTest
    public void packagedAudioUsesValidMono48KhzVorbisAndParticleSpritesResolve(GameTestHelper h) {
        try {
            for(String name:List.of("engine","startup","shutdown","wind","impact","blast","blast_echo")) {
                byte[] bytes=resource("sounds/"+name+".ogg");
                h.assertTrue(bytes.length>1000&&bytes[0]=='O'&&bytes[1]=='g'&&bytes[2]=='g'&&bytes[3]=='S',"Valid Ogg container: "+name);
                int marker=-1;for(int i=0;i<Math.min(100,bytes.length-16);i++)if(bytes[i]==1&&new String(bytes,i+1,6,java.nio.charset.StandardCharsets.US_ASCII).equals("vorbis")){marker=i;break;}
                h.assertTrue(marker>=0&&bytes[marker+11]==1&&ByteBuffer.wrap(bytes,marker+12,4).order(ByteOrder.LITTLE_ENDIAN).getInt()==48000,
                        "Positional sound must be mono at 48kHz: "+name);
            }
            for(String name:List.of("fireball","cinematic_smoke","shockwave","debris")) {
                var json=com.google.gson.JsonParser.parseString(new String(resource("particles/"+name+".json"),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                for(var frame:json.getAsJsonArray("textures")) {
                    byte[] png=resource("textures/particle/"+frame.getAsString().split(":")[1]+".png");
                    h.assertTrue(png.length>40&&png[0]==(byte)137&&png[1]=='P',"Particle sprite must resolve to PNG");
                }
            }
        } catch(Exception e) {throw new AssertionError(e);}h.succeed();
    }
    private byte[] resource(String name) throws java.io.IOException {
        try(var stream=getClass().getResourceAsStream("/assets/shahed/"+name)) {if(stream==null)throw new AssertionError("Missing "+name);return stream.readAllBytes();}
    }
}
