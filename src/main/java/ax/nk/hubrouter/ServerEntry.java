package ax.nk.hubrouter;

import java.util.List;

public record ServerEntry(
        String velocityName,
        String displayName,
        List<String> description,
        String iconMaterial,
        String pingHost,
        int pingPort
) { }