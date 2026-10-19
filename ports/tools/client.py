import re
C='io.github.riveyder.shahed.compat.client.'
def source(s,name,mc):
    for a,b in {'net.minecraft.client.gui.GuiGraphicsExtractor':C+'GuiGraphicsExtractor','net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry':C+'HudElementRegistry','ParticleProviderRegistry':'ParticleFactoryRegistry','ModelLayerRegistry':'EntityModelLayerRegistry','net.minecraft.client.model.EntityModel':C+'LegacyModel','EntityModel<':'LegacyModel<','net.minecraft.client.renderer.state.level.QuadParticleRenderState':C+'QuadParticleRenderState','sprites.first()':'sprites.get(0,1)','net.minecraft.client.particle.SingleQuadParticle':C+'SingleQuadParticle'}.items():s=s.replace(a,b)
    if name=='RaceGateParticle.java':s=s.replace('import net.minecraft.client.particle.*;','import net.minecraft.client.particle.*;\nimport '+C+'SingleQuadParticle;')
    if name=='DroneRenderState.java':s=s.replace('import net.minecraft.client.renderer.entity.state.EntityRenderState;','').replace(' extends EntityRenderState','')
    if name=='FpvController.java':
        s=s.replace('import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;','').replace('import net.minecraft.client.DeltaTracker;','').replace('DeltaTracker delta','Float delta')
        a=s.index('        for (ResourceLocation element');b=s.index('        HudElementRegistry.addLast',a);s=s[:a]+s[b:]
    if name.endswith('Screen.java'):
        s=re.sub(r'void extractRenderState\(GuiGraphicsExtractor (\w+),([^)]*)\)\s*{',lambda m:'void render(net.minecraft.client.gui.GuiGraphics rawGraphics,'+m[2]+'){GuiGraphicsExtractor '+m[1]+'=new GuiGraphicsExtractor(rawGraphics);',s)
        s=re.sub(r'super.extractRenderState\(\w+,','super.render(rawGraphics,',s)
    if name=='FpvKeys.java':s=re.sub(r'KeyMapping.Category CATEGORY = .*?;', 'String CATEGORY = "key.category.shahed.fpv";',s)
    if mc=='1.20.1':s=s.replace('net.minecraft.client.gui.screens.options.controls.KeyBindsScreen','net.minecraft.client.gui.screens.controls.KeyBindsScreen')
    if name=='MinecraftControlMixin.java':s=s.replace('            client.gui.handleKeybinds();','')
    if name in ('RaceClient.java','ExplosionEffects.java'):s=s.replace(',random)->',')->').replace(',dz,r)->',',dz)->')
    if name=='ShahedClient.java' and mc=='1.20.1':s=s.replace('        FpvController.initialize();','        net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback.EVENT.register((stack,context,lines)->io.github.riveyder.shahed.item.EquipmentTooltips.append(stack,lines));\n        FpvController.initialize();')
    s=s.replace('getMainCamera().position()','getMainCamera().getPosition()').replace('KeyBindingHelper.registerKeyMapping','KeyBindingHelper.registerKeyBinding').replace('getResourceLocation()','getLocation()')
    if name=='ReplayController.java' and mc=='1.20.1':s=s.replace('level.addEntity(ghost)','level.putNonPlayerEntity(ghost.getId(),ghost)')
    if name=='ShahedClient.java':s=s.replace('net.minecraft.client.renderer.entity.EntityRenderers','net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry').replace('EntityRenderers.register','EntityRendererRegistry.register')
    return s
