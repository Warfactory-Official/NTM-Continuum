#!/usr/bin/env python3
"""Tiny knowledge-base helper for .create-study/. Keeps reads cheap and entries deduped.

  kb.py init
  kb.py add --file patterns --ver 1.21.1 --status seen --src "Foo.java:10-40" "topic: lesson"
  kb.py find <term> [<term> ...] [-n 15]
  kb.py index
  kb.py stats
  kb.py compact [--cap 150]
  kb.py queue add <addon> <path> [--why TEXT]
  kb.py queue next [-n 3]
  kb.py queue done <path> [--note TEXT]
Files: addons patterns gotchas models sources adaptations style ideas.
  style = this mod's own conventions (palette hexes, texel density, naming) so every new asset matches.
  ideas = concepts generated, chosen, rejected (with why) so sessions do not repeat themselves.
Use --dir to change the base directory (default .create-study).
"""
import argparse, datetime, os, re, signal, sys
signal.signal(signal.SIGPIPE, signal.SIG_DFL)

FILES = ["addons", "patterns", "gotchas", "models", "sources", "adaptations", "style", "ideas"]
STATUS = {"seen", "inferred", "unverified", "verified", "hypothesis"}

def today():
    return datetime.date.today().isoformat()

def path(d, name):
    return os.path.join(d, name)

def read_lines(p):
    if not os.path.exists(p):
        return []
    with open(p, encoding="utf-8") as f:
        return [l.rstrip("\n") for l in f]

def write_lines(p, lines):
    with open(p, "w", encoding="utf-8") as f:
        f.write("\n".join(lines) + ("\n" if lines else ""))

def topic_key(line):
    m = re.match(r"^\[[^\]]*\]\s*\[[^\]]*\]\s*([^:]+):", line)
    return m.group(1).strip().lower() if m else None

def cmd_init(a):
    os.makedirs(a.dir, exist_ok=True)
    for n in FILES:
        p = path(a.dir, n + ".md")
        if not os.path.exists(p):
            write_lines(p, [f"# {n}"])
    if not os.path.exists(path(a.dir, "queue.tsv")):
        write_lines(path(a.dir, "queue.tsv"), [])
    cmd_index(a)
    print(f"ready: {a.dir}")

def cmd_add(a):
    if a.file not in FILES:
        sys.exit(f"--file must be one of {FILES}")
    if a.status not in STATUS:
        sys.exit(f"--status must be one of {sorted(STATUS)}")
    text = a.text.strip()
    if ":" not in text:
        sys.exit("entry must look like 'topic: lesson'")
    os.makedirs(a.dir, exist_ok=True)
    src = f" ({a.src})" if a.src else ""
    line = f"[{a.ver}] [{a.status}] {text.rstrip('.')}.{src} {today()}"
    p = path(a.dir, a.file + ".md")
    lines = read_lines(p) or [f"# {a.file}"]
    key = topic_key(line)
    for i, l in enumerate(lines):
        if topic_key(l) == key:
            lines[i] = line
            write_lines(p, lines)
            print("updated:", key)
            cmd_index(a, quiet=True)
            return
    lines.append(line)
    write_lines(p, lines)
    print("added:", key)
    cmd_index(a, quiet=True)

def cmd_find(a):
    terms = [t.lower() for t in a.terms]
    out = []
    for n in FILES:
        for l in read_lines(path(a.dir, n + ".md")):
            if l.startswith("#") or not l.strip():
                continue
            low = l.lower()
            score = sum(t in low for t in terms)
            if score:
                out.append((score, n, l))
    out.sort(key=lambda x: -x[0])
    for _, n, l in out[: a.n]:
        print(f"{n}: {l}")
    if not out:
        print("(no matches)")

def cmd_stats(a):
    for n in FILES:
        c = sum(1 for l in read_lines(path(a.dir, n + ".md")) if l.startswith("["))
        print(f"{n}: {c} entries")
    q = read_lines(path(a.dir, "queue.tsv"))
    print(f"queue: {sum(1 for l in q if l.split(chr(9))[0]=='todo')} todo, {sum(1 for l in q if l.split(chr(9))[0]=='done')} done")

