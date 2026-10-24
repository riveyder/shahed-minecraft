"""Asset regressions: source scale, UV bounds, editable identity, generated parity."""
import json
import base64
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

class ModelTests(unittest.TestCase):
    def test_embedded_atlas_matches_packaged_texture_and_outliner(self):
        model=json.loads((ROOT/'assets-work/Shahed_Sheep_Overhaul.bbmodel').read_text(encoding='utf-8'))
        embedded=base64.b64decode(model['textures'][0]['source'].split(',')[1])
        self.assertEqual(embedded,(ROOT/'src/main/resources/assets/shahed/textures/entity/drone.png').read_bytes())
        leaves=[child for node in model['outliner'] for child in (node['children'] if isinstance(node,dict) else [node])]
        self.assertEqual(set(leaves),{e['uuid'] for e in model['elements']})
        self.assertEqual(len(leaves),len(model['elements']))
        self.assertEqual(model['resolution'],{'width':512,'height':512})

    def test_sound_and_item_resources_resolve(self):
        assets=ROOT/'src/main/resources/assets/shahed'
        sounds=json.loads((assets/'sounds.json').read_text())
        self.assertEqual(set(sounds),{'engine','startup','shutdown','wind','impact','blast','blast_echo','blast_distant'})
        for name,event in sounds.items():
            for sound in event['sounds']:
                data=(assets/'sounds'/(sound['name'].split(':')[1]+'.ogg')).read_bytes()
                self.assertTrue(data.startswith(b'OggS'),name)
                self.assertIn(b'vorbis',data[:100],name)
        for item in ('drone','training_drone','controller','fpv_goggles'):
            definition=json.loads((assets/'items'/f'{item}.json').read_text())
            model=definition['model']['model'].split(':')[1]
            self.assertTrue((assets/'models'/(model+'.json')).is_file(),model)

    def test_source_geometry_retained_at_exact_scale(self):
        source = json.loads((ROOT/'assets-work/Shahed_Sheep.bbmodel').read_text(encoding='utf-8'))
        model = json.loads((ROOT/'assets-work/Shahed_Sheep_Overhaul.bbmodel').read_text(encoding='utf-8'))
        by_id = {e['uuid']:e for e in model['elements']}
        self.assertEqual(len(by_id),len(model['elements']))
        self.assertGreater(len(by_id),len(source['elements'])+15)
        for original in source['elements']:
            e = by_id[original['uuid']]
            for axis in range(3):
                self.assertAlmostEqual(e['to'][axis]-e['from'][axis],
                                       (original['to'][axis]-original['from'][axis])*2.5)
        java=(ROOT/'src/client/java/io/github/riveyder/shahed/client/model/DroneModel.java').read_text()
        self.assertEqual(java.count('box(root,')+java.count('box(propeller,'),len(by_id))
        for e in model['elements']:
            self.assertGreaterEqual(e['from'][1],0)
            for face in e['faces'].values():
                self.assertTrue(all(0<=v<=512 for v in face['uv']))
                self.assertEqual(face['texture'],0)

if __name__=='__main__': unittest.main()
