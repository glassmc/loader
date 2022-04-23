package com.github.glassmc.loader.api;

/**
 * A listener which is registered to a hook in a <b>info.toml</b> file to be invoked when the hook is run.
 */
public interface Listener {
    void run();
}
