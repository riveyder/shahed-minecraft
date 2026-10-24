# AUDIO LICENSES — current assets, v1.0.2

Current release uses six third-party recordings. Downloads are the publicly served HQ MP3
previews, not lossless WAV originals. Licenses were checked on the individual Freesound
pages and their Creative Commons license links on 2026-10-24. No account-only downloads,
YouTube extraction, commercial-game audio, or unlicensed recordings are included.

CC BY 4.0 permits redistribution and adaptation with credit, license link and modification
notice; those credits below must accompany redistribution of the derived piston assets.
CC0 permits redistribution/adaptation without required attribution; creators are credited anyway.
No endorsement by any recording author or aircraft manufacturer is implied.

| Recording / source | Author | License | Distributed derivatives |
|---|---|---|---|
| [Plane Cessna start stop.wav](https://freesound.org/people/Cheeseheadburger/sounds/141519/) | Cheeseheadburger | [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/) | engine.ogg, startup.ogg, shutdown.ogg |
| [Drone taking off and landing](https://freesound.org/people/hoshisato/sounds/822500/) | hoshisato | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | quad_engine.ogg, quad_startup.ogg, quad_shutdown.ogg, contact_quad_engine.ogg, contact_quad_startup.ogg, contact_quad_shutdown.ogg |
| [Jet Engine Start Cart.wav](https://freesound.org/people/C-V/sounds/704945/) | C-V | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | jet_engine.ogg, jet_startup.ogg, jet_shutdown.ogg |
| [Sharp Explosion 4 (of 5)](https://freesound.org/people/Rudmer_Rotteveel/sounds/336011/) | Rudmer_Rotteveel | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | blast.ogg, blast_distant.ogg, blast_echo.ogg |
| [thunder rumble 1.wav](https://freesound.org/people/FenrirFangs/sounds/234736/) | FenrirFangs | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | blast.ogg, blast_distant.ogg, blast_echo.ogg |
| [Metal Latch](https://freesound.org/people/mm4wood/sounds/760626/) | mm4wood | [CC0 1.0](https://creativecommons.org/publicdomain/zero/1.0/) | payload_release.ogg |

## Modifications and provenance

All derivatives: stereo-to-mono downmix, 48 kHz resampling, DC removal, band limiting,
conservative gain/headroom and Vorbis encoding. Piston, multirotor and turbine loops use
equal-power overlap; contact variant has a slight recorded-sample speed shift. Startup/
shutdown files are short excerpts of recorded transitions, not complete physical sequences.
Explosion: recorded firework attack + slowed/filtered recording for the body + recorded
low thunder for atmospheric decay. Distant and echo assets derive from that master.
Payload release: one cropped metal-latch action. No synthesized oscillator/noise replacements.

Exact trim times, filters and derived file hashes: [audio/edits.json](https://github.com/riveyder/shahed-minecraft/blob/feat/stage-three/docs/audio/edits.json).
Exact source and public CDN URLs, authors, licenses and downloaded hashes:
[audio/sources.json](https://github.com/riveyder/shahed-minecraft/blob/feat/stage-three/docs/audio/sources.json). Reproduction: `python docs/audio/fetch_sources.py`,
followed by `python docs/audio/produce.py` (existing FFmpeg, NumPy and SciPy). Source MP3/PCM
stay in ignored `build/`; finished derivatives and credits are distributed. Credits are also
inside every mod JAR at `assets/shahed/AUDIO_CREDITS.md`.

## Retained sounds / limitations

`wind.ogg` and `impact.ogg` remain the original SHAHED procedural CC0 assets, authored
for this project by КРИВЫЕ РУКИ AEROSPACE; generator: [model/generate_audio.py](https://github.com/riveyder/shahed-minecraft/blob/feat/stage-three/docs/model/generate_audio.py).
The attempted replacement metal impact (SamsterBirdies 581596) and alternative jet loop
(smidoid 128613) returned HTTP 403; neither was downloaded or included. The turbine replacement
uses the successfully retrieved C-V recording. A better recorded impact/wind remains future work.
Public previews are already lossy. Recording descriptions and numerical checks do not establish
absence of background speech, audible seams, or perceived quality. In-game listening and
multiplayer audio acceptance have NOT been performed.
