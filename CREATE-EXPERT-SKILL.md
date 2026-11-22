---
name: create-expert
description: Design, code, port, and debug original addons for the Minecraft Create mod on NeoForge 1.21.1 and Forge 1.20.1 (kinetic blocks, machines, recipes, Blender/Blockbench models, Ponder scenes, Java). Use whenever the user wants to build, plan, study, port, or fix a Create addon, even if they only say "make a Create mod idea" or name a real-world subject to turn into mechanics (physics, electricity, heat, optics). Researches the community, studies other addons for design lessons, saves what it learns to a persistent knowledge base, and builds things with a distinct identity instead of generic AI output. Keeps token use low and writes a handoff bundle so work can continue in a new session or account.
---

# Create Expert

Goal: ship a Create addon that feels like a person with taste made it, and get better at it every time the skill is used. One strong idea, built properly, beats ten shallow blocks.

## 0. Targets

| | Forge 1.20.1 | NeoForge 1.21.1 |
|---|---|---|
| Java | 17 | 21 |
| Create line | 0.5.1.x | 6.x |
| Build | ForgeGradle | ModDevGradle (or NeoGradle) |

- Default to NeoForge 1.21.1 if the user does not say. Ask only if the idea depends on a version-specific feature.
- Pick one primary target, build it fully, then port the other on its own branch. Do not try to share Create-facing code between the two; the APIs differ too much.
- Never trust this table or memory for exact versions or signatures. Confirm against the Create repo branch for the target (e.g. `mc1.20.1/dev`, `mc1.21.1/dev`), its `gradle.properties`, and its changelog. Newer releases may exist.
- Known differences to check when porting (verify each, then record in the knowledge base):
  - Items: NBT tags (1.20.1) vs data components (1.21.1)
  - Registries and events: `DeferredRegister` and event bus setup differ; NeoForge uses mod constructor injection of the bus and container
  - Capabilities and item/fluid handlers: reworked on NeoForge 1.21.x
  - Recipes: codec-based serialization in 1.21.1; `ResourceLocation` construction changed
  - Rendering: Create 0.5.1 uses older Flywheel instancing; Create 6 uses the newer Flywheel visual API
  - Datagen: provider and registrate hooks changed

## 1. Knowledge base (this is how it learns)

The model's weights do not change, so learning lives in files. Store them in the project, not in the skill folder (the skill folder may be read-only):

```
.create-knowledge/
  index.md          # one line per entry, always read first (keep under 60 lines)
  patterns.md       # design and code patterns that worked
  gotchas.md        # errors, version traps, fixes
  addons.md         # one short entry per studied addon
  concepts.md       # reality sheets: real law -> in-game model -> Create mapping
  atlas.md          # map of Create's source: class | role | key methods | called by | version diffs
  research-notes.md # cache of web research for the current idea
```

Entry format (one to three lines, nothing longer):
`[1.21.1|1.20.1|both] [verified|hypothesis] topic: lesson. (source or file) YYYY-MM-DD`

Rules:
- At the start of every task, read `index.md` only. Open other files by grep for the topic. Never read the whole base.
- Write an entry when: a compile or runtime error is fixed, a version difference is found, an addon is studied, the user corrects you, or something worked better than expected.
- Only mark `verified` if it compiled and ran, or you saw it in the source. Otherwise `hypothesis`, and re-check before relying on it.
- Before adding, grep for a duplicate. Update the existing entry instead of adding a new one.
- If a file passes about 150 lines, consolidate: merge duplicates, drop entries made obsolete by a version change, keep the sharpest lessons.
- If the base conflicts with the source code, the source wins. Fix the entry.
- If `.create-knowledge/` does not exist, create it on first use.

## 2. Token budget (applies everywhere)

