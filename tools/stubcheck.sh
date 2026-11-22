#!/bin/sh
# Offline check: compiles src/main/java against hand-written API stubs in tools/stubs.
# The stubs were written from memory, so this proves our own classes fit together, NOT that the real NeoForge/Create calls exist.
set -e
cd "$(dirname "$0")/.."
rm -rf /tmp/fw_stubs /tmp/fw_main && mkdir /tmp/fw_stubs /tmp/fw_main
JAVAC="javac"; command -v javac >/dev/null 2>&1 || JAVAC="java -m jdk.compiler/com.sun.tools.javac.Main"
find tools/stubs -name '*.java' > /tmp/fw_stubs.txt
$JAVAC -proc:none -d /tmp/fw_stubs @/tmp/fw_stubs.txt
find src/main/java -name '*.java' > /tmp/fw_main.txt
$JAVAC -proc:none -cp /tmp/fw_stubs -d /tmp/fw_main @/tmp/fw_main.txt
echo "stub compile OK: $(find /tmp/fw_main -name '*.class' | wc -l) classes"
