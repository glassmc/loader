package com.github.glassmc.loader.impl.exception;

public class NoSuchListenerException extends RuntimeException {

    public NoSuchListenerException(String listenerClass, String shardInfoPath) {
        super(String.format("Listener %s not found, used in %s", listenerClass, shardInfoPath));
    }

}
