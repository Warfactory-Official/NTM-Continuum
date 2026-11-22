#!/usr/bin/env python3
"""Sanity-check kb.py, scan_repo.py, check_mod.py and model_card.py against tiny built-in fixtures.

  python3 scripts/selftest.py

Run this once at the start of a study session (especially in an unfamiliar sandbox,
or after editing any script in this skill) instead of discovering a broken script
three steps into real work. Takes under a second, needs no network, and cleans up
after itself. Exits 0 if everything the skill depends on behaves as documented,
non-zero with a one-line reason per failure otherwise.
"""
import json, os, shutil, subprocess, sys, tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
PY = sys.executable or "python3"
failures = []

def check(label, cond, detail=""):
    status = "ok" if cond else "FAIL"
    print(f"[{status}] {label}" + (f" -- {detail}" if detail and not cond else ""))
    if not cond:
        failures.append(label)

def run(args, cwd):
    r = subprocess.run([PY] + args, cwd=cwd, capture_output=True, text=True, timeout=30)
    return r.returncode, r.stdout, r.stderr

def test_kb(tmp):
    kb = os.path.join(HERE, "kb.py")
    base = os.path.join(tmp, "kbdir")
    rc, out, err = run([kb, "--dir", base, "init"], tmp)
    check("kb.py init creates the base", rc == 0 and os.path.isdir(base), err)
    rc, out, err = run([kb, "--dir", base, "add", "--file", "patterns", "--ver", "1.21.1",
                         "--status", "seen", "--src", "Foo.java:1-5", "unit-test-topic: a lesson"], tmp)
    check("kb.py add exits cleanly", rc == 0, err)
    rc, out, err = run([kb, "--dir", base, "find", "unit-test-topic"], tmp)
    check("kb.py find retrieves the entry just added", "unit-test-topic" in out, out + err)
    # re-adding the same topic should update in place, not duplicate (the whole point of kb.py)
    run([kb, "--dir", base, "add", "--file", "patterns", "--ver", "1.21.1",
         "--status", "verified", "--src", "Foo.java:1-5", "unit-test-topic: an updated lesson"], tmp)
    with open(os.path.join(base, "patterns.md"), encoding="utf-8") as f:
        lines = [l for l in f if l.strip().startswith("[")]
    check("kb.py add dedupes by topic instead of appending a duplicate", len(lines) == 1, f"got {len(lines)} entries")
    for f in ("style", "ideas"):
        rc, out, err = run([kb, "--dir", base, "add", "--file", f, "--status", "seen", f"{f}-probe: ok"], tmp)
        check(f"kb.py accepts the '{f}' file", rc == 0, err)
    rc, out, err = run([kb, "--dir", base, "queue", "add", "testmod", "src/A.java"], tmp)
    check("kb.py queue add exits cleanly", rc == 0, err)
    rc, out, err = run([kb, "--dir", base, "queue", "next"], tmp)
    check("kb.py queue next lists the queued path", "src/A.java" in out, out + err)

def test_scan_repo(tmp):
    scan = os.path.join(HERE, "scan_repo.py")
    repo = os.path.join(tmp, "repo")
    os.makedirs(os.path.join(repo, "src/main/java/com/example"), exist_ok=True)
    with open(os.path.join(repo, "LICENSE"), "w", encoding="utf-8") as f:
        f.write("MIT License\nPermission is hereby granted, free of charge...\n"
                "Portions of this code are derived from an upstream project.\n")
    with open(os.path.join(repo, "src/main/java/com/example/Foo.java"), "w", encoding="utf-8") as f:
        f.write("// SPDX-License-Identifier: MIT\npackage com.example;\n"
                 "public class Foo extends KineticBlockEntity {}\n")
    rc, out, err = run([scan, repo, "--queue", "5"], tmp)
    check("scan_repo.py exits cleanly on a small repo", rc == 0, err)
    check("scan_repo.py detects the license kind", "-> MIT" in out, out)
    check("scan_repo.py flags 'portions ... derived from' language", "portions/derived/adapted" in out, out)
    check("scan_repo.py flags the per-file SPDX header", "SPDX headers" in out, out)
    check("scan_repo.py tags the kinetic-block-entity role", "kinetic-block-entity" in out, out)

def test_split_license(tmp):
    scan = os.path.join(HERE, "scan_repo.py")
    repo = os.path.join(tmp, "splitrepo")
    os.makedirs(repo, exist_ok=True)
    with open(os.path.join(repo, "LICENSE.md"), "w", encoding="utf-8") as f:
        f.write("Code is MIT. Textures and models: Creative Commons Attribution-NonCommercial-ShareAlike 4.0.\n")
    with open(os.path.join(repo, "LICENSE-MIT.md"), "w", encoding="utf-8") as f:
        f.write("MIT License\nPermission is hereby granted, free of charge...\n")
    rc, out, err = run([scan, repo], tmp)
    check("scan_repo.py reads every license file, not just the first", "CC" in out and "MIT" in out, out + err)
    check("scan_repo.py warns on NonCommercial terms", "NonCommercial" in out, out)

