"""Lightweight source/resource checks only; does not launch Java or Minecraft."""
from pathlib import Path
import json,re,base64,io,subprocess
from PIL import Image
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources';AS=RES/'assets/shahed'
for p in RES.rglob('*.json'):json.loads(p.read_text(encoding='utf-8'))
langs={k:json.loads((AS/f'lang/{k}.json').read_text(encoding='utf-8')) for k in ('en_us','ru_ru')}
for key,value in langs['en_us'].items():
    if key.startswith(('fleet.','help.shahed.','race.')) or any(n in key for n in ('quad','jet','payload')):
        assert key in langs['ru_ru'],key
        assert re.findall(r'%(?:\d+\$)?[sd]',value)==re.findall(r'%(?:\d+\$)?[sd]',langs['ru_ru'][key]),key
for name in ('quad','contact_quad','jet','arcade_payload'):
    assert f'item.shahed.{name}' in langs['en_us'] and f'item.shahed.{name}.hint' in langs['en_us']
    recipe=json.loads((RES/f'data/shahed/recipe/{name}.json').read_text());assert recipe['result']=={'id':'shahed:'+name,'count':1}
    assert {c for row in recipe['pattern'] for c in row if c!=' '}==set(recipe['key'])
    assert all(v.startswith(('minecraft:','#minecraft:')) for v in recipe['key'].values())
    advancement=json.loads((RES/f'data/shahed/advancement/recipes/equipment/{name}.json').read_text())
    assert advancement['rewards']['recipes']==['shahed:'+name]
    definition=json.loads((AS/f'items/{name}.json').read_text());assert definition['model']['model']=='shahed:item/'+name
    for texture in json.loads((AS/f'models/item/{name}.json').read_text())['textures'].values():
        assert (AS/'textures'/f'{texture.split(":")[1]}.png').is_file()
    Image.open(AS/f'textures/entity/{name}.png').verify()
for kind in ('quad','contact_quad','jet'):
    model=json.loads((ROOT/f'assets-work/{kind}.bbmodel').read_text(encoding='utf-8'));assert len(model['elements'])>25
    assert base64.b64decode(model['textures'][0]['source'].split(',')[1])==(AS/f'textures/entity/{kind}.png').read_bytes()
    groups={g['name']:g for g in model['outliner'] if isinstance(g,dict)}
    if kind!='jet':assert len([g for g in groups if g.startswith('rotor_')])==4
    assert ('payload' in groups)==(kind=='quad')
    ids={e['uuid'] for e in model['elements']};refs=[]
    for row in model['outliner']:refs.extend(row['children'] if isinstance(row,dict) else [row])
    assert len(refs)==len(ids) and set(refs)==ids
    for element in model['elements']:
        for face in element['faces'].values():assert all(0<=v<=512 for v in face['uv'])
manifest=json.loads((AS/'sounds.json').read_text());sounds=[]
for event,entry in manifest.items():
    for sound in entry['sounds']:
        path=AS/'sounds'/f'{sound["name"].split(":")[1]}.ogg';assert path.is_file();sounds.append(path)
assert len(sounds)==17
for path in sounds:
    if path.stem.startswith(('quad_','contact_quad_','jet_')):
        data=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_entries','stream=codec_name,channels,sample_rate','-of','json',str(path)]))['streams'][0]
        assert data['codec_name']=='vorbis' and data['channels']==1 and data['sample_rate']=='48000'
for p in (ROOT/'src/main/java').rglob('*.java'):assert 'import net.minecraft.client.' not in p.read_text(encoding='utf-8'),p
print('PASS: 4 recipes/advancements/icons, 3 editable models, 17 audio references (9 new mono Vorbis), RU/EN and common/client imports')
