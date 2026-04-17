# DummyPlayer

**DummyPlayer** is a basic utility tool that allows you to easily spawn fake "dummy" players onto your server. They are perfect for testing new mechanics, checking performance with higher player counts, or just experimenting with your setup without needing real people to log in. 

These dummies are exact copies of real players—they appear on the game map and on the server tab list just like regular users!

[![Discord Support](https://img.shields.io/badge/Support-Discord-5865F2?style=for-the-badge&logo=discord)](https://discord.gg/rVsUJ4keZN)
![License](https://img.shields.io/badge/License-Apache%202.0-blue?style=for-the-badge)

---

## Commands & Permissions

| Command | Description | Permission |
| :--- | :--- | :--- |
| `/dummy` | Opens the main GUI Manager. | `hytale.command.dummy` |
| `/dummy create <name>` | Spawns a single dummy at your location. | `hytale.command.dummy.create` |
| `/dummy create <name> <amount> [flags]` | Spawns multiple dummies. *Flags: `--gap`, `--stack`, `--center`* | `hytale.command.dummy.create` |
| `/dummy create <name> <x> <y> <z> [flags]` | Spawns a dummy at specific coordinates. *Flags: `--pitch`, `--yaw`, `--roll`* | `hytale.command.dummy.create` |
| `/dummy create <name> <amount> <x> <y> <z> [flags]`| Bulk spawns at specific coordinates with all optional flags. | `hytale.command.dummy.create` |
| `/dummy delete <name> [--bulk]` | Deletes a specific dummy, or an entire numbered group if using `--bulk`. | `hytale.command.dummy.delete` |
| `/dummy delete * --bulk=true` | Instantly wipes all dummies from the server. | `hytale.command.dummy.delete` |
| `/dummy chat <message/command>` | Sends a chat message or executes a command as a dummy. | `hytale.command.dummy.chat` |
| `/dummy update <name> --skin="[player name]"` | Updates the target dummy to look like a specific player. | `hytale.command.dummy.update` |

## Controlling Dummies

Because dummies utilize the base NPC component in Hytale, you can actually control them using native NPC mechanics! 

You can create a custom Hytale NPC role (like a `follower_role`) and assign it to a dummy using the standard `/npc role` command. They use `Dummy_Player.json` by default, but overriding this role lets Hytale's native AI take over to make them walk, follow, or attack!

*(Note: If you assign a pre-existing role like "zombie" and notice they don't attack you, it's not a bug! Those native roles are specifically configured to avoid damaging their familiars.)*
