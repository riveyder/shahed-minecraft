"""Lightweight packaging audit; never starts Minecraft or loads mod classes."""
from pathlib import Path
from zipfile import ZipFile
import json,sys,hashlib
mc=sys.argv[1]
jar=Path(sys.argv[2]) if len(sys.argv)>2 else Path('ports/build')/mc/'libs'/('shahed-fpv-mayhem-1.0.1+mc'+mc+'.jar')
java={'1.20.1':17,'1.21.1':21,'26.2':25}[mc]
with ZipFile(jar) as z:
    names=set(z.namelist());meta=json.loads(z.read('fabric.mod.json'))
    assert meta['version']=='1.0.1+mc'+mc,meta['version']
    assert meta['depends']['minecraft']==mc,meta['depends']
    assert meta['depends']['java']=='>='+str(java)
    for n in names:
        if n.endswith('.json'):json.loads(z.read(n))
        if n.startswith('io/github/riveyder/shahed/') and n.endswith('.class'):
            assert int.from_bytes(z.read(n)[6:8],'big')==java+44,n
    sounds=json.loads(z.read('assets/shahed/sounds.json'))
    for event in sounds.values():
        for sound in event['sounds']:
            path=sound['name'] if isinstance(sound,dict) else sound
            if ':' in path:
                namespace,path=path.split(':',1)
                if namespace!='shahed':continue
            assert 'assets/shahed/sounds/'+path+'.ogg' in names,path
    recipes='recipes' if mc=='1.20.1' else 'recipe'
    for item in ['drone','training_drone','fpv_goggles','controller','quad','contact_quad','jet','arcade_payload']:
        assert 'assets/shahed/models/item/'+item+'.json' in names,item
        assert 'data/shahed/'+recipes+'/'+item+'.json' in names,item
    for model in ['drone','quad','contact_quad','jet','arcade_payload']:
        assert 'assets/shahed/textures/entity/'+model+'.png' in names,model
    for key in ['fireball','smoke','shockwave','debris','race_gate']:
        matches=[n for n in names if n.startswith('assets/shahed/particles/') and key in n]
        assert matches,key
        for n in matches:
            for texture in json.loads(z.read(n))['textures']:
                ns,path=texture.split(':',1)
                assert 'assets/'+ns+'/textures/particle/'+path+'.png' in names,texture
    for file,group in [('shahed.mixins.json','mixins'),('shahed.client.mixins.json','client')]:
        data=json.loads(z.read(file));assert data['compatibilityLevel']=='JAVA_'+str(java)
        for cls in data[group]:assert data['package'].replace('.','/')+'/'+cls+'.class' in names,cls
    print(mc,'metadata / JSON / classfile Java / models / recipes / audio / particles / mixins: OK')
    print(jar.resolve(),jar.stat().st_size,'bytes')
    print('SHA256',hashlib.sha256(jar.read_bytes()).hexdigest())
