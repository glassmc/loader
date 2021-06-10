package ml.glassmc.loader;

import java.util.Map;

public interface Reference {

    Map<Class<?>, Class<? extends Hook>> getHooks();

}