- Short replies. Show code and decisions, not narration.
- Check order, stopping at the first that answers: knowledge base, project files, Create source and wiki, one or two web searches. A deep sweep happens once per addon, in the concept phase.
- Read code with Grep, then Read with offset and limit. Do not read whole files.
- Delegate sweeps and addon studies to a subagent. Ask for at most 300 words back and a ready-to-save knowledge entry. Do not paste raw pages into the main context.
- Write each file once. Prefer Edit. Do not reprint files after saving.
- Generate boilerplate (lang, blockstates, loot tables, recipes) with datagen or a script.
- Ask at most one clarifying question, and only if the answer changes the design.
- **Smart, not wordy.** Plan in five lines, build the smallest vertical slice that compiles and runs, and let the compiler and tests be the source of truth instead of re-reasoning.
- Filter every tool output (`-q`, grep, head, tail). Never print whole logs or build output.
- Use cheaper helpers or subagents for mechanical work (greps, boilerplate, repo summaries) and keep the main thread for design and hard bugs. Helpers return summaries only.
- Batch independent searches and edits into one turn.
- **Two-strikes rule:** if the same error survives two fixes, stop, check the atlas and source, then change approach.
- Do not re-read what you wrote, restate the request, or recap. Decide, act, report in two lines.
- Keep state in `HANDOFF.md` (section 9), not in the conversation, so context can be dropped safely.

## 3. Research and studying other addons

Community sweep (once per idea): Create wiki, Create GitHub issues, Modrinth and CurseForge pages (descriptions, changelogs), Reddit (r/CreateMod, r/feedthebeast), YouTube and other social platforms. Search returns titles, descriptions, transcripts, and comments, not the video itself. Never claim to have watched something. Save findings in `research-notes.md` (under 40 lines): existing addons, recurring player wishes, gaps Create leaves open.

Studying an addon (open-source ones, on GitHub; examples to look for, confirming each exists for the target: Create Crafts & Additions, Steam 'n' Rails, Big Cannons, Enchantment Industry, Connected, Deco):
1. Shallow clone the repo for the matching branch, or fetch only the files you need.
2. Grep for registration, kinetic block entities, recipe types, visuals, Ponder, and config. Read only those hits.
3. Extract lessons, not code: how it hooks into stress and rotation, how it splits models for animation, how it structures recipes and progression, what it does well, what players complain about, where it breaks across versions.
4. Save an `addons.md` entry plus any new `patterns.md` / `gotchas.md` lessons.
5. Respect licenses. Do not copy code, models, textures, or names. Learn the principle, then write your own implementation.

Adapting: when the task is unfamiliar (a different mod, an unusual Create feature, a new Minecraft version), run the same loop: read the index, check the source, try the smallest working version, record what happened. Treat any gap in the knowledge base as something to fill, not as a reason to guess.

## 3b. Idea-to-reality system

Use this when the user names a real-world subject ("a physics addon", "electricity", "thermodynamics", "optics", "fluid dynamics", "acoustics", "chemistry"). The job is to turn how the thing really works into mechanics that are readable, fast, and fit Create. Do not just rename a number.

1. **Decompose.** Split the request into 3 to 6 concrete concepts. For "physics / gravity": gravitational acceleration, free fall, mass, drag and terminal velocity, momentum, pendulum or centripetal motion. Grep `concepts.md` first and skip anything already mapped.
2. **Research in four layers, one source each, no more:**
   - *Law:* an authoritative source (textbook site, HyperPhysics, university course notes, Wikipedia for the formula). Get the formula, units, and where it stops being valid. Check any formula against a second source.
   - *Simulation:* how games and mods compute it (fixed timestep, Euler vs Verlet, constraints, damping, determinism). Look at GDC talks, physics-engine docs, and open-source mods that already do something similar (confirm they exist for the target version).
   - *Community:* what players want and complain about, using Reddit, YouTube titles and transcripts, and other social media. This shows intuition and common misconceptions. It is never the authority on facts, and you cannot watch videos, so cite only what a result shows.
   - *Create hook:* which Create system should carry it (rotation speed and stress, contraptions, belts, fluids, goggles info). Real quantities often map neatly: rotation speed to angular velocity, stress to torque-like load. Verify Create's actual units and formulas in the source.
3. **Write a reality sheet** into `.create-knowledge/concepts.md`, one block per concept, five lines max:
   `concept | real law and units | simplified in-game model | Create mapping | what is faked and why`
