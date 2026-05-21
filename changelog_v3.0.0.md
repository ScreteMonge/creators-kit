# Creator's Kit v3.0.0

A big release. Lots of new features, lots of polish, lots of bug fixes. The biggest chunks are listed first, then everything else.

---

## Headline: Multi-select

You can now hold **Ctrl** or **Shift** in the sidebar, in the manager tree, or on swatches in the scene to select multiple Characters at once. Almost everything in the toolbox is now multi-aware:

- Add, paste, delete, duplicate, drag, or update keyframes — the action applies to every selected Character
- Bulk-edit any field across the selection from the right-side attribute panel
- Pick a folder and its whole subtree is selected
- The object label shows the multi-select count
- "Deselect All" button on the sidebar
- An **Edit** toggle on the manager tree puts the tree into reorder/rename mode (kept separate from selection)

---

## Five new keyframe types

### Camera
A whole new property row for cinematic camera moves. Author camera keyframes (focal point + pitch + yaw) and the engine eases between them smoothly during playback. New camera keyframes default to the live game camera state — no starting from zero. The camera releases freely between keyframes unless you're playing or scrubbed exactly onto a keyframe tick.

### Screen Fade
A fade-to/from-black layer that covers the whole scene (player included). Defaults to opaque black at peak alpha.

### Screen Shake
Camera jitter with adjustable amplitude, frequency, and duration. Plays alongside camera keyframes without fighting them.

### Shield bar & Special bar
Sit alongside the Health bar in the HP / Shield / Special stack above the Character. Per-bar colours, widths, stack order. Defaults match in-game styling.

### Projectile
Fire native OSRS projectiles between two Characters (or a Character and a folder). The projectile model orients along its trajectory, with an optional "Face Trajectory" pitch. Saves persist projectiles. A new **Projectiles** tab in the Cache Searcher lets you pick from the cache.

---

## Boss Healthbar

A pinned, top-center boss-style HP bar that matches the in-game HpbarHud widget. Brown frame, yellow name, green/red HP bar, current/max with a percentage in parentheses.

**Damage animation:** when the boss takes damage, a darker green chunk paints over the lost HP at full width, **holds for half a tick**, then retreats right-to-left over the rest of the tick, revealing red underneath.

---

## Hitsplats automatically drive the bars

Adding, editing, moving, **or deleting** a hitsplat now automatically creates / updates / removes the matching Health, Shield, or Special bar keyframe at the same tick. The bar reflects exactly what the hitsplats show — no more manual upkeep.

- The sync routes by hitsplat sprite: Shield-style sprites → Shield bar, Poise sprites → Special bar, everything else → Health bar
- Damage from all four hitsplat slots at the same tick is summed correctly (double-hits subtract their combined damage)
- The follow-up bar keyframe respects the **remaining** lifespan of the original. A 5-tick bar hit at +2 ticks produces a 3-tick follow-up that ends exactly where the original would have
- Each bar keyframe has a **"Sync hitsplats"** toggle (defaults on, visible on the Health card). Turn it off to lock a bar in place against incoming hitsplats
- The auto-sync only owns the keyframes it created itself — manual edits or hand-authored bar keyframes survive cleanup
- The old "Quick KeyFrame Hitsplat/Bar" button is gone — the auto-sync covers its job

---

## Tools menu

### Random
- **Random Select...** — pick N random direct children of the current folder
- **Jitter keyframe ticks...** — shift every keyframe of a chosen type by a uniform random delta
- **Scatter keyframe ticks...** — re-anchor each Character's keyframe block to a random step-aligned tick. Multi-burst with **Copies (min/max)**, **Step size (min/max)**, and a **per-step copy count** mode for varied "rain density" effects across multiple Characters

### Layout
- **Fill Rectangle...** — with 2 Characters selected as opposite corners, fill the rectangle between them with duplicates on every tile. Stride spinner skips tiles for sparser fills. Duplicates land in a new folder so you can collapse / select / delete them as a group

### Ripple Delete keyframes...
Premiere Pro-style. Remove every keyframe in [from, to] for one property (or all), then shift everything after the deleted span back so the gap collapses. Multi-select aware. Also reachable by **right-clicking on empty timeline space** — opens a context menu pre-filled with the surrounding gap.

