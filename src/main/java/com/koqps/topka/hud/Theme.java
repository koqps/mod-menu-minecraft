package com.koqps.topka.hud;

import com.koqps.topka.TopkaClient;

public final class Theme {
    private Theme() { }
    public static int accent() { return TopkaClient.CONFIG.get().accentArgb; }
}
