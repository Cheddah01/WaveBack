# WaveBack 👋🏻

WaveBack is a PaperMC plugin for custom join/leave messages and welcome-back rewards. Players can greet returning players with phrases like `wb`, and WaveBack can reward those greeters with commands, items, or Vault economy money.

## Features

- Custom join and leave messages with MiniMessage formatting
- Optional join and leave sounds
- Optional join and leave fireworks
- Configurable welcome-back greeting window
- Greeting triggers like `wb`, `welcome back`, `wbb`, and `dubs`
- Reward bundles with commands, items, and optional Vault economy money
- Cooldowns, per-join reward caps, and minimum playtime checks to reduce farming
- Reload command and solo reward test command

## Requirements

- PaperMC 1.21.x
- Java 21
- Vault is optional and only required for money rewards
- An economy plugin is required if you enable Vault money rewards

## Installation

1. Download the latest WaveBack jar.
2. Put it in your server's `plugins` folder.
3. Restart the server.
4. Edit the generated config:

```text
plugins/WaveBack/config.yml
```

5. Reload the plugin:

```text
/wb reload
```

## Commands

| Command | Description | Permission |
| --- | --- | --- |
| `/waveback reload` | Reloads the config | `waveback.reload` |
| `/wb reload` | Alias for reload | `waveback.reload` |
| `/wb testreward` | Gives yourself the configured reward bundle for testing | `waveback.testreward` |

## Permissions

| Permission | Default | Description |
| --- | --- | --- |
| `waveback.reload` | OP | Allows reloading WaveBack |
| `waveback.greet` | Everyone | Allows earning rewards for greeting returning players |
| `waveback.bypasscooldown` | False | Bypasses the per-greeter reward cooldown |
| `waveback.testreward` | OP | Allows testing the configured reward bundle |

## Configuration

WaveBack adds missing config keys automatically when the plugin starts or `/wb reload` runs.

Join and leave messages support MiniMessage:

```yaml
join:
  enabled: true
  message: "<gray>[<green>+<gray>] <aqua>{player}</aqua> arrived!"
  silent: false

leave:
  enabled: true
  message: "<gray>[<red>-<gray>] <aqua>{player}</aqua> headed out."
  silent: false
```

Welcome-back rewards are configured under `rewards`:

```yaml
rewards:
  enabled: true
  greeting-window-seconds: 30
  triggers:
    - wb
    - welcome back
    - wbb
    - dubs
  whole-message-only: true
  max-rewards-per-join: 3
  reward-cooldown-seconds: 300
  reward-on-first-join: false
  minimum-joiner-playtime-minutes: 10
  bundle:
    - type: money
      amount: 50.0
    - type: item
      material: DIAMOND
      amount: 1
      name: "<aqua>Thank-You Diamond"
      lore:
        - "<gray>For being a kind soul."
  messages:
    reward-received: "<gray>✦ Thanks for welcoming <yellow>{joiner}</yellow><gray> back!"
    broadcast: ""
```

## Reward Types

| Type | Description |
| --- | --- |
| `command` | Runs a console command. Supports `{player}` and `{joiner}` |
| `money` | Gives Vault economy money when Vault and an economy plugin are installed |
| `item` | Gives a Minecraft item, with optional MiniMessage name and lore |

If a rewarded player's inventory is full, item leftovers are dropped at their feet.

## Placeholders

| Placeholder | Meaning |
| --- | --- |
| `{player}` | The joining player in join/leave messages, or the greeter in reward messages/commands |
| `{display_name}` | The joining player's display name in join/leave messages |
| `{joiner}` | The returning player being welcomed back in reward messages/commands |

## MiniMessage Formatting

MiniMessage is Paper's modern text format for colors, decorations, gradients, and other rich chat styling.

| Type | Examples |
| --- | --- |
| Basic colors | `<red>`, `<green>`, `<yellow>`, `<aqua>`, `<gray>`, `<white>` |
| Hex colors | `<#FF5733>text</#FF5733>` or `<#FF5733>text` |
| Decorations | `<bold>`, `<italic>`, `<underlined>`, `<strikethrough>`, `<obfuscated>` |
| Reset | `<reset>` |
| Gradients | `<gradient:#ff5733:#ffaa00>text</gradient>` |

Closing tags are optional in many cases, but recommended for nested formatting.

Coming from legacy `&` codes?

| Legacy | MiniMessage |
| --- | --- |
| `&a` | `<green>` |
| `&c` | `<red>` |
| `&e` | `<yellow>` |
| `&b` | `<aqua>` |
| `&7` | `<gray>` |
| `&f` | `<white>` |
| `&l` | `<bold>` |
| `&o` | `<italic>` |
| `&r` | `<reset>` |
| `&#RRGGBB` | `<#RRGGBB>` |

Official docs: https://docs.advntr.dev/minimessage/format.html

## Sounds And Fireworks

Sound names use Bukkit's `Sound` enum names, such as `ENTITY_EXPERIENCE_ORB_PICKUP`, `BLOCK_NOTE_BLOCK_PLING`, or `ENTITY_ITEM_BREAK`.

Firework types use Bukkit's `FireworkEffect.Type` names: `BALL`, `BALL_LARGE`, `BURST`, `CREEPER`, and `STAR`.

## Building

This project uses Gradle and Java 21.

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The jar is created in:

```text
build/libs/waveback-1.0.0-b6.jar
```

Update `buildNumber` in `gradle.properties` before cutting a release build.
