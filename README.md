# CustomJoinMessage

A small PaperMC 1.21.11 plugin that lets you customize the public join and leave messages shown on your server.

## Build

This project uses Gradle and Java 21.

```bash
gradle build
```

The plugin jar will be created in:

```text
build/libs/custom-join-message-1.0.0-b6.jar
```

Copy that jar into your Paper server's `plugins` folder and restart the server.

## Release builds

Update `buildNumber` in `gradle.properties` for each release. The plugin version and jar filename both include that build number, such as `1.0.0-b6`.

When the plugin starts or `/cjm reload` runs, missing config keys are copied from the bundled default config into your existing server config.

## Configure

After the first server start, edit:

```text
plugins/CustomJoinMessage/config.yml
```

Example:

```yaml
join:
  enabled: true
  message: "<gray>[<green>+<gray>] <aqua>{player}</aqua> arrived!"
  silent: false
  sound:
    enabled: true
    name: ENTITY_EXPERIENCE_ORB_PICKUP
    volume: 1.0
    pitch: 1.0
  firework:
    enabled: true
    power: 1
    type: BALL
    flicker: false
    trail: true
    colors:
      - LIME
      - YELLOW
    fade-colors:
      - AQUA

leave:
  enabled: true
  message: "<gray>[<red>-<gray>] <aqua>{player}</aqua> headed out."
  silent: false
  sound:
    enabled: true
    name: ENTITY_ITEM_BREAK
    volume: 1.0
    pitch: 1.0
  firework:
    enabled: true
    power: 1
    type: BALL
    flicker: false
    trail: true
    colors:
      - RED
      - ORANGE
    fade-colors:
      - YELLOW
```

Then run:

```text
/customjoinmessage reload
```

or:

```text
/cjm reload
```

## Placeholders

- `{player}`: the player's username
- `{display_name}`: the player's display name

Messages use MiniMessage formatting, such as `<green>`, `<yellow>`, `<bold>`, and gradients.

Sound names use Bukkit's `Sound` enum names, such as `ENTITY_EXPERIENCE_ORB_PICKUP`, `BLOCK_NOTE_BLOCK_PLING`, or `ENTITY_ITEM_BREAK`.

Firework types use Bukkit's `FireworkEffect.Type` names: `BALL`, `BALL_LARGE`, `BURST`, `CREEPER`, and `STAR`.
