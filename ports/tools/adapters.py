def source(s,name,mc):
    if mc!='1.21.1':return s
    s=s.replace('new ResourceLocation(namespace,path)','ResourceLocation.fromNamespaceAndPath(namespace,path)')
    s=s.replace('io.github.riveyder.shahed.compat.ServerPlayNetworking','net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking')
    if name=='LegacyModel.java':s=s.replace('float r,float g,float b,float a','int color').replace('light,overlay,r,g,b,a','light,overlay,color')
    if name=='SingleQuadParticle.java':s=s.replace('v.vertex(p.x,p.y,p.z).uv(u,w).color(rCol,gCol,bCol,alpha).uv2(light).endVertex()','v.addVertex(p.x,p.y,p.z).setUv(u,w).setColor(rCol,gCol,bCol,alpha).setLight(light)')
    if name=='DroneRenderer.java':s=s.replace('((state.tint>>16)&255)/255F,((state.tint>>8)&255)/255F,(state.tint&255)/255F,1','state.tint')
    if name=='PayloadRenderer.java':s=s.replace('OverlayTexture.NO_OVERLAY,1,1,1,1','OverlayTexture.NO_OVERLAY,-1')
    if name=='DroneExplosions.java':s=s.replace('target.ignoreExplosion()','target.ignoreExplosion(explosion)')
    if name=='PortExplosion.java':s=s.replace('power,false,interaction);','power,false,interaction,net.minecraft.core.particles.ParticleTypes.EXPLOSION,net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE);')
    return s
