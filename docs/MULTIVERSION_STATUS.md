# Multi-version port status — 1.0.1

Baseline: 88b4635, v1.0.0 for Minecraft 26.2, clean working tree.
- [x] 1.20.1: compilation/remapping successful; runtime untested. shared-source adapter target; Java 17, Fabric API 0.92.12+1.20.1.
- [ ] 1.21.1: follow after 1.20.1; Java 21, Fabric API 0.116.17+1.21.1.
- [ ] 26.2: preserve source, rename/version final artifact after ports.
- [ ] Resource conversions, compilation, JAR audit, docs/commits/push.

Architecture: independent `ports` Gradle build (Loom remap + official Mojang mappings),
generated shared sources plus authored narrow adapters. Root 26.2 build stays unchanged.
No Minecraft/GameTest processes. No old target is supported until compilation succeeds.

Sources: https://wiki.fabricmc.net/install ; https://fabricmc.net/2026/03/14/261.html ;
https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml .
Java 17/21 are installed locally. Gradle itself uses the existing JDK 25; old target
classfiles and game runtime requirements remain Java 17/21.

1.20.1: all shared gameplay and client sources compile; packet, NBT, rendering,
remote chunk camera, explosion policy, equipment and recipe adapters implemented.
17 OGG files / 23 PNG files packaged. Final recipe-book legacy predicate conversion
added following JAR inspection. No game process launched.
