"""Original fleet geometry, editable Blockbench sources, pixel atlases and matching Java export.
No third-party images/models. Run only writes workspace assets and generated source.
"""
from pathlib import Path
from PIL import Image,ImageDraw
import json,uuid,math,io,base64
ROOT=Path(__file__).resolve().parents[2]
AS=ROOT/'src/main/resources/assets/shahed'
JAVA=ROOT/'src/client/java/io/github/riveyder/shahed/client/model/FleetModel.java'
DARK='#303b46';METAL='#9ea9b3';BLACK='#121c24';GLASS='#2fe7e6';ORANGE='#fda83d'

def build(kind):
    cubes=[];groups={}
    def box(name,x,y,z,dx,dy,dz,color=METAL,group='root'):
        cubes.append(dict(name=name,x=x,y=y,z=z,dx=dx,dy=dy,dz=dz,color=color,group=group))
    if kind!='jet':
        accent='#f5744c' if kind=='contact_quad' else '#4cd4c8'
        box('carbon deck',-3,18,-5,6,1,10,DARK)
        box('electronics',-2.5,16,-3,5,2,6,BLACK)
        box('battery',-2,14,-1,4,2,5,accent)
        box('battery strap',-2.25,13.75,1,4.5,.25,1,BLACK)
        box('camera mount',-1.5,16,-6.5,3,3,2,DARK)
        box('FPV lens',-1,16.5,-7,2,2,.5,GLASS)
        box('antenna stalk',-.25,10,4,.5,6,.5,BLACK)
        box('antenna head',-1.5,9,3,3,1,2,accent)
        for x in (-7.5,7.5):
            for z in (-7.5,7.5):
                side='L' if x<0 else 'R';end='F' if z<0 else 'B';g='rotor_'+side+end
                # Stepped diagonal carbon arms, matching Minecraft's blocky style.
                for t in range(3):
                    px=x*(.35+.23*t);pz=z*(.35+.23*t)
                    box('arm '+g+str(t),px-1,18,pz-1,2,1.2,2,DARK)
                box('motor base '+g,x-1.4,16,z-1.4,2.8,2,2.8,accent)
                box('motor shaft '+g,x-.7,14,z-.7,1.4,2,1.4,METAL)
                groups[g]=(x,14,z)
                box('blade A '+g,-5,-.2,-.55,10,.4,1.1,BLACK,g)
                box('blade B '+g,-.55,-.2,-5,1.1,.4,10,accent,g)
                box('hub '+g,-.7,-.6,-.7,1.4,.6,1.4,METAL,g)
        for x in (-3,3):
            for z in (-3.5,3.5):box('landing leg',x-.4,19,z-.4,.8,4,.8,DARK)
            box('skid',x-.5,23,-5,1,1,10,BLACK)
        if kind=='quad':
            groups['payload']=(0,0,0)
            box('payload socket',-1,19,-1,2,1,2,METAL)
            box('arcade payload casing',-2,20,-2.5,4,2.5,5,ORANGE,'payload')
            box('payload band',-2.1,20.8,-2.6,4.2,.7,5.2,BLACK,'payload')
            box('payload indicator',-.6,20.5,-2.8,1.2,1,.2,GLASS,'payload')
        else:
            box('integrated contact core',-2.5,19,-4,5,3,7,accent)
            box('contact bumper',-3,19,-6,6,2,2,ORANGE)
            box('single use stripe',-2.6,20,-4.1,5.2,.7,7.2,BLACK)
    else:
        # Tapered fuselage, swept layered wings, twin intakes and a raised tail fin.
        for i,(x,y,z,dx,dy,dz) in enumerate([(-1,17,-21,2,2,4),(-2,16,-17,4,4,5),(-3,15,-12,6,6,8),(-3.5,15,-4,7,6,12),(-2.5,16,8,5,5,7)]):
            box('tapered fuselage '+str(i),x,y,z,dx,dy,dz,'#c5d1d9' if i%2==0 else '#788b9a')
        box('sensor canopy',-1.8,13.5,-11,3.6,1.5,7,GLASS)
        for sign in (-1,1):
            for i in range(5):
                extent=5+i*2.2
                box('swept wing',-extent if sign<0 else 3,18,-6+i*2.3,extent-3,1.2,3.2,METAL)
            box('wing accent',-13.8 if sign<0 else 11.8,17.9,4,2,1.4,3,ORANGE)
            box('intake housing',-5 if sign<0 else 3,16,-3,2,3,8,DARK)
            box('intake mouth',-4.8 if sign<0 else 3.2,16.4,-3.2,1.6,2.2,.3,BLACK)
            box('rear stabilizer',-7 if sign<0 else 2,17,10,5,1,4,METAL)
            box('landing skid',-2 if sign<0 else 1,21,-8,1,3,18,BLACK)
        box('tail fin root',-.6,9,6,1.2,7,8,DARK)
        box('tail fin tip',-.6,7,9,1.2,2,5,ORANGE)
        box('exhaust collar',-2.8,15.8,14,5.6,5.6,2,DARK)
        box('exhaust nozzle',-1.9,16.7,16,3.8,3.8,.5,BLACK)
        box('exhaust glow',-1.25,17.35,16.55,2.5,2.5,.1,ORANGE)
    return cubes,groups

