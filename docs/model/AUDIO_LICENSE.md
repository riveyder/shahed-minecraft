# Audio provenance

The six mono Vorbis files in `src/main/resources/assets/shahed/sounds/` were
procedurally authored for this project by `generate_audio.py`: sine harmonics,
seeded noise and amplitude envelopes. No recordings, samples, or third-party
sound libraries were incorporated. These generated audio assets are dedicated
to the public domain under CC0 1.0. The script is covered by the repository license.

Rebuild with Python 3 and an existing FFmpeg with libvorbis:
`python docs/model/generate_audio.py`. Temporary PCM files stay in `build/audio`.
Gameplay uses only packaged OGG files; no runtime FFmpeg dependency exists.
