package com.github.novotnyr.idea.gitlab.quickmr;

public class SettingsNotInitializedException extends RuntimeException {

    public SettingsNotInitializedException() {
        super("Settings were not initialized in Configuration");
    }

}