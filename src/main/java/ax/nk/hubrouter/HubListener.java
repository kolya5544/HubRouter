package ax.nk.hubrouter;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class HubListener implements Listener {

    private final JavaPlugin plugin;
    private final HubConfig cfg;
    private final MenuService menus;
    private final PlayerStateService playerState;

    public HubListener(JavaPlugin plugin, HubConfig cfg, MenuService menus, PlayerStateService playerState) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.menus = menus;
        this.playerState = playerState;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        // Force Adventure and teleport to platform
        World w = Bukkit.getWorld(cfg.hubWorld());
        if (w != null) {
            Location loc = new Location(w, cfg.hubX(), cfg.hubY(), cfg.hubZ(), cfg.hubYaw(), cfg.hubPitch());
            p.teleport(loc);
            p.setRespawnLocation(loc, true);
        }

        p.setGameMode(GameMode.ADVENTURE);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            menus.open(p, 0);
        }, cfg.openMenuDelayTicks());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        playerState.clear(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onStatusUpdated(StatusUpdatedEvent e) {
        // Refresh menus for viewers
        for (Player p : Bukkit.getOnlinePlayers()) {
            menus.refreshIfOpen(p);
        }
    }

    @EventHandler
    public void onInvClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!menus.isSelectorInventory(e.getView().getTopInventory())) return;

        e.setCancelled(true);

        SelectorMenuHolder holder = (SelectorMenuHolder) e.getView().getTopInventory().getHolder();
        int slot = e.getRawSlot();
        if (slot < 0 || slot >= e.getView().getTopInventory().getSize()) return;

        // Nav
        Integer newPage = holder.navSlots().get(slot);
        if (newPage != null) {
            menus.handleNav(p, newPage);
            return;
        }

        // Refresh
        if (holder.refreshSlots().contains(slot)) {
            menus.open(p, holder.page());
            return;
        }

        // Server click
        String server = holder.slotToServer().get(slot);
        if (server != null) {
            menus.handleServerClick(p, server);
        }
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        if (!menus.isSelectorInventory(e.getInventory())) return;

        // If player is connecting, don't reopen immediately
        if (playerState.get(p.getUniqueId()) == PlayerStateService.State.CONNECTING) return;

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            int page = 0;
            if (e.getInventory().getHolder() instanceof SelectorMenuHolder h) {
                page = h.page();
            }
            menus.open(p, page);
        }, cfg.reopenMenuDelayTicks());
    }
}