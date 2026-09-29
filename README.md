# NTM - Continuum

## Testing in game (harness)

The in-game test harness lives in Golem Core: see
[`../Golem-Core/harness/README.md`](../Golem-Core/harness/README.md) for setup and usage. This repository holds
HBM's part of it:

- `harness.json` — the node table, listener ports and paths.
- `harness/scenarios/*.java` — the scenarios (`Final*.java` cover every ported family; run them on all six nodes).
- `harness/checks/*.java` — client smoke checks. `harness/fixtures/*.nbt` — structure templates.
- `harness/run-suite.ps1`, `run-client-matrix.ps1`, `run-gametest-matrix.ps1` — batch runners.

With `Golem-Core` checked out next to this repository:

```bash
./gradlew chiseledBuild                                    # once, so every node has its dependencies
../Golem-Core/harness/harness up --nodes 26.1-neoforge,26.1-fabric
../Golem-Core/harness/harness run BarrelVsChest.java       # report in harness/out/runs/
../Golem-Core/harness/harness down
```

