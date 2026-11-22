#!/usr/bin/env python3
"""Find what is wrong in a mod project before launching the game. Prints problems only.

  check_mod.py <project_dir> [--max-method 40] [--max-file 400] [--max-depth 4] [--all]

Catches mistakes that compile fine and then show up as purple cubes, missing names or
spaghetti: blockstates/models pointing at files that do not exist, textures missing or
the wrong shape, mixed texel density, block entries with no lang key or item model, element
rotations vanilla rejects, assets nothing uses, and Java that has grown too complex
(long methods, deep nesting, huge files). Heuristic, text-based, no network, no Java parser.

Exit code 1 if any ERROR, else 0. Run it after every file or small batch you write, fix
everything it lists, run it again. Targets Minecraft 1.21.1 and earlier asset layout
(models/item/<name>.json); 1.21.4+ uses assets/<ns>/items/ definitions instead.
"""
import argparse, collections, json, os, re, struct, sys

SKIP_DIRS = {".git", "build", ".gradle", "node_modules", "run", "out", ".idea", ".create-study",
             ".codebase-mcp", ".sense", ".tessera", "__pycache__"}
FOREIGN_NS = {"minecraft", "forge", "neoforge", "c", "create", "flywheel", "ponder", "fabric"}
ROTATIONS = {-45, -22.5, 0, 22.5, 45}

problems = []  # (level, where, message)

def add(level, where, msg):
    problems.append((level, where, msg))

def load_json(path):
    try:
        with open(path, encoding="utf-8") as f:
            return json.load(f)
    except Exception as e:
        add("E", rel(path), f"unreadable JSON ({e})")
        return None

def rel(path):
    return os.path.relpath(path, ROOT)

def split_ref(ref):
    return tuple(ref.split(":", 1)) if ":" in ref else ("minecraft", ref)

def png_size(path):
    try:
        with open(path, "rb") as f:
            head = f.read(24)
        if head[:8] != b"\x89PNG\r\n\x1a\n" or head[12:16] != b"IHDR":
            return None
        return struct.unpack(">II", head[16:24])
    except OSError:
        return None

def collect(obj, keys, out):
    """Collect string values stored under any of `keys`, anywhere in a JSON tree."""
    if isinstance(obj, dict):
        for k, v in obj.items():
            if k in keys and isinstance(v, str):
                out.append(v)
            collect(v, keys, out)
    elif isinstance(obj, list):
        for v in obj:
            collect(v, keys, out)

def discover():
    """Map namespace -> list of asset roots; list every java file."""
    assets, java = collections.defaultdict(list), []
    for root, dirs, files in os.walk(ROOT):
        dirs[:] = [d for d in dirs if d not in SKIP_DIRS]
        if os.path.basename(root) == "assets":
            for d in dirs:
                assets[d].append(os.path.join(root, d))
        java += [os.path.join(root, f) for f in files if f.endswith(".java")]
    return assets, java

def exists(ns, kind, path, ext):
    return any(os.path.exists(os.path.join(d, kind, path + ext)) for d in ASSETS.get(ns, []))

def files_in(ns, sub, ext):
    out = []
    for d in ASSETS.get(ns, []):
        base = os.path.join(d, sub)
        for r, _, fs in os.walk(base):
            out += [os.path.join(r, f) for f in fs if f.endswith(ext)]
    return out

