package net.mackenziemolloy.shopguiplus.sellgui.shop;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.shop.item.ShopItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PricingContext {

    private final Player player;
    private final Logger logger;
    private final Map<ItemStack, ShopItem> shopItems;
    private final int budget;

    private int scanned;
    private boolean lookupFailureLogged;

    PricingContext(@NotNull Player player, @NotNull Logger logger, int budget) {
        this.player = player;
        this.logger = logger;
        this.shopItems = new HashMap<>();
        this.budget = budget;
    }

    public @NotNull Player player() {
        return this.player;
    }

    public boolean consume(int units) {
        if (units <= 0) {
            return true;
        }

        if (this.scanned > (this.budget - units)) {
            this.scanned = this.budget;
            return false;
        }

        this.scanned += units;
        return true;
    }

    public boolean exhausted() {
        return this.scanned >= this.budget;
    }

    public @Nullable ShopItem shopItem(@NotNull ItemStack singleItem) {
        ShopItem cached = this.shopItems.get(singleItem);
        if (cached != null || this.shopItems.containsKey(singleItem)) {
            return cached;
        }

        ShopItem resolved;
        try {
            resolved = ShopGuiPlusApi.getItemStackShopItem(this.player, singleItem);
        } catch (Throwable throwable) {
            resolved = null;
            logLookupFailure(throwable);
        }

        this.shopItems.put(singleItem, resolved);
        return resolved;
    }

    public double sellPrice(@NotNull ShopItem shopItem, int amount) {
        if (amount <= 0) {
            return 0.0D;
        }

        try {
            double price = shopItem.getSellPriceForAmount(this.player, amount);
            if (Double.isNaN(price) || Double.isInfinite(price) || price <= 0.0D) {
                return 0.0D;
            }

            return price;
        } catch (Throwable throwable) {
            logLookupFailure(throwable);
            return 0.0D;
        }
    }

    private void logLookupFailure(@NotNull Throwable throwable) {
        if (this.lookupFailureLogged) {
            return;
        }

        this.lookupFailureLogged = true;
        this.logger.log(Level.WARNING, "ShopGUIPlus failed to price an item for " + this.player.getName()
                + ". The affected items were returned untouched.", throwable);
    }
}
