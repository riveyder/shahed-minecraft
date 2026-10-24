# v1.0.1 — multi-version verification

Baseline: `88b4635`, v1.0.0 for Minecraft 26.2. Development branch: `feat/stage-three`.

| Target | Java classfiles/runtime | Fabric API | Result |
|---|---|---|---|
| 1.20.1 | 17 (major 61) | 0.92.12+1.20.1 | BUILD SUCCESSFUL; JAR audit passed |
| 1.21.1 | 21 (major 65) | 0.116.17+1.21.1 | BUILD SUCCESSFUL; JAR audit passed |
| 26.2 | 25 (major 69) | 0.161.0+26.2 | BUILD SUCCESSFUL; JAR audit passed |

Loader 0.19.5, Loom 1.18.4, existing Gradle wrapper 9.7.1. Gradle itself uses JDK25;
this does not impose Java25 on players of the older targets. Python3.10+ prepares ports.
The final 26.2 verification was completed on 2026-10-24 after resuming saved work.

## Completed

- [x] Independent `ports` Gradle targets, common source/resource generation and narrow adapters.
- [x] 1.20.1 legacy packet channels, item NBT, recipes/advancements, immediate renderer.
- [x] 1.21.1 typed payload API, components, resource format and renderer adapters.
- [x] Shared fleet, payload state, ownership, controls, settings, effects, races and replay compiled.
- [x] Undamageable head-slot goggles without introducing armor bonuses or maintenance.
- [x] Version-specific camera/chunk tracking, server authority, explosion policy and protection hooks.
- [x] Preserve root 26.2 implementation; change only versioned build metadata/documentation.
- [x] Named JARs, complete resource inspection and local development commits.
- [ ] Manual gameplay and dedicated-server acceptance (not performed).

## Build evidence

No Minecraft, runClient, runServer, GameTests, graphics or multiplayer simulations ran.
Porting required compile-error iterations; completed builds were repeated only for relevant changes.
Final logs are local ignored files:

- `build/port1201-build.log`: SUCCESS, 28s, goggles correction included.
- `build/port1211-build.log`: SUCCESS, 23s, goggles correction included.
- `build/multiversion-26.2-build.log`: SUCCESS, 11s; existing classes up-to-date.

Commands: `gradlew.bat -p ports -Pmc=<target> build -x test` for old targets;
`gradlew.bat build -x check -x test -x runGameTest` for 26.2.
`python -B ports/tools/audit_jar.py <target> [jar-path]` validates exact metadata,
all JSON, classfile levels, eight recipes/items, entity textures, all referenced audio,
particle sprites and registered mixin class presence. Every target contains 17 OGG and 23 PNG.
The 26.2 comparison with v1.0.0 confirmed all 190 class/asset entries byte-identical.
Mixin hook names and important invocation descriptors were reviewed against mapped bytecode;
this is static evidence, not proof of runtime application or compatibility with other mods.

## Artifacts and SHA-256

- `ports/build/1.20.1/libs/shahed-fpv-mayhem-1.0.1+mc1.20.1.jar` (857819 bytes)
  `e1e8c7b7d6394815dd043f8698b7e756e60b5c40dfa532b12044f892a053b5a7`
- `ports/build/1.21.1/libs/shahed-fpv-mayhem-1.0.1+mc1.21.1.jar` (847164 bytes)
  `eb257b8812e9ec6f992d2ba774b87eea41af8f3ed3463b5438ace13fd41ed4f5`
- `build/libs/shahed-fpv-mayhem-1.0.1+mc26.2.jar` (827357 bytes)
  `295b9986d6bd1a912ddc20df8fa14495119bb3bbdf458f02685cbf7826d7aaed`

## Compatibility limits

No other intermediate version is claimed supported. Versions between these targets
would need their own dependency, format and API checks; expanding a metadata range
would not establish compatibility. The three explicitly compiled targets are the release scope.
Older games lack the 26.2 tntExplodes rule. Their mod toggles/protection vetoes still
control gameplay, PvP uses the server PvP setting, and TNT drop decay uses its native rule.
Terrain counts can differ between Minecraft engines; no identical crater guarantee is made.

Config fields, race JSON, item/entity IDs and binding UUID keys are preserved. No silent
race/config migration is introduced. Minecraft worlds and item serialization are not
cross-version interchangeable: do not downgrade a 26.2 world. Use matching client/server
JARs, separate game directories/worlds and backups. Original audio/model licenses unchanged.

## Manual acceptance, independently on each target

1. Start a clean Fabric profile; check loading/Mixin logs, eight Creative items, recipes
   and recipe-book unlock, Russian/English labels, undamageable head-slot goggles.
2. Deploy all aircraft; bind as owner and reject another player; reconnect and reload
   saved entities/controllers/config/race records. Check deployment/admin restrictions.
3. Fly, hover/land, animate all propellers, verify FPV bank/FOV/night vision and settings.
   Travel beyond the player body's chunks; return safely. Repeat on a dedicated server.
4. Attach/release one payload: LOADED/EMPTY on a second client, no duplicate consumption
   or release. Check high-speed block/entity impacts and single destruction notification.
5. SAFE/NORMAL/CHAOS, protection vetoes, PvP, fire, smoke, distant audio, moving engines,
   camera shake and quality limits; terrain enabled/disabled independently from effects.
6. Concurrent training races, high-speed gate passage, records and cancellation; no terrain damage.
7. SIGNAL LOST and replay after destruction: correct aircraft, three-second aftermath,
   no repeated damage, camera restoration and disconnect cleanup.

Sources: https://wiki.fabricmc.net/install ; https://fabricmc.net/2026/03/14/261.html ;
https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml .
