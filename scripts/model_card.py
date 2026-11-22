#!/usr/bin/env python3
"""Compact card for Minecraft block/item models (.json), blockstates (.json), or .obj
files, or a one-line table for a directory.

  model_card.py <file.json|file.obj>
  model_card.py <dir> [--top 25]
For a model: parent, textures, element count, bounding box in pixels (16 = 1 block),
size, per-element rotations, texel density, and an animation hint (rotation axis
candidates). For a blockstate (detected by "variants"/"multipart", no "elements"):
which properties it switches on, what x/y rotations it applies, and whether it
differentiates by facing/axis -- i.e. whether rotation is handled in the blockstate
rather than by shipping a separate model per facing. See references/animation.md
("Horizontal vs vertical axis: handle in the blockstate or renderer, not by
duplicating models") for why that distinction matters.
"""
import argparse, json, os, signal, sys
signal.signal(signal.SIGPIPE, signal.SIG_DFL)

def is_blockstate(j):
    return "elements" not in j and ("variants" in j or "multipart" in j)

def json_card(path, brief=False):
    """Load path once, then dispatch to the model or blockstate card builder."""
    try:
        with open(path, encoding="utf-8") as f:
            j = json.load(f)
    except Exception as e:
        return None, f"{os.path.basename(path)}: unreadable ({e})"
    name = os.path.basename(path)
    if is_blockstate(j):
        return bscard(j, name, brief)
    return mcard(j, name, brief)

def bscard(j, name, brief=False):
    variants, multipart = j.get("variants"), j.get("multipart")
    props, xs, ys, models = set(), set(), set(), []

    def eat_apply(entry):
        for it in (entry if isinstance(entry, list) else [entry]):
            if not isinstance(it, dict):
                continue
            if it.get("model"):
                models.append(it["model"])
            if "x" in it:
                xs.add(it["x"])
            if "y" in it:
                ys.add(it["y"])

    def walk_when(w):
        if not isinstance(w, dict):
            return
        for k, v in w.items():
            if k in ("OR", "AND") and isinstance(v, list):
                for sub in v:
                    walk_when(sub)
            else:
                props.add(k)

    if isinstance(variants, dict):
        kind, count = "variants", len(variants)
        for key, entry in variants.items():
            for part in key.split(","):
                if "=" in part:
                    props.add(part.strip().split("=")[0])
            eat_apply(entry)
    elif isinstance(multipart, list):
        kind, count = "multipart", len(multipart)
        for case in multipart:
            walk_when(case.get("when", {}))
            eat_apply(case.get("apply", {}))
    else:
        return None, f"{name}: has neither 'elements', 'variants', nor 'multipart' -- not a model or blockstate I recognize"

    axis_like = bool(props & {"facing", "axis", "rotation", "orientation"})
    card = {"name": name, "kind": kind, "count": count, "properties": sorted(props),
            "x_rot": sorted(xs, key=str), "y_rot": sorted(ys, key=str),
            "models": sorted(set(models))[:4], "axis_like": axis_like}
    if brief:
        return card, None
    line = (f"{name}: blockstate ({kind}) cases={count} | properties: {card['properties'] or ['none']} "
            f"| x-rot seen: {card['x_rot'] or ['0']} y-rot seen: {card['y_rot'] or ['0']} | models: {card['models']}")
    if axis_like:
        line += " | differentiates by facing/axis -> rotation is handled HERE, not by separate per-facing models"
    return card, line

