package com.ragemod;

/** Hem istemci hem sunucunun gorebildigi kucuk paylasilan bilgi. */
public final class RageShared {
    /** Istemcideki (senin) oyuncunun rage etabi. Sunucudan gelir. */
    public static volatile int clientStage = 0;

    private RageShared() {}
}
