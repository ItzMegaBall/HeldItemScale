# Build report

**Command executed for the real build:** none. `gradlew.bat build` / `gradlew build` was NOT run.
**Why:** the build sandbox could reach only github.com. services.gradle.org, maven.fabricmc.net, piston-meta.mojang.com,
maven.terraformersmc.com and repo.maven.apache.org all failed to connect (checked with curl), so Gradle, Loom,
Minecraft and Mod Menu cannot be downloaded there.
**Build succeeded:** unknown (not attempted). **JAR:** none produced. No fake JAR is included.

## What WAS verified
- Versions: copied from the official `FabricMC/fabric-example-mod` branch `1.21.11` (Loader 0.20.1, Loom 1.19-SNAPSHOT, Gradle 9.8.2, Fabric API 0.141.6+1.21.11, Java 21) and the Mod Menu tag `v17.0.1` (targets 1.21.11). The Gradle wrapper files are the real ones from that template.
- Method signatures (Mojang names, 1.21.11) checked on mappings.dev: `ItemInHandRenderer.renderArmWithItem(AbstractClientPlayer, float, float, InteractionHand, float, ItemStack, float, PoseStack, SubmitNodeCollector, int)` and `ItemInHandRenderer.renderItem(LivingEntity, ItemStack, ItemDisplayContext, PoseStack, SubmitNodeCollector, int)`; `SubmitNodeCollector` is in `net.minecraft.client.renderer`. Mod Menu API (`ModMenuApi`, `ConfigScreenFactory`) read from its source.
- `fabric.mod.json` and the mixin config are valid JSON; description `Made By MegaBall_`, author `MegaBall_`, URL `https://megaball.netlify.app` match exactly.
- All Java files have no syntax errors (javac, ignoring missing Minecraft classes).
- `ModConfig` was compiled and RUN with stand-ins for the Fabric classes: 18/18 checks passed (file auto-created, round-trip, independent axes, clamping, NaN/string/missing values, partial files, corrupt JSON backup + recovery, non-object root, Reset Main Hand vs Reset All).

## NOT performed
Real Gradle build; compile against Minecraft; Mixin application; loading in 1.21.11; Mod Menu entry; config screen display, sliders and scrolling; any in-game rendering (scale, position, hand separation, arm unaffected, animations, maps); persistence across a real restart; Sodium/Iris/other-mod compatibility.

## Most likely failure points (in order)
1. `@Local(argsOnly = true)` / MixinExtras not on the compile classpath (it normally comes with Fabric Loader).
2. The `renderItem` call inside `renderArmWithItem` may be structured differently than expected, which would make the Mixin fail at startup.
3. A small GUI API difference in `ConfigScreen` (`StringWidget`, `AbstractSliderButton`, `mouseScrolled`).
