from pathlib import Path
import json,math,random
from PIL import Image
root=Path('src/main/resources/assets/shahed');out=root/'textures/particle';out.mkdir(parents=True,exist_ok=True)
# Original procedural cloud sprites, no borrowed images. All frames use one continuous field.
rng=random.Random(410);lobes=[(rng.uniform(-.55,.55),rng.uniform(-.55,.55),rng.uniform(.25,.5)) for _ in range(14)]
for frame in range(4):
    im=Image.new('RGBA',(64,64));fire=Image.new('RGBA',(64,64))
    for y in range(64):
        for x in range(64):
            u=(x-31.5)/31.5;v=(y-31.5)/31.5
            density=max(math.exp(-((u-cx-.03*math.sin(frame+cy))**2+(v-cy)**2)/(r*r)*2) for cx,cy,r in lobes)
            edge=max(0,1-(u*u+v*v))**.7
            grain=.85+.1*math.sin(u*28+frame*.8)*math.cos(v*23-frame*.5)+.05*math.sin(u*59+v*47)
            shade=int(185+70*density*grain)
            im.putpixel((x,y),(shade,shade,shade,int(min(1,density*edge*1.8)*225)))
            fire_density=max(density,math.exp(-(u*u+v*v)*5))*edge*grain
            heat=max(0,min(1,fire_density*1.5-frame*.10))
            fire.putpixel((x,y),(255,int(45+210*heat**1.4),int(5+210*heat**4),int(min(1,fire_density*1.8)*240)))
    im.save(out/f'cloud_{frame}.png')
    fire.save(out/f'fireball_{frame}.png')
im=Image.new('RGBA',(128,128))
for y in range(128):
    for x in range(128):
        u=(x-63.5)/63.5;v=(y-63.5)/63.5;r=math.hypot(u,v)
        alpha=math.exp(-((r-.82)/.035)**2)*210+math.exp(-((r-.79)/.09)**2)*35
        im.putpixel((x,y),(255,244,216,int(min(255,alpha))))
im.save(out/'shockwave.png')
im=Image.new('RGBA',(16,16))
for y in range(16):
    for x in range(16):
        c=rng.randrange(140,240);im.putpixel((x,y),(c,c,c,255 if 2<x<13 and 2<y<13 else 0))
im.save(out/'debris.png')
(root/'particles').mkdir(exist_ok=True)
for name,frames in [('fireball',[f'fireball_{i}' for i in range(4)]),('cinematic_smoke',[f'cloud_{i}' for i in range(4)]),('shockwave',['shockwave']),('debris',['debris'])]:
    (root/'particles'/f'{name}.json').write_text(json.dumps({'textures':['shahed:'+f for f in frames]},indent=2)+'\n',encoding='utf8')
