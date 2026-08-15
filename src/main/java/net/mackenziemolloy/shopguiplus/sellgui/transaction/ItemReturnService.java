package net.mackenziemolloy.shopguiplus.sellgui.transaction;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.mackenziemolloy.shopguiplus.sellgui.compat.Schedulers;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;

public final class ItemReturnService {

    private final Schedulers schedulers;
    private final Logger logger;

    public ItemReturnService(@NotNull Schedulers schedulers, @NotNull Logger logger) {
        this.schedulers = schedulers;
        this.logger = logger;
    }

    public boolean returnItems(@NotNull Player player, @NotNull List<ItemStack> items, boolean forceDrop) {
        if (items.isEmpty()) {
            return false;
        }

        boolean dropped = false;
        PlayerInventory inventory = player.getInventory();
        Iterator<ItemStack> iterator = items.iterator();

        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                iterator.remove();
                continue;
            }

            if (forceDrop) {
                drop(player, item);
                iterator.remove();
                dropped = true;
                continue;
            }

            Map<Integer, ItemStack> overflow;
            try {
                overflow = inventory.addItem(item);
            } catch (Throwable throwable) {
                this.logger.log(Level.WARNING, "Failed to return an item to " + player.getName()
                        + ", dropping it instead.", throwable);
                drop(player, item);
                iterator.remove();
                dropped = true;
                continue;
            }

            iterator.remove();

            for (ItemStack leftover : overflow.values()) {
                drop(player, leftover);
                dropped = true;
            }
        }

        return dropped;
    }

    private void drop(@NotNull Player player, @NotNull ItemStack item) {
        if (item.getType().isAir() || item.getAmount() <= 0) {
            return;
        }

        Location location = player.getLocation().add(0.0D, 0.5D, 0.0D);
        ItemStack copy = item.clone();

        this.schedulers.onLocation(location, () -> {
            try {
                World world = location.getWorld();
                if (world != null) {
                    world.dropItemNaturally(location, copy);
                }
            } catch (Throwable throwable) {
                this.logger.log(Level.SEVERE, "Failed to drop " + copy.getType() + " x" + copy.getAmount()
                        + " for " + player.getName() + ".", throwable);
            }
        });
    }
}
