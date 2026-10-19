"""Original CC0 electric/jet arcade audio. Only new fleet assets are written; SHAHED stays intact."""
from pathlib import Path
import numpy as np
import json,subprocess,wave
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'src/main/resources/assets/shahed/sounds';TMP=ROOT/'build/audio-fleet';TMP.mkdir(parents=True,exist_ok=True)
RATE=48000
manifest_path=OUT.parent/'sounds.json';manifest=json.loads(manifest_path.read_text())
for kind,base in [('quad',280),('contact_quad',360),('jet',180)]:
    for stage,duration in [('engine',4),('startup',1.2),('shutdown',1.4)]:
        t=np.arange(round(RATE*duration))/RATE;p=t/duration
        if stage=='engine':phase=2*np.pi*base*t;env=np.ones_like(t)
        else:
            f=base*(.2+.8*p) if stage=='startup' else base*(1-.85*p)
            phase=2*np.pi*np.cumsum(f)/RATE
            env=np.minimum(1,np.minimum(t/.05,(duration-t)/.2))
        if kind=='jet':
            signal=.32*np.sin(phase)+.19*np.sin(phase*2)+.1*np.sin(phase*4)
            for k in range(1,25):signal+=.12/np.sqrt(k)*np.sin(2*np.pi*(40+37*k)*t+k*.71)
            signal*=.88+.08*np.sin(2*np.pi*7*t)
        else:
            signal=.4*np.sin(phase)+.22*np.sin(phase*2)+.12*np.sin(phase*3)+.08*np.sin(phase*5)
            signal*=.76+.16*np.cos(2*np.pi*(12 if kind=='quad' else 19)*t)+.08*np.cos(2*np.pi*4*t)
            signal+=.11*np.sin(phase*.5)+.08*np.sin(phase*1.5+.12*np.sin(2*np.pi*8*t))
        signal*=env;signal-=signal.mean();signal*=.7/max(abs(signal))
        pcm=np.round(signal*32767).astype('<i2');name=kind+'_'+stage
        with wave.open(str(TMP/(name+'.wav')),'wb') as f:
            f.setnchannels(1);f.setsampwidth(2);f.setframerate(RATE);f.writeframes(pcm.tobytes())
        subprocess.run(['ffmpeg','-hide_banner','-loglevel','error','-y','-i',str(TMP/(name+'.wav')),'-c:a','libvorbis','-q:a','6',str(OUT/(name+'.ogg'))],check=True)
        manifest[name]={'sounds':[{'name':'shahed:'+name,'attenuation_distance':144 if stage=='engine' else 64}]}
        assert np.max(abs(signal))<.71
        if stage=='engine':assert abs(signal[0]-signal[-1])<.09
manifest_path.write_text(json.dumps(manifest,indent=2)+'\n',encoding='utf-8')
print('9 original mono OGG assets generated; PCM headroom and periodic loop seam checked')
