from pathlib import Path
import sys,json,shutil
from transforms import source,resources
import adapters
ROOT=Path(__file__).resolve().parents[2];mc=sys.argv[1]
assert mc in ('1.20.1','1.21.1')
out=ROOT/'ports/build'/mc/'generated'
# Only this generated target directory is replaced; authored sources are never mutated.
assert out.resolve().is_relative_to((ROOT/'ports/build').resolve()), 'Unsafe generated path'
if out.exists():shutil.rmtree(out)
for src,kind in [('src/main/java','main'),('src/client/java','client')]:
    for p in (ROOT/src).rglob('*.java'):
        dest=out/kind/p.relative_to(ROOT/src);dest.parent.mkdir(parents=True,exist_ok=True)
        dest.write_text(source(p.read_text(encoding='utf-8'),p.name,mc),encoding='utf-8')
for src in ('src/main/resources','src/client/resources'):
    for p in (ROOT/src).rglob('*'):
        if p.is_file():
            dest=out/'resources'/p.relative_to(ROOT/src);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
p=out/'resources/fabric.mod.json';d=json.loads(p.read_text(encoding='utf-8'));d['version']='1.0.1+mc'+mc;d['depends'].update({'minecraft':mc,'java':'>='+('17' if mc=='1.20.1' else '21'),'fabric-api':'>='+('0.92.12+1.20.1' if mc=='1.20.1' else '0.116.17+1.21.1')});p.write_text(json.dumps(d,ensure_ascii=False,indent=2),encoding='utf-8')
if mc=='1.20.1':
    (out/'main/io/github/riveyder/shahed/mixin/PlayerChunkSenderFpvMixin.java').unlink()
# Apply authored adapters last; common adapters are already written against old API names.
for layer in ('common',mc):
    for p in (ROOT/'ports/adapters'/layer).rglob('*'):
        if p.is_file():
            dest=out/p.relative_to(ROOT/'ports/adapters'/layer);dest.parent.mkdir(parents=True,exist_ok=True)
            if p.suffix=='.java':dest.write_text(adapters.source(p.read_text(encoding='utf-8'),p.name,mc),encoding='utf-8')
            else:shutil.copyfile(p,dest)
for p in (out/'resources').rglob('*.json'):
    obj=resources(p,json.loads(p.read_text(encoding='utf-8')),mc)
    if p.name=='shahed.mixins.json':
        obj['mixins']=[m for m in obj['mixins'] if mc!='1.20.1' or m!='PlayerChunkSenderFpvMixin']
        obj['mixins']+=['PilotConnectionMixin','PortExplosionMixin']
    if p.name=='shahed.client.mixins.json':obj['client']+=['PortHudMixin']
    if '/data/shahed/recipe/' in p.as_posix():
        obj['key']={k:({'tag':v[1:]} if v.startswith('#') else {'item':v}) for k,v in obj['key'].items()}
        if mc=='1.20.1':obj['result']['item']=obj['result'].pop('id')
    if mc=='1.20.1' and '/advancement/' in p.as_posix():
        for criterion in obj.get('criteria',{}).values():
            for predicate in criterion.get('conditions',{}).get('items',[]):
                if isinstance(predicate.get('items'),str):predicate['items']=[predicate['items']]
    p.write_text(json.dumps(obj,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
if mc=='1.20.1':
    data=out/'resources/data/shahed'
    for old,new in [('recipe','recipes'),('advancement','advancements')]:
        if (data/old).exists():(data/old).rename(data/new)
print('Prepared',mc)

