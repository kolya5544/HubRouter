package ax.nk.hubrouter;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.util.*;

public final class MenuService {

    private static final LegacyComponentSerializer AMP = LegacyComponentSerializer.legacyAmpersand();

    private final JavaPlugin plugin;
    private final HubConfig cfg;
    private final ServerStatusService statusService;
    private final PlayerStateService playerState;

    public MenuService(JavaPlugin plugin, HubConfig cfg, ServerStatusService statusService, PlayerStateService playerState) {
        this.plugin = plugin;
        this.cfg = cfg;
        this.statusService = statusService;
        this.playerState = playerState;
    }

    public void open(Player player, int page) {
        int totalPages = getTotalPages();
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        SelectorMenuHolder holder = new SelectorMenuHolder(safePage);
        // 5 rows (45 slots) so there isn't an extra bottom row.
        Inventory inv = Bukkit.createInventory(holder, 45, Component.text(cfg.menuTitle()));

        render(inv, holder, safePage, totalPages);
        playerState.set(player.getUniqueId(), PlayerStateService.State.VIEWING);
        player.openInventory(inv);
    }

    public boolean isSelectorInventory(Inventory inv) {
        return inv.getHolder() instanceof SelectorMenuHolder;
    }

    public void refreshIfOpen(Player player) {
        if (!(player.getOpenInventory().getTopInventory().getHolder() instanceof SelectorMenuHolder holder)) return;

        int totalPages = getTotalPages();
        int safePage = Math.max(0, Math.min(holder.page(), totalPages - 1));

        Inventory inv = player.getOpenInventory().getTopInventory();
        render(inv, holder, safePage, totalPages);

        player.updateInventory(); // helps client-side consistency
    }

    public void handleServerClick(Player player, String velocityServerName) {
        // Ignore spam clicks while we're already connecting
        if (playerState.get(player.getUniqueId()) == PlayerStateService.State.CONNECTING) {
            player.sendActionBar(Component.text("Already connecting..."));
            return;
        }

        ServerStatusService.Status st = statusService.getStatus(velocityServerName);
        if (!st.online()) {
            player.sendMessage(Component.text("Server is offline."));
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1.0f, 0.7f);
            return;
        }

