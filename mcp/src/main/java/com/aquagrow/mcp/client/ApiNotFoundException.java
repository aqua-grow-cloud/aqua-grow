package com.aquagrow.mcp.client;

/** The API answered 404 for the requested resource. */
public class ApiNotFoundException extends ToolUserException {

    public ApiNotFoundException(String message) {
        super(message);
    }
}
