![](https://i.imgur.com/kKx5gUu.png)

An addon for [ShopGUIPlus](https://www.spigotmc.org/resources/shopgui-1-7-1-16.6515/) that enables players to easily sell items in seconds, without having to go searching through the shops for them.

This is a fork of the original plugin, rebuilt for **Minecraft 1.21.11** with native Paper / Folia / CanvasMC support and ShopGUIPlus-powered shulker box selling.

![](https://i.imgur.com/P6mPXDK.gif)

![](https://i.imgur.com/h7Ruzj8.png)
Supports all currencies supported by ShopGUIPlus

Supports all hooks (HeadDatabase, etc) supported by ShopGUIPlus

Requires Minecraft 1.21.11 (Paper, Folia or CanvasMC) and Java 21

Fully customisable messages

Action bar, and title messages

(Cool lookin') receipts

Sells shulker boxes by their contents, using the prices ShopGUIPlus already has configured

![](https://i.imgur.com/NZ9sc2w.png)

The permissions are as follows;

sellgui.use grants the user access to use the SellGUI

sellgui.dump grants the user access to the dump debug command (sellgui.debug also works)

sellgui.reload grants the user access to reload the configuration

![](https://i.imgur.com/u2IFwlB.png)![](https://i.imgur.com/mXnsSK9.png)![](https://i.imgur.com/SK5ulCR.png)

## Shulker box selling

Drop a shulker box into `/sellgui` and it is valued as a single unit:

- Every colour of shulker box is supported, including the undyed one.
- The contents are read straight off the item, so nothing is ever unpacked into the world or into the player's inventory.
- Each distinct item inside is priced with `ShopItem#getSellPriceForAmount`, so ShopGUIPlus multipliers, per-player price modifiers, per-item permissions and per-shop currencies all apply exactly as they do in the shop itself.
- The empty box's own shop price is added on top when the box is consumed.
- Items ShopGUIPlus cannot buy are handled by `options.shulker.unsellable_contents`:
  - `KEEP_ITEMS` (default) pays for what is sellable and hands the box back holding only the leftovers.
  - `RETURN_BOX` refuses the sale and returns the box untouched.
  - `DISCARD` consumes the box and everything left inside it.
- Nested boxes are followed up to `options.shulker.max_depth`, and `options.shulker.max_scanned_items` caps how much a single sale may inspect.
- Players are shown an estimate as soon as they place a box into the GUI.

## Transaction safety

The sell flow is a single atomic step that runs on the region thread that owns the player:

1. The GUI slots are read from the server-side inventory and cleared in the same pass.
2. Prices are resolved from the removed copies, never from client-supplied state.
3. The economy is paid. If any deposit fails, prior deposits are withdrawn again and every item is returned.
4. Anything unsellable is returned, dropping at the player's feet only when the inventory is full.

Clicks, drags, shift-clicks, hotbar swaps, offhand swaps and double-click collection are all validated against the GUI's decoration slots, and a settled GUI rejects further interaction. Disconnects, deaths, teleports, `/sellgui` being reopened and plugin shutdown all route through the same guarded path.

## Information
- [Installation](https://github.com/MackenzieMolloy/ShopGUIPlus-SellGUI/wiki/Installation)
- [Configuration](https://github.com/MackenzieMolloy/ShopGUIPlus-SellGUI/wiki/Configuration)
- [Getting Support](https://github.com/MackenzieMolloy/ShopGUIPlus-SellGUI/wiki/Getting-Support)
