package org.example.nanobananaprobot.errors;

public class SessionExpiredException extends SearchException {
    public SessionExpiredException(String message) {
        super(message);
    }
}