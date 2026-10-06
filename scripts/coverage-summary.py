#!/usr/bin/env python3
"""Prints line/branch coverage per layer from the Kover XML report (app/build/reports/kover/report.xml)."""
import sys
import xml.etree.ElementTree as ET

path = sys.argv[1] if len(sys.argv) > 1 else "app/build/reports/kover/report.xml"
root = ET.parse(path).getroot()
layers = {}


def layer_of(package: str) -> str:
    parts = package.split("/")
    if len(parts) > 3 and parts[3] == "ui":
        return "ui (view models)"
    return parts[3] if len(parts) > 3 else "other"


def counter(el, kind):
    for c in el.findall("counter"):
        if c.get("type") == kind:
            return int(c.get("missed")), int(c.get("covered"))
    return 0, 0


for pkg in root.findall("package"):
    layer = layer_of(pkg.get("name"))
    agg = layers.setdefault(layer, {"LINE": [0, 0], "BRANCH": [0, 0]})
    for kind in ("LINE", "BRANCH"):
        m, c = counter(pkg, kind)
        agg[kind][0] += m
        agg[kind][1] += c

print(f"{'layer':<18}{'lines':>16}{'branches':>16}")
total = {"LINE": [0, 0], "BRANCH": [0, 0]}
for layer, agg in sorted(layers.items()):
    cells = []
    for kind in ("LINE", "BRANCH"):
        m, c = agg[kind]
        total[kind][0] += m
        total[kind][1] += c
        cells.append(f"{c}/{m + c} = {100 * c / (m + c):.1f}%" if m + c else "-")
    print(f"{layer:<18}{cells[0]:>16}{cells[1]:>16}")
cells = []
for kind in ("LINE", "BRANCH"):
    m, c = total[kind]
    cells.append(f"{c}/{m + c} = {100 * c / (m + c):.1f}%" if m + c else "-")
print(f"{'TOTAL':<18}{cells[0]:>16}{cells[1]:>16}")
