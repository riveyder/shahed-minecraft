"""Deterministic source-derived Blockbench + Java exporter. Requires Pillow.

The user's original is never overwritten. Dimensions are scaled before adding
surface details. Java boxes use the same unwrapped atlas as the editable model.
"""
import base64
import copy
import io
import json
import math
from pathlib import Path
import uuid
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / 'assets-work/Shahed_Sheep.bbmodel'
OUT = ROOT / 'assets-work/Shahed_Sheep_Overhaul.bbmodel'
JAVA = ROOT / 'src/client/java/io/github/riveyder/shahed/client/model/DroneModel.java'
SCALE = 2.5


def generate():
    model = json.loads(SOURCE.read_text(encoding='utf-8'))
    original = copy.deepcopy(model['elements'])
    source_texture = Image.open(io.BytesIO(base64.b64decode(model['textures'][0]['source'].split(',')[1]))).convert('RGB')
    # Remove empty space beneath the original model, retaining all relative geometry.
    ground = min(e['from'][1] for e in original)
    for e in model['elements']:
        assert e.get('rotation', [0, 0, 0]) == [0, 0, 0], 'Rotated source needs explicit export support'
        for key in ('from', 'to', 'origin'):
            e[key] = [round((v - (ground if axis == 1 else 0)) * SCALE, 5) for axis, v in enumerate(e[key])]
    def detail(name, low, high, color):
        model['elements'].append(dict(name=name, type='cube', uuid=str(uuid.uuid5(uuid.NAMESPACE_URL, 'shahed:'+name)),
                                      **{'from': low, 'to': high}, origin=[0, 0, 0], rotation=[0, 0, 0], material=color))
    detail('FPV camera bezel', [-1.8, 5.8, -33.4], [1.8, 9.2, -32.45], '#26343a')
    detail('FPV optical glass', [-1.1, 6.5, -33.7], [1.1, 8.5, -33.4], '#4cb8c3')
    detail('Service hatch', [-3.5, 13.5, -5], [3.5, 13.9, 2], '#737b70')
    detail('Antenna base', [-1, 13.6, 3], [1, 14.2, 5], '#343b38')
    detail('Antenna mast', [-.25, 14.2, 3.7], [.25, 18.5, 4.2], '#252c2d')
    for side in (-1, 1):
        lo, hi = sorted((side*8, side*28))
        detail(f'Elevon {side}', [lo, 6.4, 19.7], [hi, 8.3, 20.2], '#777e71')
        lo, hi = sorted((side*33.1, side*35))
        detail(f'Navigation light {side}', [lo, 9, 12], [hi, 10.1, 14], '#b64935' if side < 0 else '#49a58d')
        lo, hi = sorted((side*3.8, side*4.8))
        detail(f'Landing runner {side}', [lo, -.4, -12], [hi, 1.5, 16], '#424747')
        for n in range(5):
            detail(f'Cooling fin {side} {n}', [min(side*5.3,side*6), 7, 21+n*1.3],
                   [max(side*5.3,side*6), 10.8, 21.6+n*1.3], '#353b37')
    # Raise by .4 px so the additional landing runners sit on the entity base.
    for e in model['elements']:
        for key in ('from','to','origin'):
            e[key][1] = round(e[key][1] + .4, 5)
    atlas = Image.new('RGB', (512,512), '#333a37')
    draw = ImageDraw.Draw(atlas)
    calls = []
    u = v = row_height = 0
    for index, e in enumerate(model['elements']):
        x,y,z = e['from']
        dx,dy,dz = [round(b-a,5) for a,b in zip(e['from'],e['to'])]
        w,h = math.ceil(2*(dx+dz))+4, math.ceil(dy+dz)+4
        if u+w > 512: u=0; v+=row_height; row_height=0
        assert v+h <= 512
        a,b = u+2,v+2
        uv = {'west':[a,b+dz,a+dz,b+dz+dy], 'north':[a+dz,b+dz,a+dz+dx,b+dz+dy],
              'east':[a+dz+dx,b+dz,a+2*dz+dx,b+dz+dy],
              'south':[a+2*dz+dx,b+dz,a+2*dz+2*dx,b+dz+dy],
              'down':[a+dz,b,a+dz+dx,b+dz], 'up':[a+dz+dx,b,a+dz+2*dx,b+dz]}
        for face, coords in uv.items():
            rect = tuple(round(c) for c in coords)
            fw,fh = rect[2]-rect[0],rect[3]-rect[1]
            if fw<1 or fh<1: continue
            if index < len(original):
                crop = original[index]['faces'][face]['uv']
                tile = source_texture.crop(tuple(crop)).resize((fw,fh), Image.Resampling.NEAREST)
            else: tile = Image.new('RGB',(fw,fh),e['material'])
            td=ImageDraw.Draw(tile)
            if fw>=5 and fh>=5:
                td.rectangle((0,0,fw-1,fh-1),outline='#62685b')
                for px,py in ((1,1),(fw-2,1),(1,fh-2),(fw-2,fh-2)):
                    td.point((px,py),fill='#aeb2a0')
            atlas.paste(tile,rect[:2])
        e['faces']={face:{'uv':coords,'texture':0} for face,coords in uv.items()}
        e['box_uv']=False
        e.pop('material',None)
        is_propeller=e['name'].startswith('Propeller ')
        parent='propeller' if is_propeller else 'root'
        floats=', '.join(f'{n:.5f}F' for n in (x,24-e['to'][1]-(16.1 if is_propeller else 0),z-(33.125 if is_propeller else 0),dx,dy,dz))
        calls.append(f'        box({parent}, "part_{index}", {a}, {b}, {floats}); // {e["name"]}')
        u+=w; row_height=max(row_height,h)
    png=io.BytesIO(); atlas.save(png,format='PNG')
    (ROOT/'src/main/resources/assets/shahed/textures/entity/drone.png').write_bytes(png.getvalue())
    model['name']='Shahed Sheep — КРИВЫЕ РУКИ AEROSPACE — 2.5x overhaul'
    model['resolution']={'width':512,'height':512}
    propeller_ids=[e['uuid'] for e in model['elements'] if e['name'].startswith('Propeller ')]
    model['outliner']=[e['uuid'] for e in model['elements'] if e['uuid'] not in propeller_ids]
    model['outliner'].append({'name':'propeller','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'shahed:propeller')),
                              'origin':[0,7.9,33.125],'rotation':[0,0,0],'children':propeller_ids})
    model['textures']=[{'name':'drone.png','id':'0','uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,'shahed:atlas')),
                       'source':'data:image/png;base64,'+base64.b64encode(png.getvalue()).decode(),
                       'internal':True,'saved':False,'render_mode':'default'}]
    OUT.write_text(json.dumps(model,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    text=JAVA.read_text(encoding='utf-8')
    text=text[:text.index('/**')]+'''/** Generated by docs/model/overhaul.py from the editable Sheep model. */
public final class DroneModel extends EntityModel<DroneRenderState> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ShahedMod.id("drone"), "main");
    private final ModelPart propeller;
    public DroneModel(ModelPart root) { super(root);propeller=root.getChild("propeller"); }
    @Override public void setupAnim(DroneRenderState state) {
        super.setupAnim(state);
        propeller.zRot=state.propellerAngle;
    }
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition propeller = root.addOrReplaceChild("propeller", CubeListBuilder.create(), PartPose.offset(0,16.1F,33.125F));
'''+ '\n'.join(calls)+'''
        return LayerDefinition.create(mesh, 512, 512);
    }
    private static void box(PartDefinition root, String name, int u, int v,
                            float x, float y, float z, float dx, float dy, float dz) {
        root.addOrReplaceChild(name, CubeListBuilder.create().texOffs(u, v).addBox(x,y,z,dx,dy,dz), PartPose.ZERO);
    }
}
'''
    JAVA.write_text(text,encoding='utf-8')
    print(f'Exported {len(model["elements"])} cubes, atlas 512x512; source unchanged')

if __name__=='__main__': generate()
