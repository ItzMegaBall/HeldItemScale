# Held Item Scale

Client-side Fabric mod for **Minecraft Java Edition 1.21.11** that lets you change the **X / Y / Z scale** and
**X / Y / Z position** of the item you hold in **first person**, separately for the main hand and the off-hand.
It changes rendering only: no gameplay, hitbox, reach, durability, inventory or server behaviour is touched,
and it does not need to be installed on a server.

Made By MegaBall_ - https://megaball.netlify.app

> **Status: source is complete but has NOT been compiled or run in-game yet.** See `BUILD_REPORT.md`.
> Read "If the build fails" below; the most likely trouble spot is the single Mixin.

## Features
- Main hand and off-hand each have X/Y/Z scale (0.10 to 3.00, default 1.00) and X/Y/Z position (-1.00 to 1.00, default 0.00).
- Settings screen from Mod Menu: 12 sliders with numeric values, Reset Main Hand, Reset Off-Hand, Reset All, Done. Scrolls with the mouse wheel in small windows.
- Changes apply live; they are saved to disk when you close the screen.
- Only the held item model is transformed. The arm, hotbar, inventory, third-person view, other players, dropped items, item frames and armor stands are not.

## Versions (taken from official sources)
| Part | Version | Source |
|---|---|---|
| Minecraft | 1.21.11 | |
| Java | 21 | official example mod for 1.21.11 builds with `release = 21` |
| Fabric Loader | 0.20.1 | FabricMC/fabric-example-mod, branch `1.21.11` |
| Fabric Loom | 1.19-SNAPSHOT (`net.fabricmc.fabric-loom-remap`) | same |
| Gradle | 9.8.2 (via wrapper) | same |
| Fabric API | 0.141.6+1.21.11 | same (only used to run the dev client) |
| Mod Menu | 17.0.1 | TerraformersMC/ModMenu tag `v17.0.1` (targets 1.21.11) |
| Mappings | Official Mojang | same template |

## Install (Windows)
1. Install the Fabric Loader for 1.21.11 from https://fabricmc.net/use/installer/ and create the Fabric profile.
2. Start the game once with that profile, then close it.
3. Find your instance's `mods` folder. Default launcher: press `Win+R`, type `%appdata%\.minecraft\mods`, Enter. Other launchers (Prism, CurseForge, etc.): open the instance and choose "Open folder / Open mods folder".
4. Put these in `mods`: **Fabric API** (1.21.11), **Mod Menu** 17.x, and `helditemscale-1.0.0.jar` (the one WITHOUT `-sources` in its name).
5. Start the game with the Fabric profile, open **Mods**, pick **Held Item Scale**, and press the config button.
6. Hold a different item in each hand to test them independently.

## Build
You need a JDK 21 and internet access.
```bat
gradlew.bat build
```
The mod is `build\libs\helditemscale-1.0.0.jar`. Ignore `helditemscale-1.0.0-sources.jar`.
To test without installing: `gradlew.bat runClient`.

## Configuration file
`<instance folder>\config\helditemscale.json`, created automatically. Example:
```json
{ "mainHand": { "scaleX": 1.0, "scaleY": 1.0, "scaleZ": 1.0, "positionX": 0.0, "positionY": 0.0, "positionZ": 0.0 },
  "offHand":  { "scaleX": 1.0, "scaleY": 1.0, "scaleZ": 1.0, "positionX": 0.0, "positionY": 0.0, "positionZ": 0.0 } }
```
Invalid or missing values fall back to defaults while valid ones are kept. A corrupted file is renamed to
`helditemscale.json.broken-<timestamp>` and a fresh one is created.

## How it works
One Mixin (`ItemInHandRendererMixin`) wraps the call to `ItemInHandRenderer.renderItem(...)` inside
`ItemInHandRenderer.renderArmWithItem(...)`, the first-person method. It uses MixinExtras' `@WrapOperation`
(bundled with Fabric Loader) so it wraps instead of replacing code, and it reads the hand from that method's own
`InteractionHand` argument. The arm is drawn by different calls, so it is unaffected. A matrix is pushed before the call
and popped in a `finally` block. All math lives in `HeldItemTransform`.
No public Fabric API hook exists for this, which is why a Mixin is used.

## Known limitations
- **Maps** (held map and two-handed map) are drawn by a different path and are not scaled.
- "Main hand" means the main-hand slot, even if you set your main hand to Left in the game options.
- Position axes are in the held item's own space after vanilla hand/swing transforms, so for the off-hand the X direction may look mirrored. Scaling pivots on that same origin, so large scales can look like they move toward or away from you; use the position sliders to compensate.
- Not tested with Sodium, Iris, or other mods. Resource-pack/custom item models go through the same call, so they should be scaled too, but this is untested.

## Troubleshooting
- **`Unsupported class file` / wrong Java:** run `java -version`; you need 21. Install a JDK 21 and set `JAVA_HOME`.
- **Gradle can't download things:** check your internet/firewall/VPN, then rerun `gradlew.bat build --refresh-dependencies`.
- **Compile error mentioning a Minecraft class or method:** send me the full error text; names were checked against mappings.dev for 1.21.11 but not against a real compile.
- **Crash at startup mentioning `ItemInHandRendererMixin` / "injection ... failed":** the call being wrapped was not found. Send me `logs\latest.log`.
- **Mod does not appear in the list:** the wrong JAR is in `mods`, or the game is not on the Fabric profile. Check `logs\latest.log` for `helditemscale`.
- **No config button:** Mod Menu or Fabric API is missing from `mods`.
- **Reset the settings by hand:** close the game and delete `config\helditemscale.json`.

License: CC0-1.0 (from the Fabric template; change `LICENSE` and `fabric.mod.json` if you prefer another).
