# Ender Utilities Reforged

Unofficial [NeoForge](https://neoforged.net/) 1.21.1 port of [Ender Utilities](https://github.com/maruohon/enderutilities) by masa.

- **Mod ID:** `enderutilities` (item/block ids stay orig `enderutilities:...`)
- **Artifact / JAR:** `ender-utilities-reforged`
- **Package:** `dev.celli.enderutilities`
- **Author:** Cellicia_
- **License:** LGPL-3.0 (same family as the original)

## Requirements

- JDK **21** (Minecraft 1.21.1)
- The Gradle wrapper in this repo (no global Gradle install needed)

If `java -version` is not 21, install Temurin 21 and point `JAVA_HOME` at it.

On this PC you already have a 64-bit JDK 21 at:

`C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot`

Use that (or `C:\Program Files\BellSoft\LibericaJDK-21`). Do **not** use `C:\Program Files (x86)\BellSoft\LibericaJDK-21-Full` — that one is 32-bit and Gradle will fail with “Could not reserve enough space for object heap”.

## First-time setup

From this folder:

```bat
gradlew.bat genIntellijRuns
```

That downloads Minecraft, NeoForge, and mappings. Then in IntelliJ: **File → Open** this folder, wait for Gradle import, and use the **runClient** run configuration.

Command-line equivalent:

```bat
gradlew.bat runClient
```

Build a jar (goes to `build/libs/`):

```bat
gradlew.bat build
```

## What is already wired

- Lasso, Ender Bag, Link Crystals, capacitors, Jailer, parts, Tool Workstation
- Original textures for those items
- Recipes matching the 1.12 patterns (glass colors use stained glass; wool uses the wool tag)

### Using the Ender Lasso / Ender Bag (1:1 flow)

1. Craft parts: Basic Alloy, Enhanced Alloy, Inactive Enhanced Core, Ender Rope.
2. Craft a **Tool Workstation**, a **Location** or **Block** Link Crystal, and an **Ender Capacitor**.
3. Put the Lasso/Bag in the workstation and install:
   - Lasso: Location crystal + capacitor (+ optional Jailer)
   - Bag: Block crystal + capacitor
4. Charge the capacitor by right-clicking an **Ender Crystal in The End**, or use the Creative capacitor from the creative tab (it starts with charge).
5. **Lasso:** sneak + right-click a block to bind. Right-click a mob to teleport it. **Shift+G** toggles bind lock. **Ctrl+G** cycles crystals.
6. **Bag:** sneak + right-click a vanilla **Ender Chest** to bind. Right-click air to open your ender inventory. Other chests are still disabled, same as 1.12 after 1.10.

Toggle Mode defaults to **G**, same as the original.

Energy Bridge / Ender Infuser are not in yet, so survival charging is End Crystals only for now.
