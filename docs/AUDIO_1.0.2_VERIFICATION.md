# v1.0.2 — audio verification / manual acceptance

Baseline `fce8028`, branch `feat/stage-three`; 2026-10-24.
This is recording-based sound design, not an authenticity claim for a particular aircraft.
No Minecraft, GameTests, graphical client, multiplayer simulation or audio playback ran.

## Assets

- Replaced 15 previous assets: engine/startup/shutdown for SHAHED, both quads and jet;
  blast/blast_distant/blast_echo. Added payload_release. All six source recordings were
  fetched as public HQ MP3 previews; exact license/source/download hashes in `audio/sources.json`.
- Cheeseheadburger's piston source is CC BY4.0. hoshisato, C-V, Rudmer_Rotteveel,
  FenrirFangs and mm4wood sources are CC0. Detailed attribution and modifications in
  `AUDIO_LICENSES.md` and in each JAR's `assets/shahed/AUDIO_CREDITS.md`.
- Original synthetic wind/impact are retained. Attempted SamsterBirdies metal impact
  and smidoid alternative jet loop returned HTTP403 and were not included. Jet uses C-V.
- These are lossy public previews, not studio lossless masters. Source descriptions and
  signal checks cannot verify perceived realism or absence of background speech/music.
  No claim of listening, professional auditory approval or in-game sound quality is made.

## Signal / resource checks

`python -B -X utf8 docs/audio/verify.py` passed:
18 mono 48kHz Vorbis, finite samples, no decoded clipping, positive signal energy,
manifest/resource matching and five engine/wind loop seam checks. Max decoded peak
0.81302 (full scale 1); 594447 bytes total OGG assets. Results: `audio/validation.json`.
Loop overlaps prevent a discontinuity mathematically; audible seams still need listening.
All 18 sound event IDs match registration. Every target JAR contains identical sound
bytes and CC BY credits. `ports/tools/audit_jar.py` passed on all targets, including
Java classfile level, metadata, recipes, particles and models. No repeated benchmarks.

## Compile results

| Target | Java | Build result | Local log |
|---|---|---|---|
| Minecraft1.20.1 | 17 | SUCCESS, 16s | build/audio102-mc1201.log |
| Minecraft1.21.1 | 21 | SUCCESS, 15s | build/audio102-mc1211.log |
| Minecraft26.2 | 25 | SUCCESS, 10s | build/audio102-mc262.log |

Each target compiled once after implementation; no compile retries needed.
Old targets: `gradlew.bat -p ports -Pmc=<target> build -x test --offline --no-daemon`.
26.2: `gradlew.bat build -x check -x test -x runGameTest --offline --no-daemon`.
JAR names, sizes and SHA256: `audio/artifacts.json`. Other Minecraft versions are not claimed.

## Integration review

- Engine/transition positions update from the tracked entity. Separate aircraft gains
  apply live. A legacy config copies engineVolume into absent quadVolume/jetVolume.
- Start/stop follows throttle hysteresis; short recorded transitions are not simulations
  of real startup sequences. Radial pitch adjustment is restrained to ±10%.
- Wind now follows the aircraft; quiet observer layer changes with speed. Existing asset retained.
- Near/distant explosion layers crossfade; attenuation reaches zero smoothly at the configured
  audio range. Existing same-dimension reliable event broadcast and UUID dedup remain intact.
  Remote pilots are evaluated by camera distance, not their physical player-body distance.
- mechanical_sound_v1 is S2C only: emitted after successful release, and once after guarded
  payload impact. Audio does not spawn projectiles, apply damage or change inventory.
  256 dedup IDs / 16 one-shots; engine transitions capped32, original engine pool32,
  explosion pool24. Sounds stop on removal/dimension change/disconnect; no permanent emitters.
- New server entity changes are only the two sound emission calls. Flight physics, destruction
  strength/config, race logic, models, camera/HUD and replay implementation are unchanged.

## Manual checklist (each supported Minecraft target)

1. Listen on headphones and speakers: engine loops at hover/idle/full throttle, recorded
   transitions and all explosion layers. Check clicks, background voices, harshness and balance.
2. Move all aircraft past a second player; verify stereo direction, distance fade and moving
   startup/shutdown position. Destroy/despawn an aircraft, change dimension and disconnect:
   no orphan sounds. Repeat after F3+T resource reload.
3. Set each SHAHED/FPV/jet slider to zero separately while aircraft are active. Confirm other
   categories remain audible. Check wind, interface, explosion and distant rumble independently;
   close menu/restart and test migration from an old config, including old engineVolume=0.
4. Use SAFE test explosions at near, medium and configured far distance, including a remote
   FPV operator whose physical body is outside the sound range. Verify smooth blending, optional
   echo, no double playback and vanilla master/Neutral volume controls.
5. Attach one payload, release once and attempt rapid repeats: one mechanical sound, one payload,
   one impact/explosion. Listen from operator and nearby observer. Existing retained impact needs
   a better recording when a redistributable source is available.
6. On a dedicated server verify simultaneous aircraft, bounded channels, matching client/server
   v1.0.2 and no change to SAFE/NORMAL/CHAOS damage or race safety.
