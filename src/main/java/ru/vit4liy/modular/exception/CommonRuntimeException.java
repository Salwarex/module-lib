package ru.vit4liy.modular.exception;

public abstract class CommonRuntimeException extends RuntimeException {
    public CommonRuntimeException(String message) {
        super(message);
    }
}
