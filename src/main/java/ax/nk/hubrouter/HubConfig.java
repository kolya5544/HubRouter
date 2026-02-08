package ax.nk.hubrouter;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;
import java.util.logging.Logger;

public record HubConfig(
        String hubWorld,
        double hubX, double hubY, double hubZ,
        float hubYaw, float hubPitch,
        int openMenuDelayTicks,
        int reopenMenuDelayTicks,
        int pingIntervalTicks,
        int pingTimeoutMs,
        String proxyChannelPrimary,
        String proxyChannelFallback,
        String menuTitle,
        int serversPerPage,
        List<ServerEntry> servers
) {

    public static HubConfig load(FileConfiguration cfg, Logger logger) {
        String hubWorld = cfg.getString("hubWorld", "world");

        double hubX = cfg.getDouble("hubX", 0.5);
        double hubY = cfg.getDouble("hubY", 199.0);
        double hubZ = cfg.getDouble("hubZ", 0.5);
        float hubYaw = (float) cfg.getDouble("hubYaw", 0.0);
        float hubPitch = (float) cfg.getDouble("hubPitch", 0.0);

        int openMenuDelayTicks = cfg.getInt("openMenuDelayTicks", 2);
        int reopenMenuDelayTicks = cfg.getInt("reopenMenuDelayTicks", 10);

        int pingIntervalTicks = cfg.getInt("pingIntervalTicks", 100);
        int pingTimeoutMs = cfg.getInt("pingTimeoutMs", 800);

        String proxyChannelPrimary = cfg.getString("proxyChannelPrimary", "bungeecord:main");
        String proxyChannelFallback = cfg.getString("proxyChannelFallback", "BungeeCord");

        String menuTitle = cfg.getString("menuTitle", "Select a server");
        int serversPerPage = Math.max(1, cfg.getInt("serversPerPage", 3));

        List<ServerEntry> servers = new ArrayList<>();
        ConfigurationSection serversSec = cfg.getConfigurationSection("servers");
        if (serversSec == null) {
            logger.warning("No 'servers' section found in config.yml.");
        } else {
            for (String key : serversSec.getKeys(false)) {
                ConfigurationSection s = serversSec.getConfigurationSection(key);
                if (s == null) continue;

                String displayName = s.getString("displayName", key);
                List<String> description = s.getStringList("description");
                String icon = s.getString("icon", "STONE");
                String pingHost = s.getString("pingHost", "127.0.0.1");
                int pingPort = s.getInt("pingPort", 25565);

                servers.add(new ServerEntry(
                        key,
                        displayName,
                        description,
                        icon,
                        pingHost,
                        pingPort
                ));
            }
        }

        return new HubConfig(
                hubWorld,
                hubX, hubY, hubZ,
                hubYaw, hubPitch,
                openMenuDelayTicks,
                reopenMenuDelayTicks,
                pingIntervalTicks,
                pingTimeoutMs,
                proxyChannelPrimary,
                proxyChannelFallback,
                menuTitle,
                serversPerPage,
                servers
        );
    }
}