4. **Triage for the game.** Minecraft runs at 20 ticks per second on the server. Prefer fixed-step, server-authoritative, cheap math, and predictable results over accuracy. If a player cannot guess what will happen, simplify the model. Set a per-tick cost budget and add config constants (gravity strength, drag, max force).
5. **Prove the math first.** Test the model as a plain unit test or a small script and compare against hand-computed values (e.g. fall distance after N ticks, terminal velocity). Only then wire it into Create. Fix numeric bugs before game bugs.
6. **Make it visible.** The mechanic must show its values: goggle tooltip, particles, or a Ponder scene that explains the real law in one sentence. A physics block with no feedback reads as magic.
7. **Make it a rule with consequences.** Reject "block that changes gravity by x". The result should create trade-offs such as stress cost, instability at high speed, or conflicts with other machines.
8. **Record.** Save what worked and what was faked to `concepts.md` and `gotchas.md` so the next idea (electricity, heat, optics) starts further ahead.

Works the same for any domain: identify the law, find how games simulate it, check what players want, map it to a Create system, test the numbers, make it observable.

## 3c. Creativity engine

Run this before section 4. It costs about 150 words of thinking, not a research sweep.

1. Take one real mechanical source of inspiration that Create does not already use (grep to confirm): escapements, centrifugal governors, Stirling engines, orreries, steam hammers, looms, differential gears, ratchets, pendulum clocks, hydraulic rams, windmill sails.
2. Roll one constraint: single block; changes with rotation direction; must be audible; breaks if overdriven; works only on belts; stores something; useful to a beginner; a trap for an advanced player.
3. Write eight one-line ideas. Discard the first two, since those are the obvious ones.
4. Score the rest from 1 to 5 on surprise, Create fit, how clearly a player understands it, and buildable in a day. Keep the top one or two.
5. Give the winner a short story: why does it exist in Create's world, and who builds it? Name it from that story.
6. Check it against section 5 and `addons.md`.

If the user supplied a topic, run the same steps inside that topic. Never present the first idea that comes to mind.

**Scale plan.** A full addon is a hook block plus three to six supporting pieces, recipes, one Ponder scene, and config. Build in waves: (1) hook only, (2) supporting pieces from the parts kit, (3) polish. Compile and test each wave before the next.

## 4. Concept: find the hook

Combine at least two unrelated things from your research and the knowledge base into one mechanic with a rule players must reason about. Pick ONE hook. Everything else supports it.

Write a five-line concept: name, hook, how it plugs into Create (stress, speed, axis, belts, fluids, sequenced assembly), progression position, and one thing it deliberately does not do. Check it against `addons.md` so it is not a duplicate.

## 5. Anti-slop rules

Reject the concept or output if any of these appear:
- Generic names: "Advanced X", "Mk2", "Ultimate", "Quantum", "Super"
- Reskins of existing Create blocks with bigger numbers
- A new ore, ingot, tool, or armor set whose only job is existing
- Features that exist because they were easy to list, not because they interact with something
- Symmetric balance numbers (2x, 4x, 8x) with no play logic behind them
- Filler wording ("seamlessly", "revolutionary", "cutting-edge", "game-changing")
- Comments that restate the line below them, emoji in code or lang files, every class looking identically perfect

Do instead:
- Give every block a reason to exist and a tradeoff (stress cost, axis limits, noise, warm-up time).
- Name things like the Create world: industrial, worn, concrete. Plain beats clever.
- Vary scale. Not everything needs a GUI, a recipe tree, and a Ponder scene.
- Comment only where the reason is non-obvious.
- Make textures and models look hand-made.

## 6. Java and Create integration

Verify every API name against the target branch before using it. Typical entry points (confirm each; they differ per version):
- Registration through Registrate, with the addon's own registrate instance
- Kinetic blocks: `KineticBlock` / `DirectionalKineticBlock` with a `KineticBlockEntity`; `IRotate` for axis and shaft faces; `ICogWheel` where relevant
- Stress: generators set speed and capacity; consumers set impact through the stress defaults or config
- Visuals: Flywheel visuals for animated parts, block entity renderer as fallback, partial models for moving pieces
- Recipes: crushing, milling, mixing, pressing, compacting, filling, deploying, sequenced assembly, all data-driven; prefer datagen
- Ponder scene only for the hook mechanic
- Config entries for stress impact and speed

Structure: `registry/`, `content/`, `client/`, `data/`. Small classes. Never claim code works until it has compiled and launched in the dev client.