def generate(kind):
    cubes,groups=build(kind);atlas=Image.new('RGB',(512,512),'#11171c');draw=ImageDraw.Draw(atlas)
    elements=[];children={g:[] for g in groups};rootids=[];calls=[];u=2;v=2;row=0
    uid=lambda s:str(uuid.uuid5(uuid.NAMESPACE_URL,'shahed:fleet:'+kind+':'+s))
    for g,(x,y,z) in groups.items():calls.append(f'        PartDefinition {g}=root.addOrReplaceChild("{g}",CubeListBuilder.create(),PartPose.offset({x}F,{y}F,{z}F));')
    for index,c in enumerate(cubes):
        x,y,z,dx,dy,dz=[c[k] for k in ('x','y','z','dx','dy','dz')]
        w=math.ceil(2*(dx+dz))+2;h=math.ceil(dz+dy)+2
        if u+w>510:u=2;v+=row;row=0
        assert v+h<512
        a,b=u,v
        uv={'west':[a,b+dz,a+dz,b+dz+dy],'north':[a+dz,b+dz,a+dz+dx,b+dz+dy],
            'east':[a+dz+dx,b+dz,a+2*dz+dx,b+dz+dy],'south':[a+2*dz+dx,b+dz,a+2*dz+2*dx,b+dz+dy],
            'up':[a+dz,b,a+dz+dx,b+dz],'down':[a+dz+dx,b,a+dz+2*dx,b+dz]}
        draw.rectangle((u,v,u+w-2,v+h-2),fill=c['color'])
        for rect in uv.values():
            x1,y1,x2,y2=map(round,rect)
            if x2-x1>4 and y2-y1>3:
                draw.rectangle((x1,y1,x2-1,y2-1),outline=DARK)
                draw.point((x1+1,y1+1),fill='#d6e1e4');draw.point((x2-2,y2-2),fill='#d6e1e4')
        group=c['group'];ox,oy,oz=groups.get(group,(0,0,0));eid=uid(str(index))
        elements.append({'name':c['name'],'uuid':eid,'type':'cube','from':[x+ox,24-y-oy-dy,z+oz],
                         'to':[x+ox+dx,24-y-oy,z+oz+dz],'box_uv':False,'faces':{f:{'uv':q,'texture':0} for f,q in uv.items()}})
        (rootids if group=='root' else children[group]).append(eid)
        nums=','.join(f'{n}F' for n in (x,y,z,dx,dy,dz))
        calls.append(f'        box({group},"part_{index}",{u},{v},{nums}); // {c["name"]}')
        u+=w;row=max(row,h)
    out=io.BytesIO();atlas.save(out,format='PNG');png=out.getvalue();(AS/'textures/entity'/f'{kind}.png').write_bytes(png)
    outliner=rootids+[{'name':g,'uuid':uid(g),'origin':[x,24-y,z],'rotation':[0,0,0],'children':children[g]} for g,(x,y,z) in groups.items()]
    model={'meta':{'format_version':'4.10','model_format':'free','box_uv':False},'name':'KRIVYE RUKI AEROSPACE / '+kind,
           'resolution':{'width':512,'height':512},'elements':elements,'outliner':outliner,
           'textures':[{'name':kind+'.png','id':'0','uuid':uid('texture'),'source':'data:image/png;base64,'+base64.b64encode(png).decode(),'internal':True,'saved':False}]}
    (ROOT/'assets-work'/f'{kind}.bbmodel').write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    # Original pixel inventory silhouette; do not alter existing item artwork.
    icon=Image.new('RGBA',(32,32),(0,0,0,0));d=ImageDraw.Draw(icon)
    if kind=='jet':
        d.polygon([(16,1),(19,11),(29,23),(18,21),(20,29),(12,29),(14,21),(3,23),(13,11)],fill='#b8c9d4',outline='#26323c')
        d.rectangle((15,8,17,15),fill=GLASS);d.rectangle((14,26,18,28),fill=ORANGE)
    else:
        ac='#f5744c' if kind=='contact_quad' else '#4cd4c8'
        d.line((7,7,25,25),fill=DARK,width=3);d.line((7,25,25,7),fill=DARK,width=3)
        for cx,cy in ((7,7),(25,7),(7,25),(25,25)):
            d.ellipse((cx-5,cy-5,cx+5,cy+5),outline=ac,width=2);d.line((cx-4,cy,cx+4,cy),fill=METAL,width=2)
        d.rectangle((12,10,20,23),fill=ac,outline=BLACK);d.rectangle((14,8,18,10),fill=GLASS)
    icon.save(AS/'textures/item'/f'{kind}.png')
    return '\n'.join(calls),len(elements)

