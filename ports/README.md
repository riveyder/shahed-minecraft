# Independent legacy Fabric targets

The root project remains the Minecraft 26.2 implementation. This build consumes
its gameplay, configuration, packet schemas, fleet models, audio and resources.
`tools/prepare.py` generates `build/<mc>/generated`; do not edit generated sources.
`transforms.py` / `client.py` apply mechanical translations. `adapters/common` and
`adapters/<mc>` supply changed engine APIs; `adapters.py` adapts their narrow 1.21 differences.
No new gameplay dependencies or duplicate asset library are introduced.

From the repository root (existing Gradle wrapper runs on JDK25):

```powershell
.\gradlew.bat -p ports '-Pmc=1.20.1' build -x test
.\gradlew.bat -p ports '-Pmc=1.21.1' build -x test
python -B ports/tools/audit_jar.py 1.20.1
python -B ports/tools/audit_jar.py 1.21.1
```

Install JDK17/21 for the compiler toolchains. Gradle discovers local JDKs; environment
variables JAVA_HOME_17_X64 / JAVA_HOME_21_X64 are also recognized. For unusual locations
pass `-Dorg.gradle.java.installations.paths=<JDK path>`. Python3.10+ prepares sources.
Linux uses `./gradlew` and `python` pointing to Python3.10+. Generated files are ignored.
Output: `ports/build/<mc>/libs/`. No Minecraft/runtime test tasks are invoked.

Official Mojang mappings are used for compilation, then remapped to Fabric intermediary.
The root 26.2 implementation retains its unobfuscated Loom build.
1.20.1 uses legacy Fabric channels, item NBT, Java17 collection adapters and plural
recipe/advancement directories. 1.21.1 uses typed payloads, components and singular
resource directories. Both use Equipable for undamageable goggles, immediate rendering,
local camera hooks, remote chunk leases and a server explosion policy adapter.
Older games have no 26.2 tntExplodes rule: the SHAHED server toggles, protection hooks,
server PvP setting and TNT drop-decay rule govern the applicable operations.

Entity IDs, controller UUID keys, config fields and race JSON are retained. This is
not a world downgrader or cross-version multiplayer protocol. Use separate worlds and
matching client/server targets. Do not open a 26.2 world in an older game.
Compilation does not verify runtime Mixins, camera streaming, flight or multiplayer.
Follow the manual checklist in `docs/MULTIVERSION_STATUS.md`.
