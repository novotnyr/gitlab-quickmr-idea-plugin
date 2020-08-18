package com.github.novotnyr.idea.gitlab;

public class GitLabIOException extends RuntimeException {
    public GitLabIOException(String message, Throwable cause) {
        super(message, cause);
    }
}