#!/usr/bin/env python3
"""
Convert old-format Creator's Kit setup .json files (single MovementKeyFrame
with a long multi-tile path per character) into the new per-step format
(one MovementKeyFrame per pair of tiles, chained at consecutive ticks).

The new in-game system creates a fresh MovementKeyFrame each time you Add
Program Step, so individual steps can have their own speed and be edited
independently. Old saves still play, but you can't edit individual steps
because the whole walk lives inside one keyframe. This script splits each
multi-tile path into N-1 two-tile keyframes, preserving the original tick,
plane, poh, speed, turn rate, and loop flag.

Usage:
    python convert_old_movement.py <input.json> [more.json ...]
    python convert_old_movement.py <input.json> --in-place
    python convert_old_movement.py <input.json> -o <output.json>

By default writes each result alongside the input as <name>.converted.json
so the original is never touched. Pass --in-place to overwrite (only after
you've made your own backup -- the plugin's rotating snapshots also keep
recent copies under setup-versions/).

Pass --dry-run to see what would happen without writing anything.
"""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def split_movement_keyframe(mkf: dict) -> list[dict]:
    """Split one old-style MovementKeyFrame into one or more new-style ones.

    Returns a list of keyframes:
      - Empty list if the keyframe has no usable path (skip / drop).
      - Single element (unchanged) if the path is already <= 2 tiles.
      - N-1 elements if the path has N tiles, each holding [tile_i, tile_i+1].

    Tick spacing matches what the new in-game chaining logic produces:
    each subsequent keyframe starts at prev.tick + (prev_tiles - 1) / speed,
    which for a 2-tile path at speed S is prev.tick + 1/S ticks later.
    """
    path = mkf.get("path") or []
    if len(path) <= 1:
        # No movement; nothing to split. Return unchanged so we don't
        # accidentally drop a no-op spawn-position marker the user kept.
        return [mkf]
    if len(path) == 2:
        # Already in new format.
        return [mkf]

    speed = mkf.get("speed", 1.0) or 1.0
    if speed <= 0:
        speed = 1.0  # defend against bad data
    step_duration = 1.0 / speed

    base_tick = float(mkf.get("tick", 0.0))
    loop = bool(mkf.get("loop", False))

    out: list[dict] = []
    for i in range(len(path) - 1):
        segment = [path[i], path[i + 1]]
        sub = {
            # Preserve the parent KeyFrame fields that Gson emits.
            "keyFrameType": mkf.get("keyFrameType", "MOVEMENT"),
            "tick": round(base_tick + i * step_duration, 6),
            # MovementKeyFrame fields.
            "plane": mkf.get("plane", 0),
            "poh": bool(mkf.get("poh", False)),
            "path": [list(segment[0]), list(segment[1])],
            "currentStep": 0,
            "stepClientTick": 0,
            # Loop on a per-step keyframe doesn't mean what it used to mean
            # (the old single-KF loop replayed the whole multi-tile path).
            # Put loop on the FIRST step only -- if you actually want a
            # repeating walk in the new system you'd typically loop one
            # step or a small chain anyway. Easy to toggle after import.
            "loop": loop if i == 0 else False,
            "speed": float(speed),
            "turnRate": int(mkf.get("turnRate", 0)),
        }
        out.append(sub)
    return out


