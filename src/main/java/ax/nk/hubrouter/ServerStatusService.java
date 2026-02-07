package ax.nk.hubrouter;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ServerStatusService {

    public record Status(boolean online, long lastCheckedMillis) {}

    private final JavaPlugin plugin;
    private final HubConfig cfg;
    private final Map<String, Status> statusMap = new ConcurrentHashMap<>();

    private BukkitTask task;

    public ServerStatusService(JavaPlugin plugin, HubConfig cfg) {
        this.plugin = plugin;
        this.cfg = cfg;

        // Default statuses
        long now = System.currentTimeMillis();
        for (ServerEntry e : cfg.servers()) {
            statusMap.put(e.velocityName(), new Status(false, now));
        }
    }

    public Status getStatus(String velocityName) {
        return statusMap.getOrDefault(velocityName, new Status(false, 0));
    }

    public void start() {
        stop();
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::pollAll, 0L, cfg.pingIntervalTicks());
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void pollAll() {
        long now = System.currentTimeMillis();

        for (ServerEntry entry : cfg.servers()) {
            boolean online = canConnect(entry.pingHost(), entry.pingPort(), cfg.pingTimeoutMs());
            statusMap.put(entry.velocityName(), new Status(online, now));
        }

        // Ask menus to refresh on main thread (cheap; rebuild only for viewers)
        Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getPluginManager().callEvent(new StatusUpdatedEvent()));
    }

    private boolean canConnect(String host, int port, int timeoutMs) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), timeoutMs);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}