head='''package io.github.riveyder.shahed.client.model;
import io.github.riveyder.shahed.ShahedMod;
import io.github.riveyder.shahed.client.renderer.DroneRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
/** Original geometry exported with docs/model/generate_fleet.py; editable sources in assets-work. */
public final class FleetModel extends EntityModel<DroneRenderState> {
    public static final ModelLayerLocation QUAD=new ModelLayerLocation(ShahedMod.id("quad"),"main");
    public static final ModelLayerLocation CONTACT=new ModelLayerLocation(ShahedMod.id("contact_quad"),"main");
    public static final ModelLayerLocation JET=new ModelLayerLocation(ShahedMod.id("jet"),"main");
    private final ModelPart[] rotors;
    private final ModelPart payload;
    public FleetModel(ModelPart root,boolean jet,boolean reusable){super(root);
        rotors=jet?new ModelPart[0]:new ModelPart[]{root.getChild("rotor_LF"),root.getChild("rotor_RF"),root.getChild("rotor_LB"),root.getChild("rotor_RB")};
        payload=reusable?root.getChild("payload"):null;
    }
    @Override public void setupAnim(DroneRenderState state){super.setupAnim(state);
        for(int i=0;i<rotors.length;i++)rotors[i].yRot=state.propellerAngle*((i==0||i==3)?1:-1);
        if(payload!=null)payload.visible=state.loaded;
    }
'''
for kind,method in [('quad','quadLayer'),('contact_quad','contactLayer'),('jet','jetLayer')]:
    calls,n=generate(kind)
    head+=f'    public static LayerDefinition {method}() {{\n        MeshDefinition mesh=new MeshDefinition();PartDefinition root=mesh.getRoot();\n'+calls+'\n        return LayerDefinition.create(mesh,512,512);\n    }\n'
    print(kind,n,'cuboids; Blockbench + texture + Java generated')
head+='''    private static void box(PartDefinition parent,String name,int u,int v,float x,float y,float z,float dx,float dy,float dz){
        parent.addOrReplaceChild(name,CubeListBuilder.create().texOffs(u,v).addBox(x,y,z,dx,dy,dz),PartPose.ZERO);
    }
}
'''
JAVA.write_text(head,encoding='utf-8')
# Dedicated payload inventory artwork.
icon=Image.new('RGBA',(32,32),(0,0,0,0));d=ImageDraw.Draw(icon)
d.rounded_rectangle((7,5,25,27),radius=3,fill=ORANGE,outline=BLACK,width=2);d.rectangle((8,13,24,17),fill=BLACK);d.rectangle((12,2,20,6),fill=METAL);d.rectangle((13,20,19,23),fill=GLASS)
icon.save(AS/'textures/item/arcade_payload.png')

im=Image.new('RGB',(32,32),'#fda83d');d=ImageDraw.Draw(im)
d.rectangle((0,12,31,23),fill='#202b34');d.rectangle((24,0,31,6),fill='#a3b4bf')
im.save(AS/'textures/entity/arcade_payload.png')
