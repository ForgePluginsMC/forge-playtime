# ForgePlaytime

Track player playtime and hand out claimable milestone rewards through a 54-slot GUI. Playtime accrues once per second for every online player, persists to disk, and milestones unlock as players cross configured thresholds — each showing a claimed, available, or locked state until the player clicks to claim one-time console-command rewards. Original implementation; zero dependencies beyond the Paper API (Adventure ships with Paper).

## Features

- Per-second playtime tracking for online players, persisted to `playtimes.yml`
- 54-slot milestone GUI with claimed / available / locked states and a live progress footer
- One-time console-command rewards per milestone (`%player%` placeholder)
- Milestones fully defined in `config.yml`: time threshold, reward commands, GUI item, MiniMessage name and lore
- Human time parsing (`30s`, `45m`, `2h`, `1d`, `1w`) and compact duration formatting (`1d 2h 3m`)
- Admin view of any player's playtime (online or offline) and hot reload of milestones
- Autosave every 5 minutes, on quit/kick, and on disable — a crash loses at most a few minutes

## Requirements

- Paper 26.3+ (`api-version: '26.3'`)
- Java 25

## Installation

Drop `ForgePlaytime-1.0.0.jar` into your server's `plugins/` folder and restart. A default `config.yml` is generated on first run.

## Commands

| Command | Arguments | Description | Permission |
|---|---|---|---|
| `/playtime` | — | Opens your milestone GUI (players only) | `forgeplaytime.use` |
| `/playtime` | `<player>` | Shows another player's playtime and claim count; works for offline players with stored data (case-insensitive name lookup) | `forgeplaytime.admin` |
| `/fplaytime` | `reload` | Reloads `config.yml` and rebuilds the milestone list | `forgeplaytime.admin` |

## Permissions

| Permission | Default | Description |
|---|---|---|
| `forgeplaytime.use` | `true` | Use `/playtime` to view and claim milestones |
| `forgeplaytime.admin` | `op` | Use `/playtime <player>` and `/fplaytime reload` |

## Configuration

`config.yml` contains a single top-level list, `milestones`. Each entry defines one milestone:

| Key | Type | Default | Description |
|---|---|---|---|
| `time` | string (required) | — | Threshold to unlock; units `s`/`m`/`h`/`d`/`w` (e.g. `30m`, `2h`, `1d`) |
| `rewards` | list of strings | `[]` | Console commands run once per player on claim; `%player%` is replaced with the claiming player's name |
| `gui-item` | string | `CHEST` | Material name for the GUI icon; must be a valid item material |
| `gui-name` | string | `<white>Milestone` | MiniMessage display name |
| `gui-lore` | list of strings | `[]` | MiniMessage lore lines |

Example:

```yaml
milestones:
  - time: "30m"
    rewards:
      - "give %player% diamond 5"
    gui-item: "DIAMOND"
    gui-name: "<aqua>30 Minutes"
    gui-lore:
      - "<gray>Thanks for sticking around!"
      - "<gray>Reward: <white>5 diamonds"
  - time: "1d"
    rewards:
      - "give %player% elytra 1"
    gui-item: "ELYTRA"
    gui-name: "<gold>1 Day"
    gui-lore:
      - "<gray>An entire day of playtime. Legendary."
```

Notes:

- Invalid milestones are skipped with a console warning (missing `time`, bad time format, or non-item `gui-item`).
- The GUI layout fits up to 16 milestones; extras are not rendered.
- Claimed state is stored per player, so each milestone's rewards can only be claimed once.
- GUI clicks and drags are fully cancelled inside the milestone inventory.

## Data storage

Player data lives in `plugins/ForgePlaytime/playtimes.yml`:

```yaml
players:
  <uuid>:
    name: "Steve"
    seconds: 3723
    claimed: [0, 1]
```

## Building from source

```bash
bash build.sh
```

Compiles with `javac` directly against the Paper API jars (no Gradle daemon needed): JDK 25 at `~/workspace/.toolchains/jdk-25.0.4.1+1`, `-Werror` with `-Xlint:deprecation,unchecked`. The build script uses `$HOME`-relative paths, so run it as the owning user from anywhere.

## Code quality

- No deprecated APIs anywhere — enforced by `-Werror` at compile time.
- Nullness annotations throughout: `@NotNullByDefault` on the `com.forge.playtime` package, with explicit `@Nullable` on genuinely nullable returns (`PlaytimeStore.findByName`, `PlaytimeGui.Holder.getInventory`).
