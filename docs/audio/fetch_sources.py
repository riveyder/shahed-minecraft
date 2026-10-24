"""Fetch only public HQ previews whose per-sound licenses were checked; no login scraping."""
from pathlib import Path
import urllib.request,re,json,hashlib,concurrent.futures,datetime
ROOT=Path(__file__).resolve().parents[2]
SOURCES=[('piston','Cheeseheadburger',141519,'by/4.0'),('quad','hoshisato',822500,'publicdomain/zero/1.0'),('jet','C-V',704945,'publicdomain/zero/1.0'),('crack','Rudmer_Rotteveel',336011,'publicdomain/zero/1.0'),('rumble','FenrirFangs',234736,'publicdomain/zero/1.0'),('latch','mm4wood',760626,'publicdomain/zero/1.0')]
def fetch(entry):
 key,author,id,lic=entry
 manifest=ROOT/'docs/audio/sources.json';cached=ROOT/'build/audio-source'/(key+'.mp3')
 if manifest.exists() and cached.exists():
  for row in json.loads(manifest.read_text()):
   if row['key']==key and row['id']==id and row['sha256']==hashlib.sha256(cached.read_bytes()).hexdigest():return row
 url=f'https://freesound.org/people/{author}/sounds/{id}/'
 page=urllib.request.urlopen(url,timeout=30).read().decode()
 license_url='https://creativecommons.org/'+('licenses/' if lic.startswith('by') else '')+lic+'/'
 assert license_url in page or license_url.replace('https:','http:') in page,(key,'license link absent')
 links=re.findall(r'https://cdn.freesound.org/previews/[^\s<>"\x27]+-hq.mp3',page);assert links,key
 data=urllib.request.urlopen(links[0],timeout=30).read()
 out=ROOT/'build/audio-source';out.mkdir(parents=True,exist_ok=True);(out/(key+'.mp3')).write_bytes(data)
 return dict(key=key,author=author,id=id,source=url,license=license_url,download=links[0],sha256=hashlib.sha256(data).hexdigest(),bytes=len(data),retrieved=datetime.date.today().isoformat(),format='Public HQ MP3 preview; not lossless original')
if __name__=='__main__':
 with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:results=list(pool.map(fetch,SOURCES))
 (ROOT/'docs/audio/sources.json').write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8')
 for r in results:print(r['key'],r['bytes'],r['license'])
