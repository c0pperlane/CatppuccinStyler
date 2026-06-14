# CatppuccinStyler

A Paper/Spigot plugin that styles server messages with Catppuccin Mocha gradients.

Works with chat, join/quit/death/advancement messages, plugin messages, and NEZNAMY/TAB.

## Requirements

- Paper/Spigot 1.20+
- Java 17+
- Optional: [PacketEvents](https://github.com/retrooper/packetevents) for system-chat styling
- Optional: [TAB](https://github.com/NEZNAMY/TAB) for tab/scoreboard styling

## Install

1. Drop `CatppuccinStyler-*.jar` into `plugins/`.
2. Restart the server.
3. Edit `plugins/CatppuccinStyler/config.yml` to your liking.

## Commands

| Command | Permission | Description |
|---|---|---|
| `/adminstyle` | `catppuccinstyler.admin` | Open the config GUI |

Aliases: `/mocha`, `/style`

## Features

- 30 built-in Catppuccin gradients
- Per-event and per-plugin gradient defaults
- Learns system message patterns and remembers their gradient
- NEZNAMY/TAB integration: style header/footer and scoreboard lines
- Per-line gradient memory for TAB with automatic cleanup

## Building

```bash
mvn clean package
```

Output jar is in `target/`.
