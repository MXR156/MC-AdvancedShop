# AdvancedShop

A chest-based GUI shop plugin for **Paper 26.3** (Java 25). Players right-click a shop chest to open a GUI
where they can buy items, sell items, buy permissions or buy "special rewards" that run commands.
Admins build and configure everything in-game, no config editing required.

## Building

Requires **JDK 25** and Maven.

```bash
mvn package
```

The plugin jar is written to `target/AdvancedShop-<version>.jar`. Drop it in your server's `plugins/` folder.

### Optional dependencies

- **Vault** (or **VaultUnlocked**) + an economy plugin (EssentialsX, CMI, ...): needed for prices above 0.
- A permissions plugin (e.g. **LuckPerms**): needed for permission purchases. These are granted through
  Vault, or with a console command (see `permission-grant` in `config.yml`).

## Creating a shop

1. Rename a chest, trapped chest or barrel in an anvil to `[ashop.<name>]`, e.g. `[ashop.tools]`.
2. Place it (you need `ashop.admin` or OP). The shop `tools` is created and linked to that chest.
   You can also look at any existing chest and run `/ashop create tools`.
3. **Sneak + right-click** the chest to open the editor.
4. Drag an item from your inventory onto an empty slot (or shift-click it). A copy is added, so you keep your item.
   The stack size you drop becomes the amount per purchase.
5. Click the new entry to open the **item editor**:

| Button | What it does |
| --- | --- |
| Type | Cycles between `ITEM` (gives/buys back an item), `PERMISSION` (grants permission nodes) and `COMMAND` (only runs commands) |
| Buy price | Left-click, then type the price in chat. Right-click disables buying |
| Sell price | Same as buy price. Only `ITEM` entries can be sold |
| Amount | How many items are given or taken per purchase |
| Reward item | Click while holding an item on your cursor to set what is given (the display icon can be different) |
| Display icon | Click while holding an item on your cursor to change the icon |
| Permissions | Add the permission node(s) granted on purchase |
| Commands | Console commands run on every purchase (`%player%` and `%uuid%` are replaced) for any type |
| Display name / Description | MiniMessage name and extra lore lines |
| Move to slot | Moves the entry (swaps with whatever is there) |
| Delete | Shift-click to remove the entry |

Several chests can point at the same shop (`/ashop link <shop>`). Admins must crouch to break a shop chest (so it can't happen by accident). Shop chests can't be broken by
non-admins, blown up or used by hoppers, and they never merge into double chests.

## Using a shop (players)

| Click | Action |
| --- | --- |
| Left-click | Buy once |
| Shift + left-click | Buy a full stack's worth |
| Right-click | Sell once |
| Shift + right-click | Sell everything that matches |

## Commands

| Command | Permission |
| --- | --- |
| `/ashop create <shop>`: make the chest you look at a shop | `ashop.admin` |
| `/ashop link <shop>` / `/ashop unlink` | `ashop.admin` |
| `/ashop edit <shop>` | `ashop.admin` |
| `/ashop open <shop> [player]`: open a shop GUI (for NPC plugins, menus, console) | `ashop.command.open` (other players: `ashop.admin` or console) |
| `/ashop settitle <shop> <MiniMessage title>` | `ashop.admin` |
| `/ashop setrows <shop> <1-6>` | `ashop.admin` |
| `/ashop setpermission <shop> [permission\|none]`: restrict who can open the shop | `ashop.admin` |
| `/ashop delete <shop>`, `/ashop list`, `/ashop reload` | `ashop.admin` |

`ashop.use` (everyone by default) is needed to buy and sell.

### Restricting shops

By default anyone with `ashop.use` can open a shop. To limit one:

- `/ashop setpermission vip`: the `vip` shop now requires `ashop.shop.vip`.
- `/ashop setpermission vip myserver.rank.gold`: use any permission node you like.
- `/ashop setpermission vip none`: open it to everyone again.

Admins (`ashop.admin`) can always open every shop. The check also applies to `/ashop open`, so NPCs
can't be used to get around it. A restricted shop can be combined with a `PERMISSION` item in another
shop, which lets players buy access to it.

## NPC shopkeepers

There's no built-in NPC support yet, but any NPC plugin that runs console commands on click works now.
For example, with Citizens:

```
/npc command add ashop open tools <p>
```

(Citizens runs these as console by default and replaces `<p>` with the clicking player. Check the Citizens docs for your version.)

## Data

- `plugins/AdvancedShop/config.yml`: messages, lore format and permission-grant mode.
- `plugins/AdvancedShop/shops/<name>.yml`: one file per shop. Items are stored in Paper's
  version-safe binary format, so they keep working after Minecraft updates.

## Ideas / roadmap

- Built-in NPC shopkeepers
- Multiple pages per shop
- Limited stock and per-player purchase limits or cooldowns
- Dynamic prices and transaction logs
- Using item currencies instead of money
