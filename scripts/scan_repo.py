#!/usr/bin/env python3
"""Compact inventory of a mod source tree, zip, or jar. Prints under ~70 lines.

  scan_repo.py <dir|zip|jar> [--queue N] [--tsv]
--queue N : append a prioritized file queue (top N). --tsv prints the queue as
            'addon<TAB>path<TAB>why' lines ready for `kb.py queue add`.
Jars contain compiled classes: only class names and assets are listed.
"""
import argparse, collections, json, os, re, signal, sys, zipfile
signal.signal(signal.SIGPIPE, signal.SIG_DFL)

SKIP = {".git", "build", ".gradle", "node_modules", "run", ".idea", "out"}
ROLE_PATTERNS = [  # (role, regex over source text, priority)
    ("kinetic-block-entity", r"extends\s+\w*Kinetic\w*(BlockEntity|TileEntity)\b", 1),
    ("smart-block-entity", r"extends\s+(SmartBlockEntity|SmartTileEntity)", 2),
    ("visual/renderer", r"(KineticBlockEntityRenderer|KineticBlockEntityVisual|SafeBlockEntityRenderer|BlockEntityRenderer<|BlockEntityInstance|\bVisual\b)", 1),
    ("partial-models", r"PartialModel\s*\(|AllPartialModels|PartialModel\b", 2),
    ("ponder", r"(SceneBuilder|PonderScene|PonderRegistrationHelper|PonderPlugin|CreateSceneBuilder)", 2),
    ("recipe-type", r"(RecipeSerializer|ProcessingRecipe|StandardProcessingRecipe|RecipeType<)", 3),
    ("kinetic-block", r"extends\s+\w*KineticBlock\b", 2),
    ("registrate", r"(Registrate|CreateRegistrate|\.transform\(BlockStressDefaults)", 3),
    ("network", r"(CustomPayload|SimplePacketBase|SimpleChannel|PacketDistributor)", 4),
    ("mixin", r"@Mixin\b", 4),
]
LICENSES = [("LGPL", r"GNU LESSER GENERAL PUBLIC"), ("GPL", r"GNU GENERAL PUBLIC"), ("MIT", r"\bMIT License\b|Permission is hereby granted, free of charge"),
            ("Apache-2.0", r"Apache License"), ("MPL", r"Mozilla Public License"), ("ARR", r"All Rights Reserved"), ("CC", r"Creative Commons")]

class Src:
    def __init__(self, p):
        self.p = p
        self.z = zipfile.ZipFile(p) if os.path.isfile(p) and p.lower().endswith((".zip", ".jar")) else None
        self.is_jar = bool(self.z) and p.lower().endswith(".jar")
        if self.z:
            self.names = [n for n in self.z.namelist() if not n.endswith("/")]
        else:
            self.names = []
            for r, ds, fs in os.walk(p):
                ds[:] = [d for d in ds if d not in SKIP]
                for f in fs:
                    self.names.append(os.path.relpath(os.path.join(r, f), p).replace(os.sep, "/"))
    def read(self, n, limit=6000):
        try:
            if self.z:
                with self.z.open(n) as f:
                    return f.read(limit).decode("utf-8", "ignore")
            with open(os.path.join(self.p, n), "rb") as f:
                return f.read(limit).decode("utf-8", "ignore")
        except Exception:
            return ""

