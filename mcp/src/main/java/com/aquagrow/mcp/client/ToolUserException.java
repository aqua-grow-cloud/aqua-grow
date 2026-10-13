package com.aquagrow.mcp.client;

/**
 * A failure whose message is safe to show to the model and the user:
 * it never contains stack traces, URLs, tokens or other internals.
 */
public class ToolUserException extends RuntimeException {

    public ToolUserException(String message) {
        super(message);
    }

    public ToolUserException(String message, Throwable cause) {
        super(message, cause);
    }
}