def check_namespace(ns, java_text):
    model_refs, texture_refs = set(), set()
    # blockstates -> models, lang, item models
    lang = {}
    for lp in files_in(ns, "lang", "en_us.json"):
        lang.update(load_json(lp) or {})
    for bp in files_in(ns, "blockstates", ".json"):
        name = os.path.basename(bp)[:-5]
        j = load_json(bp)
        if j is None:
            continue
        refs = []
        collect(j, {"model"}, refs)
        if not refs:
            add("E", rel(bp), "no 'model' anywhere in this blockstate")
        for r in refs:
            rns, rpath = split_ref(r)
            model_refs.add((rns, rpath))
            if rns == ns and not exists(ns, "models", rpath, ".json"):
                add("E", rel(bp), f"model {r} does not exist")
        if not lang:
            add("W", rel(bp), "no en_us.json found for this namespace (block will show its raw key)")
        elif f"block.{ns}.{name}" not in lang:
            add("W", rel(bp), f"lang key block.{ns}.{name} missing")
        if not exists(ns, "models/item", name, ".json"):
            add("W", rel(bp), f"no models/item/{name}.json (held item will be missing/purple)")
    # models -> parents, textures, rotations, bounds
    for mp in files_in(ns, "models", ".json"):
        j = load_json(mp)
        if not isinstance(j, dict):
            continue
        parent = j.get("parent")
        if isinstance(parent, str):
            pns, ppath = split_ref(parent)
            model_refs.add((pns, ppath))
            if pns == ns and not exists(ns, "models", ppath, ".json"):
                add("E", rel(mp), f"parent {parent} does not exist")
        texs = j.get("textures", {}) if isinstance(j.get("textures"), dict) else {}
        for k, v in texs.items():
            if not isinstance(v, str) or v.startswith("#"):
                continue
            tns, tpath = split_ref(v)
            texture_refs.add((tns, tpath))
            if tns == ns and not exists(ns, "textures", tpath, ".png"):
                add("E", rel(mp), f"texture {v} ({k}) has no PNG")
        els = j.get("elements", [])
        used = set()
        for i, el in enumerate(els if isinstance(els, list) else []):
            for face in (el.get("faces") or {}).values():
                t = face.get("texture") if isinstance(face, dict) else None
                if isinstance(t, str) and t.startswith("#"):
                    used.add(t[1:])
            rot = el.get("rotation") or {}
            if "angle" in rot and rot["angle"] not in ROTATIONS:
                add("E", rel(mp), f"element {i}: rotation angle {rot['angle']} not allowed (only -45, -22.5, 0, 22.5, 45)")
            for key in ("from", "to"):
                pts = el.get(key) or []
                if any(isinstance(c, (int, float)) and not -16 <= c <= 32 for c in pts):
                    add("W", rel(mp), f"element {i}: '{key}' {pts} outside -16..32 (vanilla limit)")
        missing = [u for u in used if u not in texs]
        if missing and not parent:
            add("W", rel(mp), f"faces use #{', #'.join(sorted(missing))} but no such texture is defined and there is no parent")
        is_block = os.sep + "block" + os.sep in mp
        if els and is_block and "particle" not in texs and (not parent or parent in ("block/block", "minecraft:block/block")):
            add("W", rel(mp), "no 'particle' texture (breaking particles will be the missing-texture pink)")
    # textures: validity, size shape, density, orphans
    sizes = collections.Counter()
    for tp in files_in(ns, "textures", ".png"):
        sz = png_size(tp)
        if sz is None:
            add("E", rel(tp), "not a valid PNG")
            continue
        w, h = sz
        pot = lambda n: n > 0 and n & (n - 1) == 0
        if not (pot(w) and pot(h)) and h % w != 0:
            add("W", rel(tp), f"{w}x{h} is not power-of-two and not an animation strip (mipmaps and UVs get awkward)")
        if os.sep + "block" + os.sep in tp:
            sizes[f"{w}x{h}"] += 1
        stem = os.path.basename(tp)[:-4]
        tpath = os.path.relpath(tp, [d for d in ASSETS[ns] if tp.startswith(d)][0]).split(os.sep, 1)[1][:-4].replace(os.sep, "/")
        if (ns, tpath) not in texture_refs and stem not in java_text:
            add("W", rel(tp), "no model or Java mentions this texture (unused?)")
    if len(sizes) > 1:
        add("I", f"{ns} textures/block", "mixed sizes " + ", ".join(f"{k} x{v}" for k, v in sizes.most_common())
            + " -- one texel density per mod keeps the look consistent")
    for mp in files_in(ns, "models/block", ".json"):
        p = os.path.relpath(mp, [d for d in ASSETS[ns] if mp.startswith(d)][0]).split(os.sep, 1)[1][:-5].replace(os.sep, "/")
        if (ns, p) not in model_refs and os.path.basename(mp)[:-5] not in java_text:
            add("W", rel(mp), "no blockstate, parent or Java mentions this model (unused partial?)")

