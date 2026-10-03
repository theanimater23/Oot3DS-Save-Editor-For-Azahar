# OoT3D Save Editor

A save file editor for *The Legend of Zelda: Ocarina of Time 3D*, built for the
Azahar emulator (and any other setup that gives you access to a raw save file).
Runs entirely in your browser — nothing you load is ever uploaded anywhere.

![Story Progress panel](screenshots/hero.png)

## Features

- **Everything you'd expect**: rupees, hearts, magic, ammo, small keys, file name.
- **Full inventory editor**: all 24 item slots (including bottle contents and
  trade items), equipment, and every capacity upgrade.
- **Gold Skulltulas, individually** — not just a total count. Each of the 22
  in-game regions gets its own grid of tokens, sized to that region's real
  count (sourced from published token-count guides, cross-checked to sum to
  the documented totals).
- **Dungeon Completion panel** — one click to fully clear a dungeon: reward,
  map, compass, boss key, every Skulltula in it, all chests, and the boss
  arena.
- **Story Progress panel** — all 145 story/event flags from the game's own
  decompiled source, so you can skip cutscenes you've already seen or jump
  ahead in the story. See [Accuracy](#accuracy--the-guess-tags) below.
- **A real, verified checksum.** OoT3D validates each save file with a
  checksum before showing it in-game. This editor uses the actual algorithm
  (CRC-16/ARC over the full save, sourced from the original savedata-exploit
  tooling written by the people who reverse-engineered this file format) and
  recalculates it correctly on every save. Edited files load in-game.

![Dungeon Completion panel](screenshots/dungeon-completion.png)

## How to use it

**Option 1 — just open the file.** Download `oot3d-save-editor.html` from
this repo and open it in any browser (double-click it, or drag it into a
browser window). That's it — it's one self-contained file.

**Option 2 — host it yourself.** Turn on GitHub Pages for this repo
(Settings → Pages → deploy from the `main` branch) and you'll have a shareable
link to the live tool.

Then:

1. **Find your save file.** In Azahar, right-click the game in your game list
   and look for something like *Open Save Data Location* — that opens a
   folder with your save file in it.
2. **Make a spare copy** of that file before you touch anything.
3. **Load it into the editor**, make your changes — every field updates
   instantly.
4. **Download the edited file.** It's saved as a new file; your original is
   never overwritten by the tool itself. Copy the new file into the save
   folder from step 1, replacing the original, then load your save in the
   game. **Azahar must be fully closed** while you do this, or it'll
   overwrite your edit when it next writes to disk.

### Android app

The `android/` folder is a full Android Studio project that wraps the same
editor in a native app with real file access (Android's Storage Access
Framework): pick your save file once, and the app remembers it and writes
changes straight back in place — no manual copying. See
[`android/README.md`](android/README.md) for build instructions.

## Accuracy, and the GUESS tags

![Story Progress flags with GUESS tags](screenshots/story-progress.png)

This save format isn't officially documented by Nintendo. Everything here is
reverse-engineered, from a few different sources with different confidence
levels — I think it's more useful to be upfront about that than to present
guesses as facts:

- **Checksum**: verified directly against real save files. High confidence.
- **Core fields** (health, rupees, items, equipment, keys): sourced from a
  long-standing community save-format writeup, cross-checked against three
  real save files. High confidence.
- **Story/event flags**: sourced from the actual decompiled N64/3DS game
  source. About 40 of the 145 flags have a name and purpose confirmed
  directly in the source code's own comments (shown as plain text). The rest
  are tagged **GUESS** — my own inference from neighboring flags, the game's
  order of events, or partial evidence, clearly marked as unconfirmed rather
  than stated as fact.
- **A few things I couldn't verify**: exactly which bit maps to which named
  Gold Skulltula within a region (only the per-region total is confirmed),
  and the exact bit order for dungeon boss-key/compass/map flags (carried
  over from the N64 version's layout, not independently confirmed for 3DS).

**If you figure out what a GUESS flag actually does, open an issue** (there's
a template for it) with the flag number and what you observed — that's how
several flags already went from guesses to confirmed names.

## Credits

This tool wouldn't exist without the people who did the actual reverse
engineering:

- Save file offsets: the long-running community writeup at CloudModding's OoT3D
  save page, and the [OcarinaCTRComposer RAM Address Map](https://github.com/samaBR85/OcarinaCTRComposer/wiki/RAM-Address-Map).
- Checksum algorithm: `oot3d_savetool.c` from
  [smealum/oot3dhax](https://github.com/smealum/oot3dhax), written by the
  original savedata-exploit researchers.
- Story/event flag names: the [zeldaret/oot decompilation project](https://github.com/zeldaret/oot).

## License

MIT - see [LICENSE](LICENSE). This is an independent fan tool, not affiliated
with Nintendo. See the note at the bottom of the license file.
