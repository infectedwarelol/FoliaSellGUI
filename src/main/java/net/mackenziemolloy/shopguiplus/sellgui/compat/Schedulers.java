package net.mackenziemolloy.shopguiplus.sellgui.compat;

import java.util.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public final class Schedulers {

    private final Plugin plugin;

    public Schedulers(@NotNull Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public boolean ownsEntity(@NotNull Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    public boolean ownsLocation(@NotNull Location location) {
        return Bukkit.isOwnedByCurrentRegion(location);
    }

    public void onEntity(@NotNull Entity entity, @NotNull Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            task.run();
            return;
        }

        entity.getScheduler().run(this.plugin, ignored -> task.run(), null);
    }

    public void onEntityNextTick(@NotNull Entity entity, @NotNull Runnable task) {
        entity.getScheduler().run(this.plugin, ignored -> task.run(), null);
    }

    public void onLocation(@NotNull Location location, @NotNull Runnable task) {
        if (Bukkit.isOwnedByCurrentRegion(location)) {
            task.run();
            return;
        }

        Bukkit.getRegionScheduler().execute(this.plugin, location, task);
    }

    public void global(@NotNull Runnable task) {
        Bukkit.getGlobalRegionScheduler().execute(this.plugin, task);
    }

    public void async(@NotNull Runnable task) {
        Bukkit.getAsyncScheduler().runNow(this.plugin, ignored -> task.run());
    }
}