def strip_java(line, in_block):
    """Remove comments and string/char literals so braces inside them do not count."""
    out, i = [], 0
    while i < len(line):
        if in_block:
            j = line.find("*/", i)
            if j < 0:
                return "".join(out), True
            i, in_block = j + 2, False
        elif line.startswith("/*", i):
            in_block = True
            i += 2
        elif line.startswith("//", i):
            break
        elif line[i] in "\"'":
            q, i = line[i], i + 1
            while i < len(line) and line[i] != q:
                i += 2 if line[i] == "\\" else 1
            i += 1
        else:
            out.append(line[i])
            i += 1
    return "".join(out), in_block

METHOD_RX = re.compile(r"^\s*(?:@\w+\s+)*(?:(?:public|protected|private|static|final|abstract|synchronized|default)\s+)*"
                       r"[\w<>\[\],.? ]+\s+(\w+)\s*\([^;]*\)\s*(?:throws [\w., ]+)?\{\s*$")

def check_java(paths, max_method, max_file, max_depth):
    for jp in paths:
        if os.sep + "generated" + os.sep in jp:
            continue
        try:
            lines = open(jp, encoding="utf-8", errors="replace").read().splitlines()
        except OSError:
            continue
        if len(lines) > max_file:
            add("W", rel(jp), f"{len(lines)} lines (> {max_file}); split by responsibility")
        depth, in_block, stack = 0, False, []  # stack: (name, start_line, depth_at_open, max_depth_seen)
        for n, raw in enumerate(lines, 1):
            code, in_block = strip_java(raw, in_block)
            if METHOD_RX.match(code) and depth >= 1 and not stack:
                m = METHOD_RX.match(code)
                if m.group(1) not in ("if", "for", "while", "switch", "catch", "synchronized"):
                    stack.append([m.group(1), n, depth, 0])
            for ch in code:
                if ch == "{":
                    depth += 1
                    if stack:
                        stack[-1][3] = max(stack[-1][3], depth - stack[-1][2] - 1)
                elif ch == "}":
                    depth -= 1
                    if stack and depth == stack[-1][2]:
                        name, start, _, nest = stack.pop()
                        length = n - start + 1
                        if length > max_method:
                            add("W", f"{rel(jp)}:{start}", f"method {name}() is {length} lines (> {max_method}); split or simplify")
                        if nest > max_depth:
                            add("W", f"{rel(jp)}:{start}", f"method {name}() nests {nest} levels (> {max_depth}); use early returns or helpers")

def main():
    global ROOT, ASSETS
    ap = argparse.ArgumentParser()
    ap.add_argument("project")
    ap.add_argument("--max-method", type=int, default=40)
    ap.add_argument("--max-file", type=int, default=400)
    ap.add_argument("--max-depth", type=int, default=4)
    ap.add_argument("--all", action="store_true", help="print every problem instead of the first 40")
    a = ap.parse_args()
    ROOT = os.path.abspath(a.project)
    ASSETS, java = discover()
    java_text = ""
    for jp in java:
        try:
            java_text += open(jp, encoding="utf-8", errors="replace").read() + "\n"
        except OSError:
            pass
    own = [ns for ns in ASSETS if ns not in FOREIGN_NS]
    if not own and not java:
        print(f"nothing to check under {ROOT} (no assets/<namespace>/ and no .java files)")
        return
    for ns in own:
        check_namespace(ns, java_text)
    check_java(java, a.max_method, a.max_file, a.max_depth)
    order = {"E": 0, "W": 1, "I": 2}
    problems.sort(key=lambda p: (order[p[0]], p[1]))
    counts = collections.Counter(p[0] for p in problems)
    if not problems:
        print(f"OK: {len(own)} namespace(s), {len(java)} java file(s), nothing to fix")
        return
    print(f"{counts['E']} error(s), {counts['W']} warning(s), {counts['I']} note(s)")
    shown = problems if a.all else problems[:40]
    for level, where, msg in shown:
        print(f"{level} {where}: {msg}")
    if len(problems) > len(shown):
        print(f"(+{len(problems) - len(shown)} more; --all to list)")
    sys.exit(1 if counts["E"] else 0)

if __name__ == "__main__":
    main()