### A-B Loop
Drop red (A) and blue (B) markers on the timeline. Playback loops between them, with a 1-tick pause at B before jumping back to A (avoids load spikes from re-seeding spotanims / projectiles).
- **Drag** the markers to reposition
- Click the **X** on a marker's chip to remove it
- Only B set → A defaults to tick 0
- **CTRL+R** now rewinds to A (or to 0 if A isn't set) instead of always rewinding to 0

---

## Camera Lock

Lock the game camera onto a selected Character. The lock follows with a constant-fraction lag, emulating OSRS's natural camera trail. Engage from the manager tree right-click or the sidebar; **Unlock** button on the sidebar. The Oculus Orb is automatically disabled while a lock is active.

---

## Movement & pathfinding

- **Per-step movement.** Add Program Step now finds the full multi-tile path from the previous step to the clicked tile, not just the segment between them. Old single-keyframe walks auto-migrate to per-step format when you open the save
- **Per-step speed** via scroll-to-adjust on Add Program Step
- **Smooth orientation** between chained Movement keyframes — no more snap at every boundary
- Movement restart fix after rewind / scrub-back-then-play

---

## Render Fix

Some cache models ship with a non-canonical scale that breaks animation rigging. The new **Render Fix** toggle (per-Character, in the sidebar) brackets the animation around the canonical 128 scale, then expands the result back to display size. Auto-detects when needed from the NPCComposition. Per-axis scale supported.

---

## Animation keyframes

- **Animation speed** multiplier
- **Last Frame** override + **Pause Ticks** for ranged playback
- A Character can now be set to **face a target** during an animation (this option lives on the Orientation keyframe)

---

## Sub-tile positioning + hotkeys

- **ALT+WASD/R/F** — sub-tile X/Y/Z nudges on selected Characters (defaults to 4 px so it divides 128 cleanly; configurable in settings)
- **ALT+Scroll** / **ALT+=** / **ALT+-** — uniform Character scaling. Ghost preview mirrors the live scale
- **CTRL+V** — drag-paint to place Character copies at every hovered tile

---

## Model Anvil — colour picker

- Compact swatches plus the full JColorChooser tabs
- 2-column swap list
- Click the model to highlight that face's colour
- Click a swatch to highlight every face that was **originally** that colour (works across multiple recolour rules — multi-row support)
- Bidirectional hex field above the chooser
- Effective-colour matching for faces affected by chained recolours

---

## Cache Searcher

- New **Projectiles** tab
- **Animation Searcher**: double-click an animation to preview it on the selected in-world Character (the animation plays once on the Character; no spinner change, no keyframe added, no permanent state)
- The render panel supports **middle-click drag panning** and **WASDRF** keys when focused

---

## Timeline & attribute panel UX

- The timeline area now grows with the toolbox window
- A visible vertical scrollbar appears when property rows are cropped, with a subtle edge fade at the top/bottom indicating more content
- Property rows are ordered alphabetically
- **CTRL+click a property label** = toggle-select every keyframe of that property across all selected Characters
- The "Update" button is **removed**. Every field auto-applies as soon as you change it. A green fade-back on each edited field confirms your change landed
- **Anti-drag deadzone** — small mouse drift between press and release counts as a click, not a drag
- Plain scroll wheel jumps 2 properties at a time (was raw pixel scroll)
- "Add Keyframe" button is **add-only** — use Delete to remove a keyframe
- Right-click on empty timeline space → Ripple Delete with gap pre-filled
- Multi-Character marquee selection shows a wrapped, smaller-font count label

---

## Manager tree

- **Folder right-click** → Rename / Collapse all / Expand all
- **Folder right-click → Select N random Characters** for sampling subsets
- Tree selection no longer auto-expands folders (use the disclosure triangle to expand explicitly)
- Subtree highlighting when hovering a folder
- Recolour menu on right-click

---

## Save / Load

- **File → Load last setup** — quickly reload the most recently opened save
- **Rotating Setup versions** — keep the last N snapshots on a configurable cadence (so you always have a recent backup)
- Old single-keyframe walks auto-migrate to per-step movement format on load

---

## Other things worth mentioning

- Selected-keyframe highlight gently pulses so the selection is unmissable
- Field colours default to neutral grey with a green fade on edit — the old yellow/green ON/OFF states are gone (they were confusing)
- Removed the canvas-wide CTRL+Scroll binding that was conflicting with zoom

---

## Bug fixes (highlights)

- Selection was getting lost on certain inner clicks
- Mixed-type marquee card was leaking state across selections
- Attribute panel layout was unstable when keyframe selection changed
- Movement steps overlapping at speed = 2
- Per-step keyframe spacing was clamping to 1 tick
- Projectile model went stale when its ID changed on a keyframe
- Projectile face-trajectory pitch was rotating around the wrong centroid
- Render-fix bracket was leaking shrunken vertices between Characters sharing a model
- A-B loop wasn't reseeding character state on wrap, so the second loop played wrong
- Lots of copy/paste, undo/redo, and drag/release edge cases sealed up

---