def write_png(path, w, h):
    import struct, zlib
    raw = b"".join(b"\x00" + b"\x80\x80\x80\xff" * w for _ in range(h))
    def chunk(tag, data):
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xffffffff)
    with open(path, "wb") as f:
        f.write(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
                + chunk(b"IDAT", zlib.compress(raw)) + chunk(b"IEND", b""))

def test_check_mod(tmp):
    cm = os.path.join(HERE, "check_mod.py")
    proj = os.path.join(tmp, "proj")
    assets = os.path.join(proj, "src/main/resources/assets/mymod")
    for d in ("blockstates", "models/block", "models/item", "textures/block", "lang"):
        os.makedirs(os.path.join(assets, d), exist_ok=True)
    def put(rel_path, obj):
        with open(os.path.join(assets, rel_path), "w", encoding="utf-8") as f:
            json.dump(obj, f)
    put("blockstates/good.json", {"variants": {"": {"model": "mymod:block/good"}}})
    put("models/block/good.json", {"parent": "minecraft:block/cube_all", "textures": {"all": "mymod:block/good"}})
    put("models/item/good.json", {"parent": "mymod:block/good"})
    put("lang/en_us.json", {"block.mymod.good": "Good"})
    write_png(os.path.join(assets, "textures/block/good.png"), 16, 16)
    rc, out, err = run([cm, proj], tmp)
    check("check_mod.py passes a clean project", rc == 0 and out.startswith("OK"), out + err)
    # now break it three ways
    put("blockstates/bad.json", {"variants": {"": {"model": "mymod:block/ghost"}}})
    put("models/block/arm.json", {"textures": {"0": "mymod:block/good"}, "elements": [
        {"from": [0, 0, 0], "to": [4, 4, 4], "rotation": {"angle": 30, "axis": "y", "origin": [2, 2, 2]},
         "faces": {"up": {"texture": "#0"}}}]})
    write_png(os.path.join(assets, "textures/block/orphan.png"), 16, 16)
    rc, out, err = run([cm, proj], tmp)
    check("check_mod.py exits 1 on errors", rc == 1, out + err)
    check("check_mod.py finds a blockstate pointing at a missing model", "ghost does not exist" in out, out)
    check("check_mod.py finds a rotation vanilla rejects", "rotation angle 30" in out, out)
    check("check_mod.py finds a missing lang key", "block.mymod.bad missing" in out, out)
    check("check_mod.py finds an unused texture", "orphan.png" in out, out)
    check("check_mod.py does not flag the good block", "good.json" not in out.replace("blockstates/bad.json", ""), out)
    jdir = os.path.join(proj, "src/main/java/com/x")
    os.makedirs(jdir, exist_ok=True)
    body = "\n".join(f"        x += {i};" for i in range(50))
    with open(os.path.join(jdir, "Big.java"), "w", encoding="utf-8") as f:
        f.write("package com.x;\npublic class Big {\n    int x;\n    public void longOne() {\n" + body + "\n    }\n}\n")
    rc, out, err = run([cm, proj], tmp)
    check("check_mod.py flags an over-long method", "longOne() is" in out, out)

def test_model_card(tmp):
    mc = os.path.join(HERE, "model_card.py")
    model = {"parent": "block/block", "textures": {"0": "mymod:block/x"},
              "elements": [{"from": [7, 2, 7], "to": [9, 16, 9],
                            "rotation": {"angle": 0, "axis": "y", "origin": [8, 12, 8]},
                            "faces": {"up": {"uv": [7, 7, 9, 9], "texture": "#0"}}}]}
    blockstate = {"variants": {"facing=north": {"model": "mymod:block/x"},
                                "facing=east": {"model": "mymod:block/x", "y": 90}}}
    mpath = os.path.join(tmp, "model.json")
    bpath = os.path.join(tmp, "blockstate.json")
    json.dump(model, open(mpath, "w"))
    json.dump(blockstate, open(bpath, "w"))
    rc, out, err = run([mc, mpath], tmp)
    check("model_card.py reads a model file", rc == 0 and "elements=1" in out, out + err)
    rc, out, err = run([mc, bpath], tmp)
    check("model_card.py reads a blockstate file", rc == 0 and "blockstate (variants)" in out, out + err)
    check("model_card.py flags facing-driven rotation in a blockstate",
          "differentiates by facing/axis" in out, out)

def main():
    tmp = tempfile.mkdtemp(prefix="create-deep-study-selftest-")
    try:
        test_kb(tmp)
        test_scan_repo(tmp)
        test_split_license(tmp)
        test_check_mod(tmp)
        test_model_card(tmp)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    print()
    if failures:
        print(f"{len(failures)} check(s) failed: {', '.join(failures)}")
        sys.exit(1)
    print("all checks passed -- scripts are safe to rely on in this environment")

if __name__ == "__main__":
    main()