        playerState.set(player.getUniqueId(), PlayerStateService.State.CONNECTING);
        player.sendActionBar(Component.text("Connecting..."));
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);

        if (!sendConnect(player, velocityServerName)) {
            player.sendMessage(Component.text("Unable to send connect request to proxy."));
            playerState.set(player.getUniqueId(), PlayerStateService.State.VIEWING);
            return;
        }

        // Close immediately for UX (feels like “action happened”)
        player.closeInventory();

        // If still on this hub after ~2 seconds, connection didn’t happen → reopen menu
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) return; // if they transferred away, they won't be online here

            // still on hub -> show menu again
            playerState.set(player.getUniqueId(), PlayerStateService.State.VIEWING);
            open(player, 0); // or keep last page if you implemented that
            player.sendMessage(Component.text("Could not connect. Is the server name correct in Velocity?"));
        }, 40L);
    }

    public void handleNav(Player player, int newPage) {
        open(player, newPage);
        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
    }

    public int getTotalPages() {
        int n = cfg.servers().size();
        int per = cfg.serversPerPage();
        return Math.max(1, (n + per - 1) / per);
    }

    private void render(Inventory inv, SelectorMenuHolder holder, int safePage, int totalPages) {
        // Fill with gray
        ItemStack filler = grayFiller();
        for (int i = 0; i < inv.getSize(); i++) inv.setItem(i, filler);

        // Page slice
        List<ServerEntry> servers = cfg.servers();
        int start = safePage * cfg.serversPerPage();
        int end = Math.min(start + cfg.serversPerPage(), servers.size());
        List<ServerEntry> pageServers = servers.subList(start, end);

        // IMPORTANT: clear old mappings, otherwise stale slots may remain clickable
        holder.slotToServer().clear();
        holder.navSlots().clear();
        holder.refreshSlots().clear();
        holder.disconnectSlots().clear();

        // With a 5-row inventory, we render server tiles only in rows 0..2 (3 rows).
        // That leaves row 4 for controls, and row 3 as breathing room (still filled with gray panes).
        int[][] anchors = new int[][]{
                {0, 0}, {2, 0}, {4, 0}, {6, 0}
        };

        for (int i = 0; i < pageServers.size() && i < anchors.length; i++) {
            ServerEntry entry = pageServers.get(i);
            renderTile(inv, holder, anchors[i][0], anchors[i][1], entry);
        }

        renderControls(inv, holder, safePage, totalPages);
    }

    private void renderTile(Inventory inv, SelectorMenuHolder holder, int x, int y, ServerEntry entry) {
        // 3x3 tile:
        // row0: gray gray gray
        // row1: gray ICON gray
        // row2: gray STATUS gray

        int iconSlot = (y + 1) * 9 + (x + 1);
        ItemStack icon = iconItem(entry);
        inv.setItem(iconSlot, icon);
        holder.slotToServer().put(iconSlot, entry.velocityName());

        int statusSlot = (y + 2) * 9 + (x + 1);
        ItemStack status = statusItem(entry.velocityName());
        inv.setItem(statusSlot, status);
        holder.slotToServer().put(statusSlot, entry.velocityName());
    }


    private void renderControls(Inventory inv, SelectorMenuHolder holder, int page, int totalPages) {
        // Controls row is now the very bottom row of a 5-row inventory: slots 36..44.
        // Keep it symmetrical:
        // 36 prev, 39 disconnect, 40 page, 41 refresh, 44 next
        int prev = 36;
        int disconnect = 39;
        int pageInfo = 40;
        int refresh = 41;
        int next = 44;

        if (page > 0) {
            ItemStack prevItem = new ItemStack(Material.ARROW);
            ItemMeta m = prevItem.getItemMeta();
            m.displayName(Component.text("← Previous"));
            m.lore(List.of(Component.text("Page " + page + " / " + totalPages)));
            prevItem.setItemMeta(m);

            inv.setItem(prev, prevItem);
            holder.navSlots().put(prev, page - 1);
        } else {
            inv.setItem(prev, named(Material.GRAY_DYE, Component.text(" ")));
        }

        inv.setItem(disconnect, named(Material.RED_DYE, AMP.deserialize("&c&lDISCONNECT")));
        holder.disconnectSlots().add(disconnect);

        inv.setItem(pageInfo, named(Material.PAPER, Component.text("Page " + (page + 1) + " / " + totalPages)));

        if (page < totalPages - 1) {
            ItemStack nextItem = new ItemStack(Material.ARROW);
            ItemMeta m = nextItem.getItemMeta();
            m.displayName(Component.text("Next →"));
            m.lore(List.of(Component.text("Page " + (page + 2) + " / " + totalPages)));
            nextItem.setItemMeta(m);

            inv.setItem(next, nextItem);
            holder.navSlots().put(next, page + 1);
        } else {
            inv.setItem(next, named(Material.GRAY_DYE, Component.text(" ")));
        }

        inv.setItem(refresh, named(Material.CLOCK, Component.text("Refresh")));
        holder.refreshSlots().add(refresh);
    }

    private ItemStack grayFiller() {
        ItemStack it = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(" "));
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack iconItem(ServerEntry entry) {
        Material mat = materialOr(Material.STONE, entry.iconMaterial());
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();

        meta.displayName(AMP.deserialize(entry.displayName()));

        List<Component> lore = new ArrayList<>();
        for (String line : entry.description()) lore.add(AMP.deserialize(line));

        // Include status line at end
        ServerStatusService.Status st = statusService.getStatus(entry.velocityName());

        lore.add(Component.text(" "));
        if (st.online()) {
            lore.add(AMP.deserialize("&a&lCLICK TO JOIN NOW!"));
            lore.add(AMP.deserialize("&7Status: &aOnline"));
        } else {
            lore.add(AMP.deserialize("&c&lOFFLINE"));
            lore.add(AMP.deserialize("&7Status: &cOffline"));
            lore.add(AMP.deserialize("&7Try again later."));
        }

        meta.lore(lore);
        it.setItemMeta(meta);
        return it;
    }

    private ItemStack statusItem(String velocityName) {
        ServerStatusService.Status st = statusService.getStatus(velocityName);

        Material mat = st.online() ? Material.LIME_WOOL : Material.RED_WOOL;
        ItemStack it = new ItemStack(mat);

        ItemMeta meta = it.getItemMeta();
        if (st.online()) {
            meta.displayName(AMP.deserialize("&aOnline"));
            meta.lore(List.of(
                    AMP.deserialize("&a&lCLICK TO JOIN NOW!")
            ));
        } else {
            meta.displayName(AMP.deserialize("&cOffline"));
            meta.lore(List.of(
                    AMP.deserialize("&c&lOFFLINE"),
                    AMP.deserialize("&7Come back later.")
            ));
        }

        it.setItemMeta(meta);
        return it;
    }


    private ItemStack named(Material mat, Component name) {
        ItemStack it = new ItemStack(mat);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(name);
        it.setItemMeta(meta);
        return it;
    }

    private Material materialOr(Material fallback, String name) {
        try {
            Material m = Material.matchMaterial(name);
            return m != null ? m : fallback;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private boolean sendConnect(Player player, String serverName) {
        byte[] payload;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(baos);
            out.writeUTF("Connect");
            out.writeUTF(serverName);
            payload = baos.toByteArray();
        } catch (Exception e) {
            return false;
        }

        boolean sent = false;

        if (Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, cfg.proxyChannelFallback())) {
            player.sendPluginMessage(plugin, cfg.proxyChannelFallback(), payload);
            sent = true;
        }
        if (Bukkit.getMessenger().isOutgoingChannelRegistered(plugin, cfg.proxyChannelPrimary())) {
            player.sendPluginMessage(plugin, cfg.proxyChannelPrimary(), payload);
            sent = true;
        }

        return sent;
    }

}