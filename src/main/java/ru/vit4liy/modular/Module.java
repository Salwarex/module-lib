package ru.vit4liy.modular;

import ru.vit4liy.modular.exception.ModuleInitializeException;
import ru.vit4liy.modular.exception.ModuleShutdownException;

import java.util.HashSet;
import java.util.Set;

public abstract class Module implements Runnable{
    public abstract void initialize() throws ModuleInitializeException;
    public abstract void shutdown() throws ModuleShutdownException;
    public abstract String moduleName();
    protected Set<Class<? extends Module>> dependencies;
    private Set<Class<? extends Module>> unloadedDependencies;
    protected ModuleLoader loader;

    public Module(ModuleLoader loader) {
        this.loader = loader;
    }

    @Override
    public void run() {
        dependencies = dependencies();
        unloadedDependencies = new HashSet<>(dependencies);
        waitDependencies();
        System.out.printf("%s > All dependencies successfully initialized!%n", moduleName());
        try { initialize(); } catch (ModuleInitializeException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract Set<Class<? extends Module>> dependencies();

    public void waitDependencies(){
        while (!unloadedDependencies.isEmpty()){
            try{Thread.sleep(1000L);}
            catch (InterruptedException e){throw new RuntimeException(e);}

            for(Class<? extends Module> dependency : dependencies){
                if(!unloadedDependencies.contains(dependency)) continue;
                if(loader.isInit(dependency)) {
                    System.out.printf("%s > Dependency %s successfully found%n", this.moduleName(), dependency.getSimpleName());
                    unloadedDependencies.remove(dependency);
                }
                else{
                    System.err.printf("%s > Dependency %s not found! Waiting for loading...%n", this.moduleName(), dependency.getSimpleName());
                }
            }
        }
    }
}