def mcard(j, name, brief=False):
    els = j.get("elements", [])
    tex = j.get("textures", {})
    if not els:
        return None, f"{name}: parent={j.get('parent')} no elements textures={len(tex)}"
    mn = [min(e["from"][i] for e in els) for i in range(3)]
    mx = [max(e["to"][i] for e in els) for i in range(3)]
    size = [round(mx[i] - mn[i], 2) for i in range(3)]
    ctr = [round((mx[i] + mn[i]) / 2, 2) for i in range(3)]
    rots = []
    for e in els:
        r = e.get("rotation")
        if r:
            rots.append((r.get("axis"), r.get("angle"), r.get("origin")))
    faces = sum(len(e.get("faces", {})) for e in els)
    dens = []
    for e in els:
        f, t = e["from"], e["to"]
        dims = {"north": (t[0]-f[0], t[1]-f[1]), "south": (t[0]-f[0], t[1]-f[1]), "east": (t[2]-f[2], t[1]-f[1]), "west": (t[2]-f[2], t[1]-f[1]), "up": (t[0]-f[0], t[2]-f[2]), "down": (t[0]-f[0], t[2]-f[2])}
        for k, fc in e.get("faces", {}).items():
            uv = fc.get("uv")
            if uv and k in dims:
                w, h = abs(uv[2]-uv[0]), abs(uv[3]-uv[1])
                a = abs(dims[k][0]*dims[k][1])
                if a > 0.01:
                    dens.append((w*h) / a)
    avg = round(sum(dens)/len(dens), 2) if dens else None
    near = lambda i: abs(ctr[i] - 8) <= 0.6
    holds = [ax for ax, (i, j) in {"x": (1, 2), "y": (0, 2), "z": (0, 1)}.items() if near(i) and near(j)]
    longest = max(range(3), key=lambda i: size[i])
    others = [size[i] for i in range(3) if i != longest]
    if "xyz"[longest] in holds and size[longest] >= 1.5 * max(others):
        axes = ["xyz"[longest] + " (long axis)"]
    elif holds:
        axes = ["ambiguous:" + ",".join(holds)]
    else:
        axes = []
    hint = "static-body-like" if size[0] >= 14 and size[1] >= 14 and size[2] >= 14 else "possible moving part"
    card = {"name": name, "elements": len(els), "bbox_from": mn, "bbox_to": mx, "size_px": size, "center": ctr,
            "textures": sorted({v for v in tex.values() if isinstance(v, str)})[:4], "faces": faces,
            "rotated_elements": len(rots), "texel_density": avg, "axis_hint": axes or ["none"], "hint": hint,
            "parent": j.get("parent"), "groups": len(j.get("groups", []))}
    if brief:
        return card, None
    lines = [f"{name}: parent={card['parent']} elements={card['elements']} faces={faces} groups={card['groups']}",
             f"  bbox {mn} -> {mx} size {size} px center {ctr}",
             f"  textures {card['textures']} (slots={len(tex)}) texel density ~{avg} px per px (1.0 = 1:1)",
             f"  animation hint: centered on axis {card['axis_hint']} ({hint})"]
    if rots:
        uniq = sorted({(a, b, tuple(c) if c else None) for a, b, c in rots}, key=str)[:6]
        lines.append(f"  rotations ({len(rots)} elements): " + "; ".join(f"{a}{b:+} @ {c}" for a, b, c in uniq))
    return card, "\n".join(lines)

def ocard(path):
    v = vt = f = 0
    mn = [1e9]*3; mx = [-1e9]*3
    with open(path, encoding="utf-8", errors="ignore") as fh:
        for l in fh:
            if l.startswith("v "):
                p = [float(x) for x in l.split()[1:4]]
                v += 1
                for i in range(3):
                    mn[i] = min(mn[i], p[i]); mx[i] = max(mx[i], p[i])
            elif l.startswith("vt "): vt += 1
            elif l.startswith("f "): f += 1
    if not v:
        return f"{os.path.basename(path)}: empty obj"
    size = [round(mx[i]-mn[i], 3) for i in range(3)]
    return (f"{os.path.basename(path)}: OBJ verts={v} uvs={vt} faces={f} bbox size (model units) {size}; "
            f"if units are blocks, px = x16; check Minecraft OBJ loader scale and Flywheel partial-model support")

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("path"); ap.add_argument("--top", type=int, default=25)
    a = ap.parse_args()
    p = a.path
    if os.path.isdir(p):
        rows, bsrows = [], []
        for r, ds, fs in os.walk(p):
            for fn in fs:
                fp = os.path.join(r, fn)
                if fn.endswith(".json"):
                    c, msg = json_card(fp, brief=True)
                    if not c:
                        continue
                    if "elements" in c:
                        rows.append((c["elements"], os.path.relpath(fp, p), c))
                    else:
                        bsrows.append((os.path.relpath(fp, p), c))
                elif fn.endswith(".obj"):
                    rows.append((0, os.path.relpath(fp, p), None))
        rows.sort(key=lambda x: -x[0])
        print("elements | size_px | tex | axis | path")
        for n, rel, c in rows[: a.top]:
            if c:
                print(f"{n} | {c['size_px']} | {len(c['textures'])} | {','.join(c['axis_hint'])} | {rel}")
            else:
                print(f"obj | - | - | - | {rel}")
        print(f"({len(rows)} models; showing {min(len(rows), a.top)})")
        if bsrows:
            print(f"\n-- blockstates ({len(bsrows)}); kind | cases | properties | axis-handled-here | path")
            for rel, c in bsrows[: a.top]:
                print(f"{c['kind']} | {c['count']} | {c['properties'] or ['none']} | {'yes' if c['axis_like'] else 'no'} | {rel}")
    elif p.endswith(".obj"):
        print(ocard(p))
    else:
        c, msg = json_card(p)
        print(msg)

if __name__ == "__main__":
    main()
