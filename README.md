# NTM - Continuum

## Setting up the project

1. **Clone this repository and Golem Core next to each other.** The build includes Golem Core from `../Golem-Core`, so
   the folder must have that name:

   ```bash
   git clone https://github.com/Warfactory-Official/Golem-Core.git
   git clone https://github.com/Warfactory-Official/NTM-Continuum.git
   ```

2. **Java.** Any JDK 17 or newer runs Gradle. The JDKs each Minecraft version needs (25 for 26.1, 21 for 1.21.1, 17 for
   1.20.1) are downloaded by Gradle on first use.

3. **Build Golem Core, then the mod**, on every version:

   ```bash
   cd Golem-Core && ./gradlew chiseledBuild
   cd ../NTM-Continuum && ./gradlew chiseledBuild
   ```

   Rebuild Golem Core this way whenever you change it or its version.

4. **Open `NTM-Continuum` in IntelliJ IDEA** and import it as a Gradle project. Golem Core comes in with it.

## Running

The source tree is shared by six versions (Stonecutter); `26.1-neoforge` is the active one by default.

```bash
./gradlew :26.1-neoforge:runClient            # a client
./gradlew :26.1-neoforge:runGameTestServer    # the GameTests, headless
./gradlew :1.20.1-forge:runClient             # any other version: 26.1-fabric, 1.21.1-neoforge, 1.21.1-fabric, 1.20.1-fabric
```

To edit against another version, run the Stonecutter task `Set active project to <version>` from the Gradle panel, and
`Reset active project` before committing.

The in-game test harness is documented in [`../Golem-Core/harness/README.md`](../Golem-Core/harness/README.md).

## Addon policies

Addons provide environment policy through `Environment` hooks. `TERRAIN_FOG_RANGE` lets an addon own the
terrain fog range while HBM's soot color remains active; without a listener, HBM retains its default range.
Fog ranges use Golem Core's linear fog facade for compatibility with shader packs.

Material ingredients accept every item in their common material tag. Their typed material/shape recipe
declarations retain the canonical item used to generate recipe names.
