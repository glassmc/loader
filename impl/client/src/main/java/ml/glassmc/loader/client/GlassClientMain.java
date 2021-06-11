package ml.glassmc.loader.client;

import java.lang.reflect.Method;

public class GlassClientMain {

    public static void main(String[] args) throws Exception {
        Class<?> mainClass = Class.forName("net.minecraft.client.main.Main");
        Method mainMethod = mainClass.getMethod("main", String[].class);
        mainMethod.invoke(null, (Object) args);
    }

}
