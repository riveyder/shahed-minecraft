"""Mechanical common API translations; implementations live under adapters. No gameplay stubs."""
import re
C='io.github.riveyder.shahed.compat.'
def source(s,name,mc):
    replacements={
        'net.minecraft.resources.Identifier':'net.minecraft.resources.ResourceLocation','Identifier':'ResourceLocation',
        'Math.clamp(':C+'Compat.clamp(','.identifier()':'.location()',
        'net.minecraft.world.level.storage.ValueInput':C+'ValueInput','net.minecraft.world.level.storage.ValueOutput':C+'ValueOutput',
        'net.minecraft.world.entity.InterpolationHandler':C+'InterpolationHandler',
        'net.minecraft.server.permissions.Permissions':C+'Permissions',
        'net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab':'net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup',
        'FabricCreativeModeTab':'FabricItemGroup',
        'net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper':'net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper',
        'KeyMappingHelper':'KeyBindingHelper',
        '.sendOverlayMessage(':'.displayClientMessage(',
        '.gui.screen()':'.screen','.gui.overlay()':'.getOverlay()', '.gui.setScreen(':'.setScreen(',
        '.gui.hud.isHidden()':'.options.hideGui','.gameRenderer.mainCamera()':'.gameRenderer.getMainCamera()',
        '.getIdentifier()':'.getLocation()', '.noCollision()':'.noCollission()',
    }
    for a,b in replacements.items():s=s.replace(a,b)
    s=re.sub(r'(\b\w+)\.permissions\(\)\.hasPermission\(Permissions.COMMANDS_GAMEMASTER\)',r'\1.hasPermission(2)',s)
    # Player permission helpers differ from command-source permissions.
    s=s.replace('player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)','player.hasPermissions(2)')
    s=re.sub(r'(\b(?:player|p|pilot))\.hasPermission\(2\)',r'\1.hasPermissions(2)',s)
    s=s.replace('ChunkPos.containing(', 'new ChunkPos(')
    s=s.replace('recording.frames().getLast()',C+'Compat.last(recording.frames())').replace('frames.getLast()',C+'Compat.last(frames)')
    s=s.replace('.getFirst()', '.get(0)')
    if name!='ReplayBuffer.java':s=s.replace('.removeFirst()', '.remove(0)')
    # Old client GUI signatures are adapted below; keep all widget logic shared.
    if mc=='1.20.1':
        s=s.replace('net.minecraft.network.RegistryFriendlyByteBuf','net.minecraft.network.FriendlyByteBuf').replace('RegistryFriendlyByteBuf','FriendlyByteBuf')
        s=s.replace('net.minecraft.network.codec.StreamCodec',C+'StreamCodec').replace('net.minecraft.network.protocol.common.custom.CustomPacketPayload',C+'CustomPacketPayload')
        s=s.replace('net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking',C+'ServerPlayNetworking')
        s=s.replace('net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry',C+'PayloadTypeRegistry')
        s=s.replace('net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking',C+'client.ClientPlayNetworking')
        if 'import net.fabricmc.fabric.api.networking.v1.*;' in s:s=s.replace('import net.fabricmc.fabric.api.networking.v1.*;','import net.fabricmc.fabric.api.networking.v1.*;\nimport '+C+'ServerPlayNetworking;')
        if 'import net.fabricmc.fabric.api.client.networking.v1.*;' in s:s=s.replace('import net.fabricmc.fabric.api.client.networking.v1.*;','import net.fabricmc.fabric.api.client.networking.v1.*;\nimport '+C+'client.ClientPlayNetworking;')
        s=s.replace('ResourceLocation.fromNamespaceAndPath(',C+'Compat.id(')
    if name in ('DroneEntity.java','PayloadEntity.java'):
        s=re.sub(r'protected void readAdditionalSaveData\(ValueInput (\w+)\)\s*\{',lambda m:'protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { ValueInput '+m[1]+'=new ValueInput(tag);',s)
        s=re.sub(r'protected void addAdditionalSaveData\(ValueOutput (\w+)\)\s*\{',lambda m:'protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) { ValueOutput '+m[1]+'=new ValueOutput(tag);',s)
        if mc=='1.20.1':s=re.sub(r'defineSynchedData\(SynchedEntityData.Builder (\w+)\)', 'defineSynchedData()',s).replace('builder.define(', 'entityData.define(')
        s=s.replace('    @Override\n    public InterpolationHandler', '    public InterpolationHandler').replace('@Override public InterpolationHandler','public InterpolationHandler')
        s=s.replace('    @Override\n    public boolean hurtServer','    public boolean hurtServer').replace('@Override public boolean hurtServer','public boolean hurtServer')
        s=s.replace('needsSync', 'hasImpulse').replace('isInvulnerableToBase(', 'isInvulnerableTo(')
        s=s.replace('Entity.collectAllColliders(',C+'Compat.colliders(')
        if name=='DroneEntity.java':s=s.replace('    public InterpolationHandler getInterpolation()', '    public float getYRot(float t){return net.minecraft.util.Mth.rotLerp(t,yRotO,getYRot());}\n    public float getXRot(float t){return net.minecraft.util.Mth.lerp(t,xRotO,getXRot());}\n    public InterpolationHandler getInterpolation()')
        idx=s.rfind('}')
        s=s[:idx]+'''    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){interpolation.set(x,y,z,yaw,pitch,steps);}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){return level() instanceof ServerLevel server&&hurtServer(server,source,amount);}
'''+s[idx:]
    if name=='PayloadEntity.java':s=s.replace('import net.minecraft.world.entity.*;','import net.minecraft.world.entity.*;\nimport '+C+'InterpolationHandler;\nimport '+C+'ValueInput;\nimport '+C+'ValueOutput;')
    if name in ('DroneCommands.java','RaceAircraft.java','DroneSessions.java','DroneSettingsAuthority.java','DroneReplays.java','DroneRacing.java','ControllerItem.java'):
        for var in ('player','serverPlayer','pilot','owner','p','e.player','session.player'):s=s.replace(var+'.level()',var+'.serverLevel()')
    # Component overlay call now has the explicit action-bar boolean.
    start=0
    while True:
        start=s.find('.displayClientMessage(',start)
        if start<0:break
        end=start+len('.displayClientMessage(');depth=1
        while depth:
            if s[end]=='(':depth+=1
            elif s[end]==')':depth-=1
            end+=1
        s=s[:end-1]+',true'+s[end-1:];start=end+5
    s=re.sub(r'([\w.]+)\.connection\.resetFlyingTicks\(\)',r'(('+C+r'PilotConnection)\1.connection).shahed$resetFlyingTicks()',s)
    if name=='ModEntities.java':
        s=re.sub(r'\.eyeHeight\([^)]*\)','',s)
        s=re.sub(r'\.build\((DRONE_KEY|PAYLOAD_KEY|key)\)',r'.build(\1.location().toString())',s)
    if name=='CrashFireBlock.java':
        s=s.replace('.setId(ResourceKey.create(Registries.BLOCK,ShahedMod.id("crash_fire")))','')
        if mc=='1.20.1':
            s=re.sub(r'    public static final MapCodec<CrashFireBlock> CODEC=.*?;\n','',s)
            s=re.sub(r'    @Override protected MapCodec<CrashFireBlock> codec\(\) \{return CODEC;}\n','',s)
            s=s.replace('protected void onPlace','public void onPlace').replace('protected void tick','public void tick')
    if name in ('DroneSessions.java','DroneReplays.java'):
        s=re.sub(r'\(\(ChunkMapFpvAccess\)chunks?\.chunkMap\)\.shahed\$updateChunkTracking\(p(?:layer)?\);','',s)
        s=re.sub(r'\(\(ChunkMapFpvAccess\) chunks\.chunkMap\)\.shahed\$updateChunkTracking\(player\);','',s)
    if name=='DroneSessions.java':
        s=s.replace('UseItemCallback.EVENT.register((player, level, hand) -> controllingResult(', 'UseItemCallback.EVENT.register((player, level, hand) -> new net.minecraft.world.InteractionResultHolder<>(controllingResult(')
        s=s.replace('&& !player.getItemInHand(hand).is(ModItems.CONTROLLER)));','&& !player.getItemInHand(hand).is(ModItems.CONTROLLER)),player.getItemInHand(hand)));')
    if name=='ServerPlayerControlMixin.java':s=s.replace('"handleAttack", ','')
    import client
    return client.source(s,name,mc)

def resources(path,obj,mc):
    if path.name.endswith('mixins.json'):obj['compatibilityLevel']='JAVA_17' if mc=='1.20.1' else 'JAVA_21'
    if mc=='1.20.1' and path.parts[-3:-1]==('recipe','equipment'):pass
    return obj
