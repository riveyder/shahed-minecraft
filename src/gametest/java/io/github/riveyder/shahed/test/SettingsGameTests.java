package io.github.riveyder.shahed.test;

import com.mojang.authlib.GameProfile;
import io.github.riveyder.shahed.control.*;
import io.github.riveyder.shahed.network.*;
import io.netty.buffer.Unpooled;
import java.nio.file.*;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.*;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.core.BlockPos;

public final class SettingsGameTests {
    @GameTest
    public void chaosRequiresExplicitBoundConfirmationEvenForAnAdministrator(GameTestHelper h) {
        var original=DroneConfig.get();var admin=new Admin(h);
        try {
            var candidate=original.preset(2);
            var pending=DroneSettingsAuthority.handle(admin,request(admin,1,candidate,SettingsRequestPayload.NONE));
            h.assertTrue(pending.confirmationAction()==6&&!pending.nonce().equals(SettingsRequestPayload.NONE),"CHAOS must issue a dedicated confirmation");
            h.assertTrue(DroneConfig.get().equals(original),"Preparing CHAOS must not apply it");
            admin.allowed=false;
            h.assertTrue(DroneSettingsAuthority.handle(admin,request(admin,6,candidate,pending.nonce())).status().contains("администратора"),"Recheck authority at confirmation");
            admin.allowed=true;
            // The candidate is server-bound: even another policy in the confirm packet cannot replace it.
            var confirmed=DroneSettingsAuthority.handle(admin,request(admin,6,original.preset(0),pending.nonce()));
            h.assertTrue(confirmed.settings().equals(candidate)&&DroneConfig.get().equals(candidate),"Exact confirmed policy must become live");
            var oldRevision=confirmed.revision();
            var repeat=DroneSettingsAuthority.handle(admin,request(admin,6,original,pending.nonce()));
            h.assertTrue(repeat.revision()==oldRevision,"Consumed token cannot apply policy twice");
        } finally {h.assertTrue(DroneConfig.update(original),"Restore policy");}
        h.succeed();
    }
    private static final class Admin extends ServerPlayer {
        boolean allowed=true;
        Admin(GameTestHelper h) {super(h.getLevel().getServer(),h.getLevel(),new GameProfile(UUID.randomUUID(),"PolicyTest"),ClientInformation.createDefault());}
        @Override public PermissionSet permissions() {return allowed?PermissionSet.ALL_PERMISSIONS:PermissionSet.NO_PERMISSIONS;}
    }
    private SettingsRequestPayload request(ServerPlayer p,int action,DroneConfig.Settings s,UUID nonce) {
        return new SettingsRequestPayload(action,DroneSettingsAuthority.snapshot(p,"",SettingsRequestPayload.NONE).revision(),s,nonce);
    }
    @GameTest
    public void permissionsRevisionsAndSanitizationProtectLiveSettings(GameTestHelper h) {
        var before=DroneConfig.get();
        try {
            for(int action=1;action<=4;action++) {
                var ordinary=new Admin(h);ordinary.allowed=false;
                var response=DroneSettingsAuthority.handle(ordinary,request(ordinary,action,before.preset(2),UUID.randomUUID()));
                h.assertTrue(response.status().contains("администратора") && !response.admin(),"All mutations require server permissions");
                h.assertTrue(before.equals(DroneConfig.get()),"Unauthorized requests cannot alter policy");
            }
            var stale=new Admin(h);
            var response=DroneSettingsAuthority.handle(stale,new SettingsRequestPayload(1,-1,before.preset(2),SettingsRequestPayload.NONE));
            h.assertTrue(response.status().contains("изменились")&&before.equals(DroneConfig.get()),"Stale revisions must not overwrite live policy");
            var admin=new Admin(h);
            var invalid=new DroneConfig.Settings(false,Float.NaN,true,Float.POSITIVE_INFINITY,true,true,Integer.MAX_VALUE,false,Float.NaN,0,true,999);
            response=DroneSettingsAuthority.handle(admin,request(admin,1,invalid,SettingsRequestPayload.NONE));
            h.assertTrue(response.status().contains("сохранены"),"Admin changes must persist and publish");
            var safe=DroneConfig.get();
            h.assertTrue(safe.explosionPower()==19&&safe.damageScale()==.25F&&safe.maxBlocks()==16384&&safe.dragMultiplier()==1
                    &&safe.activeLimit()==1&&safe.telemetryInterval()==20,"Invalid values must be bounded at authority boundary");
            h.assertTrue(DroneSettingsAuthority.handle(admin,request(admin,1,before,SettingsRequestPayload.NONE)).status().contains("часто"),"Rapid writes must be throttled");
        } finally {h.assertTrue(DroneConfig.update(before),"Restore test policy");}
        h.succeed();
    }
    @GameTest
    public void presetsMigrationPersistenceAndWireRoundTrip(GameTestHelper h) {
        Path file=Path.of("settings-regression.json");
        try {
            Files.writeString(file,"{\"destroyBlocks\":false,\"explosionPower\":2}");
            var old=DroneConfig.read(file);
            h.assertTrue(old.explosionPower()==2&&old.activeLimit()==16&&old.telemetryInterval()==5,"Existing values survive missing-field migration");
            for(int i=0;i<3;i++) {
                var policy=old.preset(i);DroneConfig.write(file,policy);
                h.assertTrue(DroneConfig.read(file).equals(policy),"Policy must persist without restart");
                h.assertTrue(i==0?!policy.destroyBlocks()&&policy.damageScale()<1:policy.destroyBlocks()&&policy.maxBlocks()>0,"Presets must implement actual gameplay differences");
                var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
                try {
                    var outgoing=new SettingsRequestPayload(1,41,policy,UUID.randomUUID());
                    var incoming=new SettingsStatePayload(42,policy,true,"Сохранено",UUID.randomUUID(),"Точка");
                    SettingsRequestPayload.CODEC.encode(buffer,outgoing);SettingsStatePayload.CODEC.encode(buffer,incoming);
                    h.assertTrue(outgoing.equals(SettingsRequestPayload.CODEC.decode(buffer))&&incoming.equals(SettingsStatePayload.CODEC.decode(buffer))
                            &&!buffer.isReadable(),"Complete settings/permissions/revision/nonce wire round trip");
                } finally {buffer.release();}
            }
        } catch(Exception e) {throw new AssertionError(e);}
        finally {try {Files.deleteIfExists(file);} catch(Exception ignored) {}}
        h.succeed();
    }
    @GameTest(maxTicks=100)
    public void testExplosionRequiresDesignationFreshConfirmationAndCurrentPermission(GameTestHelper h) {
        var admin=new Admin(h);var pos=h.absolutePos(new BlockPos(2,100,2));admin.setPos(pos.getX(),pos.getY(),pos.getZ());
        var noArea=new Admin(h);
        h.assertTrue(DroneSettingsAuthority.handle(noArea,request(noArea,3,DroneConfig.get(),SettingsRequestPayload.NONE)).nonce().equals(SettingsRequestPayload.NONE),"Cannot prepare without a designated area");
        h.assertTrue(DroneSettingsAuthority.handle(admin,request(admin,2,DroneConfig.get(),SettingsRequestPayload.NONE)).status().contains("назначена"),"Admin designates current position");
        UUID[] nonce={null};
        h.runAtTickTime(12,()->{
            nonce[0]=DroneSettingsAuthority.handle(admin,request(admin,3,DroneConfig.get(),SettingsRequestPayload.NONE)).nonce();
            h.assertTrue(!nonce[0].equals(SettingsRequestPayload.NONE),"Prepare returns a fresh challenge");
        });
        h.runAtTickTime(24,()->{
            admin.allowed=false;
            try(var probe=new CrashEffectsProbe()) {
                DroneSettingsAuthority.handle(admin,request(admin,4,DroneConfig.get(),nonce[0]));
                h.assertTrue(probe.explosions==0,"Revoked admin cannot confirm a previously authorized test");
            }
            admin.allowed=true;
        });
        h.runAtTickTime(36,()->{
            try(var probe=new CrashEffectsProbe()) {
                var response=DroneSettingsAuthority.handle(admin,request(admin,4,DroneConfig.get(),nonce[0]));
                h.assertTrue(response.status().contains("выполнен")&&probe.explosions==1,"Valid confirmation executes one real test explosion");
            }
        });
        h.runAtTickTime(48,()->{
            try(var probe=new CrashEffectsProbe()) {
                DroneSettingsAuthority.handle(admin,request(admin,4,DroneConfig.get(),nonce[0]));
                h.assertTrue(probe.explosions==0,"Consumed confirmation must never explode twice");
            }
            h.succeed();
        });
    }
}
