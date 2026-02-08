package ax.nk.hubrouter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerStateService {

    public enum State { VIEWING, CONNECTING, DISCONNECTING }

    private final Map<UUID, State> state = new ConcurrentHashMap<>();

    public State get(UUID uuid) {
        return state.getOrDefault(uuid, State.VIEWING);
    }

    public void set(UUID uuid, State s) {
        state.put(uuid, s);
    }

    public void clear(UUID uuid) {
        state.remove(uuid);
    }
}