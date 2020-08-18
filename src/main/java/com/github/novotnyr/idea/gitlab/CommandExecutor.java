package com.github.novotnyr.idea.gitlab;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface CommandExecutor {
    <T> CompletableFuture<T> execute(AbstractGitLabCommand<T> command);

    CommandExecutor DEFAULT = new CommandExecutor() {
        @Override
        public <T> CompletableFuture<T> execute(AbstractGitLabCommand<T> command) {
            return command.call();
        }
    };
}
