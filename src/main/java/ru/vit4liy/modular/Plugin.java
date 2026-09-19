package ru.vit4liy.modular;

import ru.vit4liy.modular.exception.ModuleInitializeException;
import ru.vit4liy.modular.exception.ModuleShutdownException;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class Plugin implements Module {
    @Override
    public void initialize() throws ModuleInitializeException {
        System.out.printf("Start loading plugin %s...", pluginName());
        onStart();
        System.out.printf("Plugin %s successfully loaded!", pluginName());
    }

    public abstract void onStart();

    @Override
    public void shutdown() throws ModuleShutdownException {
        System.out.printf("Stopping plugin %s...%n", pluginName());
        onStop();
        System.out.printf("Plugin %s successfully stopped!%n", pluginName());
    }

    public abstract void onStop();

    @Override
    public String moduleName() {
        return "%s-%s.plugin".formatted(pluginName(), version());
    }

    public abstract String pluginName();
    public abstract String version();

    static List<Plugin> getServices(ModuleLayer layer) {
        return ServiceLoader
                .load(layer, Plugin.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .collect(Collectors.toList());
    }
}
