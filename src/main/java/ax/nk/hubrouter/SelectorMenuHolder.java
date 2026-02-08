package ax.nk.hubrouter;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.*;

public final class SelectorMenuHolder implements InventoryHolder {

    private final int page;
    private final Map<Integer, String> slotToServer = new HashMap<>();
    private final Map<Integer, Integer> navSlots = new HashMap<>();
    private final Set<Integer> refreshSlots = new HashSet<>();
    private final Set<Integer> disconnectSlots = new HashSet<>();

    public SelectorMenuHolder(int page) {
        this.page = page;
    }

    public int page() {
        return page;
    }

    public Map<Integer, String> slotToServer() {
        return slotToServer;
    }

    public Map<Integer, Integer> navSlots() {
        return navSlots;
    }

    public Set<Integer> refreshSlots() {
        return refreshSlots;
    }

    public Set<Integer> disconnectSlots() {
        return disconnectSlots;
    }

    @Override
    public Inventory getInventory() {
        return null; // We build inventories via Bukkit.createInventory(holder, ...)
    }
}