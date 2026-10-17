"""Offline geometry/atlas preview; does not claim Minecraft rendering verification."""
import base64
import io
import json
import os
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
os.environ['MPLCONFIGDIR']=str(ROOT/'build/matplotlib')
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d.art3d import Poly3DCollection
import numpy as np
from PIL import Image

model=json.loads((ROOT/'assets-work/Shahed_Sheep_Overhaul.bbmodel').read_text(encoding='utf-8'))
atlas=Image.open(io.BytesIO(base64.b64decode(model['textures'][0]['source'].split(',')[1]))).convert('RGB')
polygons=[];colors=[]
def world(v): return np.array([v[0],-v[2],v[1]])/16
for e in model['elements']:
    x,y,z=e['from'];X,Y,Z=e['to']
    faces={'up':([x,Y,z],[X,Y,z],[x,Y,Z]),'down':([x,y,Z],[X,y,Z],[x,y,z]),
           'north':([x,Y,z],[X,Y,z],[x,y,z]),'south':([X,Y,Z],[x,Y,Z],[X,y,Z]),
           'west':([x,Y,Z],[x,Y,z],[x,y,Z]),'east':([X,Y,z],[X,Y,Z],[X,y,z])}
    for face,(a,b,c) in faces.items():
        uv=e['faces'][face]['uv'];w=max(1,min(24,round(uv[2]-uv[0])));h=max(1,min(24,round(uv[3]-uv[1])))
        texture=np.asarray(atlas.crop(tuple(uv)).resize((w,h)))/255
        origin=world(a);du=(world(b)-origin)/w;dv=(world(c)-origin)/h
        shade={'up':1,'down':.5,'north':.75,'south':.8,'west':.7,'east':.9}[face]
        for row in range(h):
            for col in range(w):
                p=origin+du*col+dv*row
                polygons.append([p,p+du,p+du+dv,p+dv]);colors.append(texture[row,col]*shade)
fig=plt.figure(figsize=(13,7),facecolor='#e7ece9')
ax=fig.add_subplot(111,projection='3d')
ax.add_collection3d(Poly3DCollection(polygons,facecolors=colors,edgecolors='none'))
ax.set(xlim=(-2.5,2.5),ylim=(-2.5,2.5),zlim=(0,1.8),xlabel='X (blocks)',ylabel='Nose +Z (blocks)',zlabel='Height')
ax.set_box_aspect((5,5,1.8));ax.view_init(elev=27,azim=46)
fig.suptitle('SHAHED.exe | Sheep overhaul | 2.5x | 63 editable cubes',fontsize=16)
fig.text(.5,.03,'Offline atlas/geometry preview. In-game lighting, camera and UV orientation still require a client check.',ha='center',fontsize=10)
fig.savefig(ROOT/'docs/model/sheep-overhaul-preview.png',dpi=140)
print('Rendered docs/model/sheep-overhaul-preview.png')
