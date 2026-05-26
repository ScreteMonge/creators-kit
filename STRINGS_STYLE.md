# UI strings style guide

Rules for any user-facing string in the plugin. Reference for me, for you,
and for any future contributor.

## Hover tooltips (`setToolTipText`)

- **Length**: 1–2 sentences max. Prefer 1.
- **Voice**: Imperative. "Set the loop-start marker" — not "Sets..." not "This sets...".
- **What, not why**: Tooltips answer "what does this do?". The "why" goes in
  the question-mark help button, not the tooltip.
- **HTML**: `<html>` wrapping with `<br>` is allowed when you genuinely need
  a two-line break. Single-line tooltips can skip `<html>` entirely.
- **Examples**: avoid parenthetical jargon, internal type names, file-format
  details, implementation notes ("Premiere nested-clip equivalent",
  "shifts by tick", "scope picks one property"). These belong in the help
  button or the code comments.

## Menu items (`JMenu` / `JMenuItem`)

- **No tooltips.** The label is the doc. If a menu item needs explanation,
  the dialog it opens does the explaining, or its visible label is changed
  to be self-evident.

## Question-mark help buttons

- **Long-form is fine here.** This is where the "why" / "when to use this"
  / examples / internal notes go.
- **Still readable English**. Not technical jargon for its own sake — the
  user is not a developer, but not a 5th grader either.

## Modal dialogs (`JOptionPane.showMessageDialog` and dialog body text)

- **Can be more verbose than tooltips** — the user is already paused on the
  modal, reading is the action.
- **Non-technical**. No internal type names, no `[from, to]` notation, no
  "globals (Camera / Fade / Shake) are stored in the central store" —
  user doesn't need to know.
- **Tell them what the tool does and what they need to provide**, not how
  it works under the hood.

## Chat messages (`sendChatMessage`)

- **Narrative**, not terse. "Wrote 12 of 12 keyframes." — not "12/12".
- Same imperative-voice / non-technical rule as the rest.

## Widget labels (`new JLabel("X")`, `new JButton("X")`)

- **Already short.** Leave alone unless the label is misleading.

## Examples

| Bad | Good |
|---|---|
| `<html>Sets the red A marker (loop start) at the current playhead tick.<br>Playback loops from A to B; if only B is set, A defaults to tick 0.</html>` | `Set red A marker (loop start) at the playhead.` |
| `<html>Remove every keyframe in [from, to] for the chosen scope, then shift everything after the deleted span back...</html>` | `Delete keyframes in a range and shift the rest back.` |
| `<html>Group the currently marquee-selected keyframes into a named, coloured block (Premiere nested-clip equivalent). Requires at least 2 keyframes...</html>` | `Group the marquee selection into a named block.` |
| `<html>Manage the list of in-game GameObjects suppressed from rendering. Add via right-click > Hide on any object; this dialog lets you review the list and unhide entries you've added.</html>` | `Manage hidden GameObjects.` |

## Anti-patterns to grep out

- `<html>...<br>...<br>...<br>...</html>` — three-plus `<br>` is almost
  always too long for a tooltip.
- "Useful for X" / "Counterpart to Y" / "Used by Z" — explanatory framing
  that belongs in the help button.
- Mentioning internal data structures by name (`KeyFrameAction`,
  `replacements map`, `the central store`, `CKObject`). Translate to
  user-facing terms.
- Saying both "Set" and "Sets" within the same tooltip — pick one. We
  use imperative ("Set").
