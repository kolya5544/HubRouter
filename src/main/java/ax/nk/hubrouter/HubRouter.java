package ax.nk.hubrouter;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class HubRouter extends JavaPlugin {
    private HubConfig hubConfig;
    private ServerStatusService statusService;
    private MenuService menuService;
    private PlayerStateService playerStateService;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        this.hubConfig = HubConfig.load(getConfig(), getLogger());
        this.playerStateService = new PlayerStateService();
        this.statusService = new ServerStatusService(this, hubConfig);
        this.menuService = new MenuService(this, hubConfig, statusService, playerStateService);

        // Register plugin messaging channels for proxy connect.
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, hubConfig.proxyChannelPrimary());
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, hubConfig.proxyChannelFallback());

        // Start ping scheduler
        statusService.start();

        // Events + command
        Bukkit.getPluginManager().registerEvents(new HubListener(this, hubConfig, menuService, playerStateService), this);
        getCommand("server").setExecutor(new ServerCommand(menuService));

        getLogger().info("IKTeamHubRouter enabled.");
    }

    @Override
    public void onDisable() {
        if (statusService != null) statusService.stop();
        getLogger().info("IKTeamHubRouter disabled.");
    }
}