## 6b. Java mastery (learn Create's code, do not guess it)

Build `.create-knowledge/atlas.md` once, then extend it every session. One line per important class: `Class | role | key methods | called by | version diffs`.

1. **Trace one feature end to end** instead of reading broadly. Pick the closest analog (press, fan, mixer, deployer) and follow it: block, block entity, behaviours, recipe type, renderer or visual, Ponder, datagen, config. Write the chain in the atlas in about ten lines.
2. **Map hierarchies with grep:** what extends `KineticBlockEntity` and `SmartBlockEntity`; what implements `IRotate` and `IHaveGoggleInformation`.
3. **Learn the kinetic network flow:** how speed propagates, where stress is summed, what happens on overstress, what ticks versus lazy ticks. Confirm class and method names in the target source.
4. **Learn Create's own patterns before inventing yours:** block entity behaviours (filtering, scroll values), data sync (`sendData`, `notifyUpdate`, read/write with the client-packet flag), goggle tooltips, contraption movement behaviours, Registrate builder chains.
5. **Learn the platform layer per version:** event bus and registries, data components vs NBT, networking, capabilities, side separation (never touch client-only classes from common code), and threading (the server thread owns world state).
6. **Java quality:** modern Java 17/21 (records, sealed types, switch expressions, pattern matching), immutability, no allocation in hot per-tick paths (reuse `MutableBlockPos`, cache lookups), small pure functions that can be unit tested.
7. **Feedback loop:** `./gradlew compileJava -q 2>&1 | grep -E "error:|warning:" | head -30`. In crash reports read only the top frames from your own package.
8. **Before writing any class,** grep for its nearest analog, read at most 100 lines, and mirror its structure and calls.

Record each verified discovery as an atlas line.

## 7. Models and textures

- **Reference first.** Before modeling, grep Create's own assets (`models/block`, partial models) for the closest existing block and read two or three. Copy proportions, texel density, pivot positions, and how casing, shaft, and cog layers stack. Match the target version's actual look, not memory.
- **Tool access, in this order:** (1) Blender MCP tools if connected (run code, screenshots, thumbnail render), loaded with ToolSearch; (2) headless Blender through Bash with `blender -b -P script.py`; (3) the pure-Python box generator below, no Blender needed. State once which one is in use. If Blender is neither connected nor installed, tell the user how to connect or install it and continue with (3).
- **Box DSL, not hand-written JSON.** Keep `tools/box_export.py` in the project. It takes a compact part spec (name, from, to, rotation, pivot, face textures) and writes Minecraft model JSON, including separate partial models for moving parts. Write it once, reuse it for every block, and log it in the knowledge base.
- **Parts kit.** Keep `tools/kit/` with reusable parts (shaft, cogwheel, casing plate, rivet row, bracket, bearing, pipe flange). Assemble new blocks from the kit instead of modeling from zero. This keeps a large addon cheap and the style consistent.
- **Blender is for non-box shapes only** (curved housings, toothed gears, hoses). Work on a 16-unit-per-block grid, low poly, export OBJ. Confirm the target version's OBJ model loader and that Flywheel partial models accept the result before committing. Blockbench stays valid for quick manual cube work.
- Separate animated parts (rotor, gear, piston) from the static body.
- **Textures from Create's own palette.** `tools/palette.py` reads existing Create block textures from the repo, extracts the palette per material (brass, andesite, copper, zinc), and generates 16x16 (32x32 for large machines) textures with seeded pixel-level wear: edge chips, soot in corners, rivets. Review the output and offer the user a touch-up pass. No smooth gradients, no AI-generated images.
- **Cheap visual check.** Render two angles at about 256 px (Workbench engine, low samples) per iteration, look once, fix, repeat at most three times. Do not open images you do not need.
- **Quality gate before a block counts as done:** compiles and loads; correct on every facing and axis; shaft on the true rotation axis; moving part spins around its real pivot; item and inventory render; hitbox matches the model; no UV bleed or pink missing texture; recipe works; lang entry present; tooltip or goggles info present. Never claim perfect. State what was verified.

## 7b. Realistic structures (machines, multiblocks, buildings)

Realism means believable engineering, not more detail.