def convert_setup(data: dict) -> tuple[dict, dict]:
    """Walks the SetupSave dict and rewrites movementKeyFrames per character.

    Returns (converted_data, stats). stats has per-character split counts and
    a totals summary, used for the printed report.
    """
    stats = {"characters": [], "total_input_mkfs": 0, "total_output_mkfs": 0, "total_splits": 0}

    saves = data.get("saves")
    if not isinstance(saves, list):
        return data, stats

    for char in saves:
        if not isinstance(char, dict):
            continue
        mkfs = char.get("movementKeyFrames")
        if not isinstance(mkfs, list):
            continue

        input_count = len(mkfs)
        new_mkfs: list[dict] = []
        split_count = 0
        for mkf in mkfs:
            if not isinstance(mkf, dict):
                new_mkfs.append(mkf)
                continue
            replacements = split_movement_keyframe(mkf)
            if len(replacements) > 1:
                split_count += 1
            new_mkfs.extend(replacements)

        # Sort by tick to keep the array monotonic (Gson read path expects it).
        new_mkfs.sort(key=lambda k: float(k.get("tick", 0.0)) if isinstance(k, dict) else 0.0)
        char["movementKeyFrames"] = new_mkfs

        stats["characters"].append({
            "name": char.get("name", "<unnamed>"),
            "input_mkfs": input_count,
            "output_mkfs": len(new_mkfs),
            "splits": split_count,
        })
        stats["total_input_mkfs"] += input_count
        stats["total_output_mkfs"] += len(new_mkfs)
        stats["total_splits"] += split_count

    return data, stats


def print_stats(path: Path, stats: dict) -> None:
    print(f"\n[{path.name}]")
    print(f"  characters processed: {len(stats['characters'])}")
    print(f"  movement keyframes: {stats['total_input_mkfs']} -> {stats['total_output_mkfs']}"
          f"  (split {stats['total_splits']} multi-tile keyframe(s))")
    interesting = [c for c in stats["characters"] if c["splits"] > 0]
    if interesting:
        print("  per-character splits:")
        for c in interesting:
            print(f"    - {c['name']}: {c['input_mkfs']} -> {c['output_mkfs']}"
                  f"  ({c['splits']} split)")


def process_file(in_path: Path, out_path: Path, dry_run: bool) -> bool:
    try:
        with in_path.open("r", encoding="utf-8") as f:
            data = json.load(f)
    except (OSError, json.JSONDecodeError) as e:
        print(f"ERROR reading {in_path}: {e}", file=sys.stderr)
        return False

    converted, stats = convert_setup(data)
    print_stats(in_path, stats)

    if dry_run:
        print(f"  (dry-run; not writing {out_path.name})")
        return True

    try:
        with out_path.open("w", encoding="utf-8") as f:
            # No indent -- Creator's Kit writes single-line JSON via Gson.toJson,
            # so we match that to keep diffs minimal if the user diffs by hand.
            json.dump(converted, f, separators=(",", ":"), ensure_ascii=False)
    except OSError as e:
        print(f"ERROR writing {out_path}: {e}", file=sys.stderr)
        return False

    print(f"  wrote {out_path}")
    return True


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Convert old Creator's Kit setup files (single multi-tile MovementKeyFrame "
                    "per character) into the new per-step format (one keyframe per pair of tiles).",
        epilog="Example: python convert_old_movement.py my_setup.json")
    parser.add_argument("files", nargs="+", help="Setup .json file(s) to convert.")
    out_group = parser.add_mutually_exclusive_group()
    out_group.add_argument("-o", "--output",
                           help="Output path. Only valid when converting a single file.")
    out_group.add_argument("--in-place", action="store_true",
                           help="Overwrite the input file. ONLY use after you've backed up. "
                                "Creator's Kit's rotating setup-versions/ folder also keeps copies.")
    parser.add_argument("--dry-run", action="store_true",
                        help="Show what would change without writing anything.")
    args = parser.parse_args()

    if args.output and len(args.files) > 1:
        print("ERROR: -o/--output only works with a single input file.", file=sys.stderr)
        return 2

    ok = True
    for f in args.files:
        in_path = Path(f)
        if not in_path.exists():
            print(f"ERROR: {in_path} not found", file=sys.stderr)
            ok = False
            continue

        if args.in_place:
            out_path = in_path
        elif args.output:
            out_path = Path(args.output)
        else:
            # Default: my_setup.json -> my_setup.converted.json next to original.
            out_path = in_path.parent / (in_path.stem + ".converted" + in_path.suffix)

        ok = process_file(in_path, out_path, args.dry_run) and ok

    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
