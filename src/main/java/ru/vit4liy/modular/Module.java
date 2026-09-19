package ru.vit4liy.modular;

import ru.vit4liy.modular.exception.ModuleInitializeException;
import ru.vit4liy.modular.exception.ModuleShutdownException;

public interface Module {
    void initialize() throws ModuleInitializeException;
    void shutdown() throws ModuleShutdownException;
    String moduleName();
}