def grab(text, key):
    m = re.search(rf"^\s*{re.escape(key)}\s*=\s*(.+)$", text, re.M)
    return m.group(1).strip().strip('"') if m else None

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("path"); ap.add_argument("--queue", type=int, default=0); ap.add_argument("--tsv", action="store_true")
    a = ap.parse_args()
    if not os.path.exists(a.path):
        sys.exit("not found: " + a.path)
    s = Src(a.path)
    names = s.names
    addon = os.path.basename(os.path.normpath(a.path)).rsplit(".", 1)[0]
    out = [f"== {addon}: {len(names)} files" + (" (jar: compiled classes)" if s.is_jar else "")]

    # license
    lic = [n for n in names if re.match(r"(^|.*/)(LICEN[CS]E|COPYING)[^/]*$", n, re.I) and n.count("/") <= 1]
    kind, kinds, noncommercial = "unknown", [], False
    if lic:
        # Read every license file (split-licensed addons ship LICENSE-MIT + LICENSE-CCBYNCSA, etc.),
        # not just the first: code and assets often carry different terms.
        for n in lic[:4]:
            t = s.read(n, 4000)
            found = [k for k, rx in LICENSES if re.search(rx, t, re.I)]
            if "LGPL" in found and "GPL" in found:
                found.remove("GPL")  # the LGPL text quotes the GPL
            kinds += [k for k in found if k not in kinds]
            noncommercial = noncommercial or bool(re.search(r"Non-?Commercial|BY-NC|\bNC-SA\b", t, re.I))
        kind = kinds[0] if kinds else "unknown"
        out.append(f"license file(s): {', '.join(lic[:4])} -> {' + '.join(kinds) or 'unknown'} "
                   "(confirm by reading; look for 'portions from' notes in README)")
        if noncommercial:
            out.append("license WARNING: NonCommercial terms found -- textures/models/code under them cannot be reused "
                       "in a monetized mod, and share-alike may apply. Study the technique, paint your own pixels.")
        if len(kinds) > 1:
            out.append("license note: several license kinds present -- code and assets may differ; check which paths each covers")
    else:
        out.append("license file: NONE found (treat as all rights reserved; do not copy anything)")

    # Automate the "look for portions from" step instead of relying on a human to remember it.
    readme = [n for n in names if re.match(r"(^|.*/)README[^/]*$", n, re.I) and n.count("/") <= 1]
    PORTION_RX = (r"(portions?\s+(?:of\s+)?(?:this\s+|the\s+)?(?:code\s+)?(?:are|is)?\s*(?:from|derived from)"
                  r"|derived from|adapted from|forked from|based on code from|incorporates code from"
                  r"|some (?:files|code) (?:are|is|was) (?:from|taken from))")
    hits = []
    for n in (lic[:1] + readme[:1]):
        t = s.read(n, 20000)
        for m in re.finditer(PORTION_RX, t, re.I):
            snippet = re.sub(r"\s+", " ", t[max(0, m.start() - 20): m.start() + 90]).strip()
            hits.append(f"{n}: \u2026{snippet}\u2026")
    if hits:
        out.append("license sub-notice: 'portions/derived/adapted from' language found -- read before reusing anything:")
        out.extend("  " + h for h in hits[:3])

    # Per-file SPDX headers can carry different terms than the repo-level LICENSE (common when an
    # addon bundles a file or two from elsewhere). Cheap to sample; catches what scanning only
    # the root LICENSE would miss.
    java_sample = [n for n in names if n.endswith(".java")][:60]
    spdx = collections.Counter()
    for n in java_sample:
        m = re.search(r"SPDX-License-Identifier:\s*([A-Za-z0-9.+-]+)", s.read(n, 400))
        if m:
            spdx[m.group(1)] += 1
    if spdx:
        mismatch = len(spdx) > 1 or (bool(kinds) and not any(k in next(iter(spdx)) for k in kinds))
        note = " -- differs from the repo license above; those specific files carry their own terms" if mismatch else ""
        out.append(f"per-file SPDX headers (sampled {len(java_sample)} java files): " +
                    ", ".join(f"{k}={v}" for k, v in spdx.most_common(5)) + note)

    # metadata
    meta = [n for n in names if n.endswith(("neoforge.mods.toml", "META-INF/mods.toml", "fabric.mod.json", "gradle.properties"))]
    for n in meta[:4]:
        t = s.read(n, 5000)
        if n.endswith(".json"):
            try:
                j = json.loads(t); out.append(f"{n}: id={j.get('id')} version={j.get('version')} depends={list((j.get('depends') or {}).keys())[:6]}")
            except Exception:
                out.append(f"{n}: (unparsed)")
        elif n.endswith("gradle.properties"):
            keys = [k for k in ("mod_id", "mod_version", "minecraft_version", "create_version", "flywheel_version", "neo_version", "forge_version", "loader_version") if grab(t, k)]
            out.append(f"{n}: " + ", ".join(f"{k}={grab(t,k)}" for k in keys))
        else:
            deps = re.findall(r'modId\s*=\s*"([^"]+)"', t)
            out.append(f"{n}: modId={grab(t,'modId')} version={grab(t,'version')} deps={sorted(set(deps))[:8]}")

    # java / classes
    java = [n for n in names if n.endswith(".java")]
    classes = [n for n in names if n.endswith(".class")]
    roles = collections.defaultdict(list)
    if java:
        for n in java:
            t = s.read(n, 5000)
            for role, rx, pr in ROLE_PATTERNS:
                if re.search(rx, t):
                    roles[role].append((pr, n))
        out.append(f"java: {len(java)} files; roles: " + ", ".join(f"{r}={len(v)}" for r, v in sorted(roles.items(), key=lambda kv: -len(kv[1]))))
    elif classes:
        for n in classes:
            base = n.rsplit("/", 1)[-1]
            for role, kw, pr in [("visual/renderer", ("Renderer", "Visual"), 1), ("kinetic-block-entity", ("KineticBlockEntity", "BlockEntity"), 2), ("ponder", ("Ponder", "Scene"), 2), ("recipe-type", ("Recipe",), 3)]:
                if any(k in base for k in kw):
                    roles[role].append((pr, n)); break
        out.append(f"classes: {len(classes)}; by name: " + ", ".join(f"{r}={len(v)}" for r, v in roles.items()))

    # assets
    def count(rx): return sum(1 for n in names if re.search(rx, n))
    models = [n for n in names if re.search(r"assets/[^/]+/models/.*\.(json|obj)$", n)]
    out.append("assets: models=%d (block=%d item=%d partial/other=%d) blockstates=%d textures=%d lang=%d | data: recipes=%d ponder-nbt=%d" % (
        len(models), count(r"/models/block/"), count(r"/models/item/"), len(models) - count(r"/models/block/") - count(r"/models/item/"),
        count(r"/blockstates/"), count(r"/textures/.*\.png$"), count(r"/lang/.*\.json$"), count(r"/recipes?/.*\.json$"), count(r"ponder/.*\.nbt$")))

    # recipe types
    rt = collections.Counter()
    for n in [n for n in names if re.search(r"/recipes?/.*\.json$", n)][:300]:
        m = re.search(r'"type"\s*:\s*"([^"]+)"', s.read(n, 400))
        if m: rt[m.group(1)] += 1
    if rt:
        out.append("recipe types: " + ", ".join(f"{k}={v}" for k, v in rt.most_common(6)))

    # queue
    merged = {}
    for role, items in roles.items():
        for pr, n in items:
            best, rs = merged.get(n, (9, []))
            merged[n] = (min(best, pr), rs + [role])
    queue = [(pr, "+".join(rs), n) for n, (pr, rs) in merged.items()]
    big = []
    for n in models:
        if n.endswith(".json"):
            t = s.read(n, 20000)
            big.append((-t.count('"from"'), n))
    for k, n in sorted(big)[:12]:
        queue.append((3, "model(" + str(-k) + " elements)", n))
    for n in [n for n in names if re.search(r"ponder/.*\.nbt$", n)][:5]:
        queue.append((3, "ponder-schematic", n))
    queue.sort(key=lambda x: (x[0], x[2]))
    if a.queue:
        out.append(f"-- study queue (top {a.queue}); priority, role, path")
        for pr, role, n in queue[: a.queue]:
            out.append(f"{pr}\t{role}\t{n}")
    text = "\n".join(out)
    if a.tsv:
        print("\n".join(f"{addon}\t{n}\t{role}" for pr, role, n in queue[: a.queue or 40]))
    else:
        print(text)

if __name__ == "__main__":
    main()
