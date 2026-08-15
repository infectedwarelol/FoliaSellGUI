package net.mackenziemolloy.shopguiplus.sellgui.shop;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import net.brcdev.shopgui.ShopGuiPlusApi;
import net.brcdev.shopgui.ShopGuiPlugin;
import net.brcdev.shopgui.economy.EconomyManager;
import net.brcdev.shopgui.economy.EconomyType;
import net.brcdev.shopgui.provider.economy.EconomyProvider;
import net.brcdev.shopgui.shop.Shop;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ShopService {

    private static final int PROVIDER_CACHE_LIMIT = 256;

    private final Logger logger;
    private final Map<Shop, EconomyProvider> providerCache;

    public ShopService(@NotNull Logger logger) {
        this.logger = logger;
        this.providerCache = new ConcurrentHashMap<>();
    }

    public @NotNull PricingContext newContext(@NotNull Player player, int budget) {
        return new PricingContext(player, this.logger, budget);
    }

    public @Nullable EconomyProvider provider(@Nullable Shop shop) {
        if (shop != null) {
            EconomyProvider cached = this.providerCache.get(shop);
            if (cached != null) {
                return cached;
            }
        }

        EconomyProvider resolved = resolveProvider(shop);
        if (shop != null && resolved != null) {
            if (this.providerCache.size() >= PROVIDER_CACHE_LIMIT) {
                this.providerCache.clear();
            }

            this.providerCache.put(shop, resolved);
        }

        return resolved;
    }

    public boolean isPlayerDataReady(@NotNull Player player) {
        try {
            ShopGuiPlugin shopGui = ShopGuiPlusApi.getPlugin();
            if (shopGui == null) {
                return false;
            }

            return shopGui.getPlayerManager().isPlayerLoaded(player);
        } catch (Throwable ignored) {
            return true;
        }
    }

    public void invalidate() {
        this.providerCache.clear();
    }

    private @Nullable EconomyProvider resolveProvider(@Nullable Shop shop) {
        EconomyManager economyManager = economyManager();
        EconomyProvider provider = null;

        if (shop != null) {
            try {
                provider = shop.getEconomyProvider();
            } catch (Throwable ignored) {
                provider = null;
            }

            if (provider == null && economyManager != null) {
                EconomyType economyType = shop.getEconomyType();
                if (economyType != null) {
                    try {
                        provider = economyManager.getEconomyProvider(economyType);
                    } catch (Throwable ignored) {
                        provider = null;
                    }
                }
            }
        }

        if (provider == null && economyManager != null) {
            try {
                provider = economyManager.getDefaultEconomyProvider();
            } catch (Throwable ignored) {
                provider = null;
            }
        }

        return provider;
    }

    private @Nullable EconomyManager economyManager() {
        try {
            ShopGuiPlugin shopGui = ShopGuiPlusApi.getPlugin();
            return (shopGui == null ? null : shopGui.getEconomyManager());
        } catch (Throwable ignored) {
            return null;
        }
    }
}
