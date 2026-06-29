package br.com.seuprojeto.pascoa.configengine.exception;

public class ConfigException extends RuntimeException {
    public ConfigException(String message) { super(message); }
    public ConfigException(String message, Throwable cause) { super(message, cause); }
}
