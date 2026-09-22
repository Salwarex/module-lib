package ru.vit4liy.modular;

import ru.vit4liy.modular.exception.ModuleInitializeException;
import ru.vit4liy.modular.exception.ModuleShutdownException;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public abstract class ModuleLoader {
    protected final Map<Class<? extends Module>, Module> modules = new ConcurrentHashMap<>();
    protected final ExecutorService pool;

    public ModuleLoader(ExecutorService pool, boolean pluginsIncluded){
        this.pool = pool;
        this.loadStandardModules();
        if(pluginsIncluded) this.loadPlugins();
    }

    protected abstract void loadStandardModules() ;
    protected void loadPlugins() {
        Path pluginsDir = Paths.get("plugins");
        System.out.println("Search plugins in " + pluginsDir.toAbsolutePath());

        if (Files.notExists(pluginsDir)) {
            System.out.println("Plugins directory not exist!");
            return;
        }

        try (var stream = Files.list(pluginsDir)) {
            List<Path> jars = stream.filter(path -> path.toString().endsWith(".jar"))
                    .collect(Collectors.toList());

            System.out.println("Found JARs: " + jars.size());

            for (Path path : jars) {
                try {
                    System.out.println("Loading: " + path.getFileName());

                    URL jarUrl = path.toUri().toURL();
                    URLClassLoader classLoader = new URLClassLoader(
                            new URL[]{jarUrl},
                            Plugin.class.getClassLoader()
                    );

                    List<Class<? extends Plugin>> pluginClasses = ServiceLoader.load(Plugin.class, classLoader)
                            .stream()
                            .map(ServiceLoader.Provider::type)
                            .collect(Collectors.toList());

                    for (Class<? extends Plugin> pluginClass : pluginClasses) {
                        try {
                            Plugin plugin = pluginClass.getDeclaredConstructor(ModuleLoader.class).newInstance(this);

                            System.out.printf("  ✓ Plugin loaded successfully: %s%n", plugin.pluginName());
                            modules.put(pluginClass, plugin);

                        } catch (NoSuchMethodException e) {
                            System.err.println("  ✗ Error: Class " + pluginClass.getName() + " must have public constructor with ModuleLoader argument");
                        } catch (Exception e) {
                            System.err.println("  ✗ Error during loading plugin " + pluginClass.getName());
                            e.printStackTrace();
                        }
                    }

                } catch (Exception e) {
                    System.err.println("   Critical error load plugin " + path.getFileName());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error read plugins directory", e);
        }

        long pluginCount = modules.values().stream()
                .filter(m -> m instanceof Plugin).count();
        System.out.println("Всего загружено плагинов: " + pluginCount);
    }

    public synchronized  <T extends Module> T getModule(Class<T> moduleClass) throws ClassNotFoundException{
        if(!modules.containsKey(moduleClass)) throw new ClassNotFoundException("Module %s not initialized!".formatted(moduleClass.getSimpleName()));
        return moduleClass.cast(modules.get(moduleClass));
    }

    public void initModules() throws ModuleInitializeException {
        for (Class<? extends Module> clazz : modules.keySet()) {
            initModule(clazz);
        }
    }

    public void initModule(Class<? extends Module> moduleClass) throws ModuleInitializeException{
        if(!modules.containsKey(moduleClass)) throw new ModuleInitializeException("Module %s not initialized!".formatted(moduleClass.getSimpleName()));
        Module module = modules.get(moduleClass);
        System.out.printf("%s > Initializing module...%n", module.moduleName());
        pool.submit(module);
    }

    public void stopModule(Class<? extends Module> moduleClass) throws ModuleShutdownException{
        if(!modules.containsKey(moduleClass)) throw new ModuleShutdownException("Module %s not loaded!".formatted(moduleClass.getSimpleName()));
        Module module = modules.get(moduleClass);
        System.out.printf("Stopping module %s...%n", module.moduleName());
        module.shutdown();
        System.out.printf("%s > Module successfully stopped!%n", module.moduleName());
    }


    public void stopModules() throws ModuleShutdownException {
        for (Class<? extends Module> clazz : modules.keySet()) {
            stopModule(clazz);
        }
    }

    public synchronized boolean isInit(Class<? extends Module> moduleClass){
        return modules.containsKey(moduleClass);
    }
}
