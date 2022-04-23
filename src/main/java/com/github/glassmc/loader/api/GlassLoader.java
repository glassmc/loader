package com.github.glassmc.loader.api;

import com.github.glassmc.loader.api.loader.Transformer;
import com.github.glassmc.loader.api.loader.TransformerOrder;
import com.github.glassmc.loader.impl.exception.NoSuchInterfaceException;
import com.github.glassmc.loader.impl.exception.NoSuchShardException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A central hub for the glass context, used for working with its various features.
 */
public interface GlassLoader {

    AtomicReference<GlassLoader> loader = new AtomicReference<>(null);

    /**
     * @return The {@link GlassLoader} instance, which is dependent on the glass platform. The actual implementation is defined in the file <b>glassmc.loader.impl</b>.
     */
    static GlassLoader getInstance() {
        GlassLoader loader = GlassLoader.loader.get();
        if (loader == null) {
            try {
                String className = IOUtils.toString(Objects.requireNonNull(GlassLoader.class.getClassLoader().getResource("glassmc.loader.impl")), StandardCharsets.UTF_8);
                loader = (GlassLoader) Class.forName(className).getConstructor().newInstance();
                GlassLoader.loader.set(loader);
            } catch (IOException | ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return loader;
    }

    /**
     * Runs the listeners attached to the specific hook id.
     * @param hook The hook id.
     */
    void runHooks(String hook);

    /**
     * Allows the creation of essentially singleton classes with easy access from other shards.
     * @param api An instance of the api to register.
     */
    void registerAPI(Object api);

    /**
     * @param apiClass The class of the api, which cannot be a parent class or interface. It must match the class of the api registered.
     * @return The requested api, given there is one present that matches the <b>apiClass</b> requested.
     * @throws NoSuchShardException If the requirements for return are not met.
     */
    <T> T getAPI(Class<T> apiClass);

    /**
     * <p>
     *     Registers a class that implements a core interface, with the main purpose of making it easier to abstract the logic of specific minecraft versions or implementations.
     * </p>
     *
     * <p>
     *     <b>Example:</b> A Renderer class which is implemented both in 1.7.10 and 1.8.9 that can be referenced in a central shard knowing it is separately implemented for each version.
     * </p>
     * @param interfaceClass The specific interface that the implementor implements.
     * @param implementor The class object that implements a specific function for a certain environment.
     * @throws IllegalStateException If <b>interfaceClass</b> is not an interface.
     */
    <T> void registerInterface(Class<T> interfaceClass, T implementor);

    /**
     * @param interfaceClass The interface class which is implemented.
     * @return The implementor of the requested interface class.
     * @throws NoSuchInterfaceException If there is not a registered implementor of the interface class.
     */
    <T> T getInterface(Class<T> interfaceClass);

    /**
     * Registers a transformer with the default ordering.
     * @see GlassLoader#registerTransformer(Class, TransformerOrder)
     */
    default void registerTransformer(Class<? extends Transformer> transformer) {
        this.registerTransformer(transformer, TransformerOrder.DEFAULT);
    }

    /**
     * Registers a transformer to be applied to all future classes loaded through Glass' systems.
     * @param transformer
     * @param order The priority in which to invoke the transformer. All transformers are applied to classes, however higher priority transformers are invoked prior to lower priority transformers.
     */
    void registerTransformer(Class<? extends Transformer> transformer, TransformerOrder order);

    /**
     * @param name The fully qualified class name, using <b>.</b> to separate packages and <b>$</b> for inner classes.
     * @return The raw data for the class.
     * @throws ClassNotFoundException If the class could not be loaded.
     */
    byte[] getClassBytes(String name) throws ClassNotFoundException;

    /**
     * @param id The id of the shard as declared in its <b>info.toml</b>.
     * @return The declared version of the shard matching <b>id</b>.
     */
    String getShardVersion(String id);

}
