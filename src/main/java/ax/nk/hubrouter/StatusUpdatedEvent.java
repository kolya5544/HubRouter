package ax.nk.hubrouter;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public final class StatusUpdatedEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}