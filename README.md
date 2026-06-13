# **Tradable** 💰🏪⚔️

---

**1. Tradable is a fully featured economy and trading plugin for modern Minecraft servers, built to turn simple money systems into a complete player-driven marketplace.**

**2. It combines a built-in economy, server shop, auction house, bounty system, baltop leaderboard, player payments, admin shop management, GUI menus, compact money formatting, and PlaceholderAPI support into one polished and connected experience.**

**3. Whether you run a survival server, an SMP, a PvP world, or a progression-based economy server, Tradable gives your players more ways to earn, trade, compete, and spend — all through a clean, modern system.**

---

## **✨ Why Tradable?**

Tradable is not just a balance plugin.

It is a complete economy ecosystem designed for servers that want trade and progression to feel like real gameplay instead of background utility.

With Tradable, your server gets:

- ✅ a complete player economy
- ✅ a full Auction House
- ✅ a configurable Server Shop
- ✅ a competitive Bounty system
- ✅ a visual Balance Top leaderboard
- ✅ player-to-player payments
- ✅ polished GUI menus
- ✅ refined admin market tools
- ✅ PlaceholderAPI integration
- ✅ compact values like 1K / 2.5M / 3B
- ✅ clean command UX for both admins and normal players

Everything is designed to feel consistent, clean, and practical for real server use.

---

## **🔧 Core Features**

### ✅ Built-In Economy

Tradable includes a complete balance system used across every part of the plugin.

- Player balances
- Deposits and withdrawals
- Money transfers between players
- Clean compact currency support
- Formatted balance output for commands and menus
- Economy data used across shop, auction house, bounty, and baltop systems

---

### ✅ Auction House

Create a real player market where players can list items and browse active listings in a GUI.

- Sell held items with /ahsell <price>
- Browse listings with /ah
- Buy directly from the menu
- Remove your own listings
- Price and time sorting
- Configurable listing limits
- Optional bypass permission for limits
- Proper item delivery without decorative GUI lore remaining on purchased items

This gives your server a true player-driven trading layer.

---

### ✅ Server Shop

Tradable includes a configurable server shop system with categories, icons, slot placement, and item pricing.

- Category-based shop browsing
- Item pricing and layout control
- Category icon customization
- Absolute slot and multi-page support
- Shop menu presentation
- Preserves original item metadata correctly
- Admin tools for adding, removing, pricing, and styling shop listings

Perfect for servers that want a curated marketplace in addition to player trading.

---

### ✅ Bounty System

- Encourage competition and PvP with active player bounties.
-
- Place bounties with /bountyset
- Browse active bounties in a GUI
- Track bounty totals by target
- Sort bounty listings
- Add meaningful risk and reward to survival and SMP gameplay

---

### ✅ Baltop Leaderboard

Let players compete for wealth and prestige.

Visual balance rankings
Head-based player display
Compact and full balance values
Great for long-term progression and economy competition

---

### ✅ GUI Menus

Tradable is built around polished inventory menus instead of command clutter.

Included interfaces:

- Auction House menu
- Shop menu
- Purchase confirmation menu
- Bounty menu
- Baltop menu

Features:

- paging
- sorting
- category browsing
- cleaner navigation
- purchase previews
- more intuitive player interaction

---

### ✅ Compact Money Formatting

Tradable supports modern short money values, so nobody has to type or read giant walls of zeroes.

Examples:

- 100
- 1K
- 2.5K
- 1M
- 3.25B

This works across player commands, admin commands, GUI displays, and placeholders.

---

### ✅ Cleaner Command Experience

Tradable is designed to look professional in normal use.

- normal players see clean commands like /bal, /shop, and /ah
- admin help and player help are clearly separated
- tab completion is refined for both players and staff
- namespaced clutter such as tradable:ah can be hidden from normal players

That means the plugin feels cleaner and more polished from the player’s perspective.

---
## 🎯 Player Commands
| Command                       | Description                                          |
| ----------------------------- | ---------------------------------------------------- |
| `/bal`                        | View your current balance                            |
| `/baltop`                     | Open the balance leaderboard                         |
| `/shop`                       | Open the server marketplace                          |
| `/provide <player> <money>`   | Send money to another online player                  |
| `/bounty`                     | Open the bounty board                                |
| `/bountyset <player> <money>` | Place a bounty on a player                           |
| `/ah`                         | Open the auction house                               |
| `/ahsell <money>`             | List the item in your main hand on the auction house |

---

## 🛠️ Admin Commands

Tradable includes a full admin command system for managing the economy and shop directly in-game.
**/tradable subcommands:**
| Subcommand                           | Description                                    |
| ------------------------------------ | ---------------------------------------------- |
| `addmoney [player] <money>`          | Add money to yourself or another player        |
| `minusmoney [player] <money>`        | Remove money from yourself or another player   |
| `addcategory <name> <slot>`          | Create or move a shop category                 |
| `removecategory <name>`              | Remove a shop category                         |
| `removeallcategory`                  | Remove all shop categories                     |
| `additem <category> <slot> <price>`  | Add the held item into a shop category slot    |
| `removeitem`                         | Remove matching shop entries for the held item |
| `setprice <category> <price>`        | Set price by matching the held item            |
| `setprice <category> <slot> <price>` | Set price directly by slot                     |
| `seticoncategory <category>`         | Use the held item as the category icon         |
| `reload`                             | Reload config, messages, and storage files     |
| `renamecategory <current name> <new name>`                             | Rename a category     |

