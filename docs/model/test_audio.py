"""Decode shipped OGG, test spatial format/headroom and loop continuity. Uses existing FFmpeg only."""
import array,json,math,subprocess,unittest
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
ASSETS=ROOT/'src/main/resources/assets/shahed'

def decode(name):
    p=subprocess.run(['ffmpeg','-v','error','-i',str(ASSETS/'sounds'/f'{name}.ogg'),'-f','f32le','-acodec','pcm_f32le','-'],capture_output=True,check=True)
    a=array.array('f');a.frombytes(p.stdout);return a

def rms(a):return math.sqrt(sum(v*v for v in a)/len(a))

class AudioTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):cls.samples={n:decode(n) for n in json.loads((ASSETS/'sounds.json').read_text(encoding='utf8'))}
    def test_decode_headroom_and_non_silent_duration(self):
        for n,a in self.samples.items():
            with self.subTest(sound=n):
                self.assertTrue(all(math.isfinite(v) for v in a))
                self.assertGreater(len(a),12000)
                self.assertLess(max(abs(v) for v in a),1.0,'decoded signal must not clip')
                self.assertGreater(rms(a),.015)
    def test_loop_join_has_no_spike_or_silent_window(self):
        for name in ('engine','wind'):
            a=self.samples[name]
            with self.subTest(sound=name):
                self.assertLess(abs(len(a)/48000-4),.01)
                jumps=sorted(abs(a[i]-a[i-1]) for i in range(1,len(a)))
                seam=abs(a[-1]-a[0]);reference=jumps[int(len(jumps)*.995)]
                self.assertLess(seam,max(.03,reference*1.5),'join is louder than ordinary waveform derivatives')
                edge=rms(list(a[-480:])+list(a[:480]));self.assertGreater(edge,rms(a)*.4,'no fade-to-silence gap at loop join')
                print(f'{name}: duration={len(a)/48000:.3f}s peak={max(abs(v) for v in a):.3f} rms={rms(a):.3f} seam={seam:.5f} edge_rms={edge:.3f}')
    def test_positional_ranges_and_manifest(self):
        manifest=json.loads((ASSETS/'sounds.json').read_text(encoding='utf8'))
        self.assertEqual(set(manifest),{'engine','startup','shutdown','wind','impact','blast','blast_echo'})
        for n,v in manifest.items():
            sample=v['sounds'][0];self.assertEqual(sample['name'],'shahed:'+n)
            self.assertGreaterEqual(sample['attenuation_distance'],40)
        self.assertEqual(manifest['blast']['sounds'][0]['attenuation_distance'],128)
if __name__=='__main__':unittest.main()
