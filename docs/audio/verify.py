"""Static audio checks only. No game or audio playback is started."""
from pathlib import Path
import json,subprocess,hashlib,numpy as np
ROOT=Path(__file__).resolve().parents[2];ASSETS=ROOT/'src/main/resources/assets/shahed'
manifest=json.loads((ASSETS/'sounds.json').read_text());edits=json.loads((ROOT/'docs/audio/edits.json').read_text())
report={}
for name,event in manifest.items():
 p=ASSETS/'sounds'/(name+'.ogg');assert event['sounds'][0]['name']=='shahed:'+name
 info=json.loads(subprocess.check_output(['ffprobe','-v','error','-show_streams','-of','json',str(p)]))['streams'][0]
 assert info['codec_name']=='vorbis' and info['channels']==1 and int(info['sample_rate'])==48000,(name,info)
 raw=subprocess.check_output(['ffmpeg','-v','error','-i',str(p),'-f','f32le','-'])
 x=np.frombuffer(raw,dtype='<f4');assert np.isfinite(x).all() and np.max(abs(x))<.99,name
 rms=float(np.sqrt(np.mean(x*x)));assert rms>.001 and len(x)>10000,name
 r=dict(seconds=round(len(x)/48000,3),peak=round(float(np.max(abs(x))),5),rms=round(rms,5),bytes=p.stat().st_size,sha256=hashlib.sha256(p.read_bytes()).hexdigest())
 if edits.get(name,{}).get('loop') or name=='wind':
  seam=float(abs(x[-1]-x[0]));reference=float(np.quantile(abs(np.diff(x)),.995));edge=float(np.sqrt(np.mean(np.concatenate([x[-960:],x[:960]])**2)))
  assert seam<max(.025,reference*1.6),(name,'seam spike',seam,reference)
  assert edge>rms*.3,(name,'silent edge',edge,rms)
  r.update(seam=round(seam,6),edge_rms=round(edge,5))
 if name in edits:assert r['sha256']==edits[name]['sha256'],name
 report[name]=r
assert len(manifest)==18 and set(edits)<=set(manifest)
(ROOT/'docs/audio/validation.json').write_text(json.dumps(report,indent=2)+'\n')
print('18 mono 48kHz Vorbis resources: decoding, finite samples, headroom, paths and 5 loop seams OK; no listening/runtime verification')
