"""Recorded-source edit recipe. Offline; requires fetched files, NumPy/SciPy and FFmpeg.
No synthesized oscillators/noise. All layers derive from credited recordings.
"""
from pathlib import Path
import json,hashlib,subprocess,numpy as np
from scipy.signal import butter,sosfiltfilt,resample_poly
ROOT=Path(__file__).resolve().parents[2];SR=48000
OUT=ROOT/'src/main/resources/assets/shahed/sounds';PCM=ROOT/'build/audio-master';PCM.mkdir(parents=True,exist_ok=True)
sources={}
for row in json.loads((ROOT/'docs/audio/sources.json').read_text()):
 p=ROOT/'build/audio-source'/(row['key']+'.mp3');assert hashlib.sha256(p.read_bytes()).hexdigest()==row['sha256']
 data=subprocess.check_output(['ffmpeg','-v','error','-i',str(p),'-ac','1','-ar',str(SR),'-f','f32le','-'])
 sources[row['key']]=np.frombuffer(data,dtype='<f4').astype(np.float64)
report={}
def band(x,lo=50,hi=10000):return sosfiltfilt(butter(2,[lo,hi],btype='bandpass',fs=SR,output='sos'),x)
def cut(key,start,end):return sources[key][int(start*SR):int(end*SR)].copy()
def peak(x,p=.70):return x*(p/max(abs(x).max(),1e-8))
def fade(x,start=.01,end=.12):
 x=x.copy();n=min(int(start*SR),len(x)//2);m=min(int(end*SR),len(x)//2)
 if n:x[:n]*=np.linspace(0,1,n)**.5
 if m:x[-m:]*=np.linspace(1,0,m)**1.5
 return x
def loop(x,cross=.5):
 n=int(cross*SR);t=np.linspace(0,np.pi/2,n)
 # End blends into the original beginning. The new boundary joins adjacent source samples.
 return np.concatenate([x[n:-n],x[-n:]*np.cos(t)+x[:n]*np.sin(t)])
def write(name,x,keys,edit,isloop=False,rms=None):
 x=x-np.mean(x);x=peak(x,.68 if isloop else .75)
 if rms:x*=min(1,rms/np.sqrt(np.mean(x*x)))
 raw=PCM/(name+'.f32');raw.write_bytes(x.astype('<f4').tobytes())
 subprocess.run(['ffmpeg','-v','error','-y','-f','f32le','-ar',str(SR),'-ac','1','-i',str(raw),'-c:a','libvorbis','-q:a','5','-map_metadata','-1',str(OUT/(name+'.ogg'))],check=True)
 report[name]=dict(sources=keys,edit=edit,loop=isloop,seconds=round(len(x)/SR,3),sha256=hashlib.sha256((OUT/(name+'.ogg')).read_bytes()).hexdigest())
write('engine',loop(band(cut('piston',10,17),55,8500),1),['piston'],'10–17s, bandpass, 1s equal-power overlap',True,.14)
write('startup',fade(band(cut('piston',0,3.6),55,8500),.02,.4),['piston'],'0–3.6s startup excerpt, bandpass, edge fades')
write('shutdown',fade(band(cut('piston',24.7,28.5),55,8500),.06,.6),['piston'],'24.7–28.5s shutdown excerpt, bandpass, edge fades')
q=loop(band(cut('quad',5.8,10.8),180,12000),.8)
for prefix in ['quad','contact_quad']:
 # Same real multirotor family; contact variant is a lightly shifted recording, not a beep generator.
 variant=q if prefix=='quad' else resample_poly(q,100,104)
 write(prefix+'_engine',variant,['quad'],'5.8–10.8s; 0.8s overlap; contact speed +4%',True,.115)
 write(prefix+'_startup',fade(band(cut('quad',4.65,6.35),180,12000),.01,.25),['quad'],'4.65–6.35s motor spool-up excerpt')
 write(prefix+'_shutdown',fade(band(cut('quad',42.7,45.4),180,12000),.03,.4),['quad'],'42.7–45.4s landing motor decay excerpt')
write('jet_engine',loop(band(cut('jet',45,54),75,10500),1),['jet'],'45–54s steady start-cart turbine, bandpass and 1s overlap',True,.135)
write('jet_startup',fade(band(cut('jet',13,16.5),75,10500),.08,.5),['jet'],'13–16.5s turbine rise excerpt; not a full physical startup sequence')
write('jet_shutdown',fade(band(cut('jet',104,108.5),75,10500),.03,.7),['jet'],'104–108.5s turbine decay excerpt')
crack=peak(band(cut('crack',0,1.048),60,14500))
body=peak(band(resample_poly(crack,5,2),40,900))
tail=peak(band(cut('rumble',0,8),35,900))
mix=np.zeros(8*SR);mix[:len(crack)]+=.72*crack;mix[:len(body)]+=.40*body
mix+=.24*fade(tail,.10,3)*np.exp(-np.arange(len(tail))/(SR*5))
write('blast',fade(mix,.001,.6),['crack','rumble'],'Recorded firework transient + slowed filtered body + recorded thunder decay; 8s layered master')
far=band(mix,35,850);write('blast_distant',fade(far,.035,1.2),['crack','rumble'],'Low-passed near master; softened attack and extended recorded decay')
echo=np.zeros(9*SR)
for seconds,gain in [(0.0,.35),(.37,.23),(.81,.13)]:
 offset=int(seconds*SR);echo[offset:offset+len(far)]+=far*gain
write('blast_echo',fade(band(echo,80,1600),.15,1),['crack','rumble'],'Three quiet filtered reflections; client delays optional echo once')
write('payload_release',fade(band(cut('latch',3.16,3.72),100,11500),.006,.10),['latch'],'Single mechanical latch movement, 3.16–3.72s, bandpass/fades')
# Original wind and impact are intentionally preserved: no unverified replacement disguised as a recording.
(ROOT/'docs/audio/edits.json').write_text(json.dumps(report,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
print('Edited',len(report),'recording-based assets')