---

## 🔐 Permissions
| Permission                     | Description                   | Default |
| ------------------------------ | ----------------------------- | ------- |
| `tradable.admin`               | Access to admin commands      | `OP`    |
| `tradable.auction.bypasslimit` | Bypass auction listing limits | `false` |

---

## 🔁 PlaceholderAPI Support

Tradable includes built-in PlaceholderAPI expansion support, making it easy to integrate with:

- scoreboards
- tab lists
- holograms
- chat formatting
- menus
- NPC text
- other placeholder-compatible plugins

You can display real-time economy and trading data anywhere on your server.
| Placeholder                               | Description                                         |
| ----------------------------------------- | --------------------------------------------------- |
| `%tradable_balance%`                      | Player balance in compact format                    |
| `%tradable_balance_compact%`              | Compact balance display                             |
| `%tradable_balance_full%`                 | Full balance display                                |
| `%tradable_balance_raw%`                  | Raw full balance output                             |
| `%tradable_auction_count%`                | Number of auction listings owned by the player      |
| `%tradable_auction_total_value%`          | Total value of the player’s auction listings        |
| `%tradable_auction_total_value_full%`     | Full-value auction listing total                    |
| `%tradable_auction_total_value_raw%`      | Raw full auction total                              |
| `%tradable_bounty_target_count%`          | Number of bounty entries targeting the player       |
| `%tradable_bounty_target_total%`          | Total bounty value on the player                    |
| `%tradable_bounty_target_total_full%`     | Full-value bounty total                             |
| `%tradable_bounty_target_total_raw%`      | Raw bounty total                                    |
| `%tradable_auction_total_listings%`       | Total number of active auction listings server-wide |
| `%tradable_auction_total_value_all%`      | Compact total value of all auction listings         |
| `%tradable_auction_total_value_all_full%` | Full total value of all auction listings            |
| `%tradable_auction_total_value_all_raw%`  | Raw auction total value                             |
| `%tradable_bounty_total_entries%`         | Total number of bounty entries server-wide          |
| `%tradable_bounty_total_value_all%`       | Compact total value of all active bounties          |
| `%tradable_bounty_total_value_all_full%`  | Full total bounty value                             |
| `%tradable_bounty_total_value_all_raw%`   | Raw total bounty value                              |
| `%tradable_baltop_position%`              | Player’s current baltop rank                        |
| `%tradable_baltop_balance%`               | Player’s baltop balance in compact format           |
| `%tradable_baltop_balance_full%`          | Player’s baltop balance in full format              |
| `%tradable_shop_enabled%`                 | Whether the shop is enabled                         |
| `%tradable_shop_category_count%`          | Number of configured shop categories                |
| `%tradable_viewer_name%`                  | Viewer/player name                                  |


**Dynamic placeholders**
| Placeholder                                    | Description                                   |
| ---------------------------------------------- | --------------------------------------------- |
| `%tradable_player_balance_<name>%`             | Balance of a specific player                  |
| `%tradable_player_balance_compact_<name>%`     | Compact balance of a specific player          |
| `%tradable_player_balance_full_<name>%`        | Full balance of a specific player             |
| `%tradable_player_auction_count_<name>%`       | Auction listing count of a specific player    |
| `%tradable_player_auction_total_value_<name>%` | Auction total value of a specific player      |
| `%tradable_player_bounty_total_<name>%`        | Total bounty value on a specific player       |
| `%tradable_player_bounty_count_<name>%`        | Number of bounty entries on a specific player |
| `%tradable_top_balance_name_1%`                | Name of the player at baltop rank 1           |
| `%tradable_top_balance_value_1%`               | Compact balance of baltop rank 1              |
| `%tradable_top_balance_value_full_1%`          | Full balance of baltop rank 1                 |
| `%tradable_top_balance_uuid_1%`                | UUID of baltop rank 1                         |

---

## 🚀 Perfect For

Tradable is especially well suited for:

- SMP servers
- Survival servers
- Economy servers
- PvP survival worlds
- Community trading servers
- Progression-driven servers
- Long-term public or private worlds

If your server benefits from money, trade, competition, or player interaction, Tradable fits naturally.

---

## 💎 What Makes It Feel Premium
Tradable is built around one simple idea:

economy should feel like gameplay.

Instead of acting like a tiny side utility, it turns your server economy into a connected system where players can:

- earn money
- transfer money
- spend money
- compete for baltop
- hunt for bounty rewards
- sell high-value items
- browse curated shop categories
- interact through polished menus instead of command spam

Everything works together, which makes the server feel more alive and more professional.

---

## 📦 In Short

> Tradable transforms your server economy into a full trading experience.
>
> From quick player payments to full Auction House trading, from shop browsing to bounty competition, Tradable gives your players meaningful ways to earn, spend, trade, and compete — all inside one unified plugin.
>
> If you want your server economy to feel important instead of optional, Tradable is built for that.
>
> Made for servers that want their economy to matter.
