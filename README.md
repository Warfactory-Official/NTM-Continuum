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

## Coming from NTM: Decentralized Edition

On Minecraft 1.21.1 NeoForge, a world saved by NTM: Decentralized Edition (DE) converts itself the first time Continuum
opens it. Remove the DE jar, add Continuum and Golem Core, and open the world.

- **Back up first.** The importer copies the world to `<world>_DE_backup_<date>` beside it before writing anything, but
  the conversion is one way: going back to DE means using that backup.
- It runs only when NeoForge reports that the world was last saved by another `hbm` version and the world carries DE's own
  data. `hbm_de_import.json` in the world records the result; a converted or declined world is never touched again.
- What could not be converted is kept, not deleted, in `data/hbm_de_leftovers.dat` (while the import runs, in
  `data/hbm_de_pending/`, one crash-safe file per converted store), and listed with positions in
  `hbm_de_import_report.txt`. Every converted stack, block entity and entity is checked against Continuum's own
  loading first, so anything Continuum would drop (a field, a component, a block property) lands there instead. `/hbm deimport status` counts it. Machines with no Continuum counterpart (the steam turbine,
  DE's test blocks) are removed and their contents put in a chest where they stood.
- PWR reactors and ICF arrays come back unassembled (PWR rods fully in): reassemble them. Create Aeronautics airships convert
  in place when Sable is installed; without it they wait in `sublevels.de-pending` and convert once Sable is added.
- A DE stack that loads later from another mod's storage (an AE2 drive, a backpack) converts the first time a player
  opens that storage, picks it up or joins with it; what it cannot keep goes to `data/hbm_de_legacy_leftovers.dat`.
- `-Dhbm.deimport=off` disables the importer; `/hbm deimport decline` marks a world it must never convert;
  `-Dhbm.deimport.backup=none` skips the backup when you made your own.

The importer is the `src/deimport` source root (`feature.de_import`), and the legacy layer for DE stacks in other mods'
storage (registry aliases, DE's split items and item components, the stack conversion itself) is `src/delegacy`
(`feature.de_legacy`, which the importer needs). Both are 1.21.1 NeoForge only; nothing else refers to them, so
removing a root and its flag removes it (`deimportBoundary` checks). Mapping tables come from
`port-tools/de_import/gen_de_tables.py` and its `overrides.tsv`.

## Addon policies

Addons provide environment policy through `Environment` hooks. `TERRAIN_FOG_RANGE` lets an addon own the
terrain fog range while HBM's soot color remains active; without a listener, HBM retains its default range.
Fog ranges use Golem Core's linear fog facade for compatibility with shader packs.

Material ingredients accept every item in their common material tag. Their typed material/shape recipe
declarations retain the canonical item used to generate recipe names.
