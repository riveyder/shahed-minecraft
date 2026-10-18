"""Original CC0 arcade aircraft/impact synthesis, 48 kHz mono. No recorded samples.
Engine and wind are periodic functions across the full loop, with no end fades/gaps.
Run from any directory; only project resources and build/audio are written.
"""
import math,random,struct,subprocess,wave,json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'src/main/resources/assets/shahed/sounds';OUT.mkdir(parents=True,exist_ok=True)
TEMP=ROOT/'build/audio';TEMP.mkdir(parents=True,exist_ok=True)
RATE=48000
DURATIONS={'engine':4,'startup':1.15,'shutdown':1.25,'wind':4,'impact':.35,'blast':3.2,'blast_echo':2.4}
rng=random.Random(410)
# Integer frequency bins over a four-second period guarantee continuity without silence at the join.
wind_bands=[(rng.randrange(100,16000)/4,rng.random()*math.tau,1/math.sqrt(i+1)) for i in range(48)]
def periodic_noise(t):
    return sum(a*math.sin(math.tau*f*t+p) for f,p,a in wind_bands)/10

def engine(phase,t):
    exhaust=sum(math.sin(k*phase+.15*math.sin(math.tau*2*t))/k**.85 for k in range(1,19))
    body=.32*math.sin(phase/2)+.16*math.sin(phase*2)
    return math.tanh(exhaust*.85+body)*(.82+.10*math.sin(math.tau*4*t)+.06*math.sin(math.tau*11*t))

for name,duration in DURATIONS.items():
    rng=random.Random(410);values=[];phase=0;low=0;mid=0
    for i in range(round(RATE*duration)):
        t=i/RATE;p=t/duration;n=rng.uniform(-1,1)
        low+=.008*(n-low);mid+=.13*(n-mid)
        if name=='engine': value=engine(math.tau*72*t,t)+.055*periodic_noise(t)
        elif name=='wind': value=periodic_noise(t)*(.85+.15*math.sin(math.tau*t))
        elif name in ('startup','shutdown'):
            freq=22+65*p**.55 if name=='startup' else 20+65*(1-p)**.7
            phase+=math.tau*freq/RATE
            envelope=min(1,t/.035,(duration-t)/.18)
            value=(engine(phase,t)*.7+mid*.2)*envelope
            if name=='startup': value+=mid*.5*math.exp(-((t-.18)/.045)**2)
        elif name=='impact': value=(n*.35+mid*.4+math.sin(math.tau*120*t)*.3)*math.exp(-t*22)*min(1,t*1200)
        elif name=='blast':
            transient=n*.8*math.exp(-t*90)
            kick=math.sin(math.tau*(35*t+37*.15*(1-math.exp(-t/.15))))*.6*math.exp(-t*4)
            crack=mid*2.2*math.exp(-t*7)
            rumble=(low*7+.09*math.sin(math.tau*43*t))*(1-math.exp(-t*18))*math.exp(-t*1.7)
            value=(transient+kick+crack+rumble)*min(1,t*1600,(duration-t)*8)
        else:
            reflections=sum(math.exp(-((t-d)/.09)**2)*a for d,a in [(.08,.6),(.31,.4),(.59,.3),(.95,.2)])
            value=(mid*.35+low*5)*(reflections+.2*math.exp(-t*1.3))*min(1,t*80,(duration-t)*5)
        values.append(value)
    # Remove DC offset, retain headroom instead of clipping the final mix.
    mean=sum(values)/len(values);values=[v-mean for v in values];gain=.89/max(abs(v) for v in values)
    pcm=b''.join(struct.pack('<h',round(v*gain*32767)) for v in values)
    wav=TEMP/(name+'.wav')
    with wave.open(str(wav),'wb') as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(RATE);f.writeframes(pcm)
    subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(wav),'-c:a','libvorbis','-q:a','6',str(OUT/(name+'.ogg'))],check=True)
manifest={name:{'sounds':[{'name':'shahed:'+name,'attenuation_distance':128 if name.startswith('blast') else 64 if name=='engine' else 40}]} for name in DURATIONS}
(ROOT/'src/main/resources/assets/shahed/sounds.json').write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf8')
print('Generated 7 original 48 kHz mono Vorbis sounds; loops have no envelope fades')