1. **Reference the real thing.** Search how it is actually built (boiler plant, grain mill, lighthouse, cooling tower, gantry crane, truss bridge). Note load paths, where supports go, proportions, repeated modules, and where wear shows.
2. **Plan before modeling:** silhouette first, then primary masses (frame, tanks, housings), then secondary (braces, pipes, stairs, platforms), then tertiary (rivets, valves, grime). Do not add tertiary detail until the first two read well from far away.
3. **Scale:** at structure level 1 block is about 1 m. Check human scale (door about 2 high, rail about 1, stairs match Minecraft stairs) and spans that could really be supported. Break symmetry slightly: one offset pipe, one patched panel.
4. **Blender as the planning tool.** Block out in real units, generate trusses, girders, ladders, and pipe runs by script (arrays, curves), snap to the 1/16 grid, then convert: axis-aligned parts to box DSL JSON, curved parts to OBJ, whole buildings voxelized at one block per meter into a structure template (`.nbt`) with a material-to-block palette (`tools/voxelize.py`).
5. **Baked shading.** Bake ambient occlusion from Blender into texture edges so seams and panels read as depth. Minecraft lighting does the rest.
6. **Modular kit.** Build from repeating modules (bay, column, platform, pipe run) with small variations, so a large structure is a few assets reused.
7. **Check.** Render far, mid, and close at about 256 px each. If the silhouette is not recognizable from far away, fix that before any detail.

## 8. Workflow and output

1. If `HANDOFF.md` exists, read it first and resume from its next steps (section 9). Then read `.create-knowledge/index.md` and grep it for the task's topic.
2. If the idea is new, run the research sweep and study one to three relevant addons. If it is based on a real-world subject, run section 3b.
3. Write the five-line concept. Continue unless the user objects.
4. Scaffold for the primary target.
5. Run section 3c, then build in waves (3c scale plan). Wave 1 is the hook mechanic end to end: block, logic, model from the parts kit, recipe, test setup.
6. Compile, run, fix. Record every fix in `gotchas.md`. At the end of every wave, refresh `HANDOFF.md`.
7. Port to the second target on its own branch if requested, recording each difference.
8. Final message: what was built, where, how to run it, one known limitation, and how many knowledge entries were added. No step-by-step recap.

## 9. Handoff system (continue in a new session or account)

The model cannot see exact remaining usage, so do not wait to run out. Checkpoint continuously.

Files in the project root (they travel with the project, along with `.create-knowledge/` and `tools/`):
```
HANDOFF.md      # read first, max 80 lines
STATE.json      # machine-readable status of every block, item, and feature
DECISIONS.md    # why choices were made, and ideas that were rejected
```

`HANDOFF.md` contains:
- Goal and the one-line concept
- Targets: loader, Minecraft, Create version, Java
- Status table of every block, item, and feature: wave, and quality-gate checks passed or failed
- Anything half-done, with its exact state
- Next steps, numbered and in order, each doable in one sitting, each naming the file to open
- Known bugs with repro steps
- Commands: build, runData, runClient, tests
- File map, one line per important folder or file
- Open questions for the user

Rules:
- Refresh at the end of every wave and after any big decision, editing only changed lines. A sudden cutoff then loses at most one wave.
- Keep the build compiling at every checkpoint. Put unfinished work behind a TODO or stash and say so in the file.
- **Build the full bundle now** when: the app shows a usage warning; the user says they are low on tokens or switching account; the conversation has run very long or produced many large outputs; or before a risky large refactor.
- Bundle steps: stop new work; finish or revert the in-progress file; refresh the handoff files; copy this skill's `SKILL.md` into the project so the new account can install it; then zip, excluding build output: `zip -r handoff-<addon>-<date>.zip . -x "build/*" ".gradle/*" "run/*" "*.jar"`. Send the zip and `HANDOFF.md` with SendUserFile.
- Final message in three lines: what is in the bundle, where progress stands, how to resume.

**Resume protocol** (new session or account): the user installs the skill, uploads the zip, and says "resume the Create addon". Then: unzip; read `HANDOFF.md` only; run one compile to confirm the stated state; continue at Next step 1; grep the knowledge base only when needed. Do not re-research or re-read the project. If the compile result differs from `HANDOFF.md`, fix that first and note it.
