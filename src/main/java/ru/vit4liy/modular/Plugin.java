package ru.vit4liy.modular;

import ru.vit4liy.modular.exception.ModuleInitializeException;
import ru.vit4liy.modular.exception.ModuleShutdownException;

import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public abstract class Plugin extends Module {
    public Plugin(ModuleLoader loader) {
        super(loader);
    }

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
        return "%s-v%d.%d.%d.plugin".formatted(pluginName(), release(), version(), build());
    }

    public abstract String pluginName();
    public abstract int release();
    public abstract int version();
    public abstract int build();

    static List<Plugin> getServices(ModuleLayer layer, ModuleLoader loader) {
        return ServiceLoader.load(layer, Plugin.class)
                .stream()
                .map(provider -> {
                    try {
                        return provider.type().getDeclaredConstructor(ModuleLoader.class).newInstance(loader);
                    } catch (NoSuchMethodException e) {
                        throw new RuntimeException("Плагин " + provider.type().getName() +
                                " должен иметь публичный конструктор, принимающий ModuleLoader", e);
                    } catch (Exception e) {
                        throw new RuntimeException("Не удалось создать экземпляр плагина " + provider.type().getName(), e);
                    }
                })
                .collect(Collectors.toList());
    }
}
