package com.github.novotnyr.idea.gitlab;

public class UnsupportedContentTypeException extends RuntimeException {

    public UnsupportedContentTypeException() {
        super();
    }

    public UnsupportedContentTypeException(String msg) {
        super(msg);
    }

    public UnsupportedContentTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedContentTypeException(Throwable cause) {
        super(cause);
    }
}