def cmd_index(a, quiet=False):
    os.makedirs(a.dir, exist_ok=True)
    lines = ["# index (read this first; use kb.py find for details)", f"updated {today()}", ""]
    for n in FILES:
        entries = [l for l in read_lines(path(a.dir, n + ".md")) if l.startswith("[")]
        lines.append(f"## {n} ({len(entries)})")
        for l in entries[-6:]:
            k = topic_key(l)
            if k:
                lines.append(f"- {k}")
    write_lines(path(a.dir, "index.md"), lines[:60])
    if not quiet:
        print("index rebuilt")

def cmd_compact(a):
    for n in FILES:
        p = path(a.dir, n + ".md")
        lines = read_lines(p)
        seen, out, dropped = {}, [], 0
        for l in lines:
            k = topic_key(l)
            if k is None:
                out.append(l)
            elif k in seen:
                out[seen[k]] = l
                dropped += 1
            else:
                seen[k] = len(out)
                out.append(l)
        write_lines(p, out)
        flag = " OVER CAP: merge or drop obsolete entries by hand" if len(out) > a.cap else ""
        print(f"{n}: {len(out)} lines, {dropped} duplicates merged{flag}")
    cmd_index(a, quiet=True)

def qpath(a):
    return path(a.dir, "queue.tsv")

def cmd_queue(a):
    rows = [l.split("\t") for l in read_lines(qpath(a)) if l.strip()]
    if a.qcmd == "add":
        existing = {r[2] for r in rows if len(r) > 2}
        if a.path in existing:
            print("already queued"); return
        os.makedirs(a.dir, exist_ok=True)
        rows.append(["todo", a.addon, a.path, a.why or ""])
        write_lines(qpath(a), ["\t".join(r) for r in rows])
        print("queued:", a.path)
    elif a.qcmd == "next":
        todo = [r for r in rows if r[0] == "todo"][: a.n]
        for r in todo:
            print("\t".join(r[1:]))
        if not todo:
            print("(queue empty)")
    elif a.qcmd == "done":
        hit = False
        for r in rows:
            if len(r) > 2 and r[2] == a.path:
                r[0] = "done"
                r[3:] = [a.note or (r[3] if len(r) > 3 else "")]
                hit = True
        write_lines(qpath(a), ["\t".join(r) for r in rows])
        print("done:" if hit else "not found:", a.path)

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--dir", default=".create-study")
    sp = ap.add_subparsers(dest="cmd", required=True)
    sp.add_parser("init").set_defaults(f=cmd_init)
    p = sp.add_parser("add"); p.set_defaults(f=cmd_add)
    p.add_argument("--file", required=True); p.add_argument("--ver", default="both")
    p.add_argument("--status", default="seen"); p.add_argument("--src", default="")
    p.add_argument("text")
    p = sp.add_parser("find"); p.set_defaults(f=cmd_find)
    p.add_argument("terms", nargs="+"); p.add_argument("-n", type=int, default=15)
    sp.add_parser("index").set_defaults(f=cmd_index)
    sp.add_parser("stats").set_defaults(f=cmd_stats)
    p = sp.add_parser("compact"); p.set_defaults(f=cmd_compact); p.add_argument("--cap", type=int, default=150)
    q = sp.add_parser("queue"); q.set_defaults(f=cmd_queue)
    qs = q.add_subparsers(dest="qcmd", required=True)
    x = qs.add_parser("add"); x.add_argument("addon"); x.add_argument("path"); x.add_argument("--why", default="")
    x = qs.add_parser("next"); x.add_argument("-n", type=int, default=3)
    x = qs.add_parser("done"); x.add_argument("path"); x.add_argument("--note", default="")
    a = ap.parse_args()
    a.f(a)

if __name__ == "__main__":
    main()
