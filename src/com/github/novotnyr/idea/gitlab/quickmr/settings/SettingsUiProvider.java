package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConfigurableProvider;
import org.jetbrains.annotations.Nullable;

public class SettingsUiProvider implements VcsConfigurableProvider {
    @Nullable
    @Override
    public Configurable getConfigurable(Project project) {
        return new SettingsUi(project);
    }
}
