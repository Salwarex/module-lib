package ru.vit4liy.modular;

import ru.vit4liy.modular.exception.ModuleInitializeException;
import ru.vit4liy.modular.exception.ModuleShutdownException;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class ModuleLoader {
    protected final Map<Class<? extends Module>, Module> modules = new HashMap<>();

    public ModuleLoader(boolean pluginsIncluded){
        this.loadStandardModules();
        if(pluginsIncluded) this.loadPlugins();
    }

    protected abstract void loadStandardModules() ;
    protected void loadPlugins(){
        Path pluginsDir = Paths.get("plugins");

        ModuleFinder pluginsFinder = ModuleFinder.of(pluginsDir);

        List<String> plugins = pluginsFinder
                .findAll()
                .stream()
                .map(ModuleReference::descriptor)
                .map(ModuleDescriptor::name)
                .collect(Collectors.toList());

        Configuration pluginsConfiguration = ModuleLayer
                .boot()
                .configuration()
                .resolve(pluginsFinder, ModuleFinder.of(), plugins);

        ModuleLayer layer = ModuleLayer
                .boot()
                .defineModulesWithOneLoader(pluginsConfiguration, ClassLoader.getSystemClassLoader());

        List<Plugin> services = Plugin.getServices(layer);
        for (Plugin plugin : services) {
            System.out.printf("Found plugin %s", plugin.pluginName());
            modules.put(plugin.getClass(), plugin);
        }
    }

    public <T extends Module> T getModule(Class<T> moduleClass) throws ClassNotFoundException{
        if(!modules.containsKey(moduleClass)) throw new ClassNotFoundException("Module %s not loaded!".formatted(moduleClass.getSimpleName()));
        return moduleClass.cast(modules.get(moduleClass));
    }

    public void initModules() throws ModuleInitializeException {
        for (Class<? extends Module> clazz : modules.keySet()) {
            initModule(clazz);
        }
    }

    public void initModule(Class<? extends Module> moduleClass) throws ModuleInitializeException{
        if(!modules.containsKey(moduleClass)) throw new ModuleInitializeException("Module %s not loaded!".formatted(moduleClass.getSimpleName()));
        Module module = modules.get(moduleClass);
        System.out.printf("Loading module %s...%n", module.moduleName());
        module.initialize();
        System.out.printf("Module %s successfully loaded!%n", module.moduleName());
    }

    public void stopModule(Class<? extends Module> moduleClass) throws ModuleShutdownException{
        if(!modules.containsKey(moduleClass)) throw new ModuleShutdownException("Module %s not loaded!".formatted(moduleClass.getSimpleName()));
        Module module = modules.get(moduleClass);
        System.out.printf("Stopping module %s...%n", module.moduleName());
        module.shutdown();
        System.out.printf("Module %s successfully stopped!%n", module.moduleName());
    }


    public void stopModules() throws ModuleShutdownException {
        for (Class<? extends Module> clazz : modules.keySet()) {
            stopModule(clazz);
        }
    }
}
