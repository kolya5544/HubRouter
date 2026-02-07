package ax.nk.hubrouter;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ServerCommand implements CommandExecutor {

    private final MenuService menus;

    public ServerCommand(MenuService menus) {
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Players only.");
            return true;
        }
        menus.open(p, 0);
        return true;
    }
}