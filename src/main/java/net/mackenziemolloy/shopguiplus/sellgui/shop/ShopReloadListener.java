package net.mackenziemolloy.shopguiplus.sellgui.shop;

import net.brcdev.shopgui.event.ShopsPostLoadEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public final class ShopReloadListener implements Listener {

    private final ShopService shopService;

    public ShopReloadListener(@NotNull ShopService shopService) {
        this.shopService = shopService;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onShopsPostLoad(@NotNull ShopsPostLoadEvent event) {
        this.shopService.invalidate();
    }
}
