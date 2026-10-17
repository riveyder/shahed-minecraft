"""Original procedural arcade audio (CC0); Python stdlib + existing ffmpeg."""
import math
from pathlib import Path
import random
import struct
import subprocess
import wave

ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'src/main/resources/assets/shahed/sounds'
OUT.mkdir(parents=True,exist_ok=True)
TEMP=ROOT/'build/audio'; TEMP.mkdir(parents=True,exist_ok=True)
RATE=22050
for name,duration in [('engine',2),('startup',.8),('shutdown',.7),('wind',2),('impact',.25),('blast',1.4)]:
    rng=random.Random(42); values=[]; phase=0; noise=0
    for i in range(round(RATE*duration)):
        t=i/RATE; progress=t/duration
        noise=.8*noise+.2*rng.uniform(-1,1)
        frequency=75
        if name=='startup': frequency=35+75*progress
        if name=='shutdown': frequency=100-75*progress
        phase+=2*math.pi*frequency/RATE
        pulse=(math.sin(phase)+.35*math.sin(phase*2)+.15*math.sin(phase*4))*.3
        value=pulse if name in ('engine','startup','shutdown') else noise
        if name in ('impact','blast'): value=(noise+.3*math.sin(2*math.pi*45*t))*math.exp(-6*progress)
        if name in ('startup','shutdown'): value*=math.sin(math.pi*progress)**.5
        if name in ('impact','blast'): value*=min(1,t*500)
        if name=='wind': value*=.5
        # Loop ends cross zero; wind uses a short fade to avoid clicks.
        if name in ('engine','wind'): value*=min(1,t*50,(duration-t)*50)
        values.append(struct.pack('<h',round(max(-1,min(1,value))*24000)))
    wav=TEMP/(name+'.wav')
    with wave.open(str(wav),'wb') as f:
        f.setnchannels(1); f.setsampwidth(2); f.setframerate(RATE); f.writeframes(b''.join(values))
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','4',str(OUT/(name+'.ogg'))],check=True)
print('Generated 6 original mono Vorbis sounds')
