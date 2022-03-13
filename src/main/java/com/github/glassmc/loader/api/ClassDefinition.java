package com.github.glassmc.loader.api;

import java.net.URL;

public class ClassDefinition {

    private final URL location;
    private final byte[] data;

    public ClassDefinition(URL location, byte[] data) {
        this.location = location;
        this.data = data;
    }

    public URL getLocation() {
        return location;
    }

    public byte[] getData() {
        return data;
    }

}
