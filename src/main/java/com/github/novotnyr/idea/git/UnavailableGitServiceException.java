package com.github.novotnyr.idea.git;

public class UnavailableGitServiceException extends RuntimeException {
    public UnavailableGitServiceException() {
        super("Git service is not available");
    }
}