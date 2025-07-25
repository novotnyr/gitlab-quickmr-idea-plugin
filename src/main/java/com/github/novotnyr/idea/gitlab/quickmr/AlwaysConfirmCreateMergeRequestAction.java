package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.gitlab.MergeRequestRequest;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

import java.util.concurrent.CompletableFuture;

public class AlwaysConfirmCreateMergeRequestAction extends CreateMergeRequestAction {
    @Override
    protected CompletableFuture<Boolean> validate(MergeRequestRequest request, SelectedModule module, Settings settings) {
        return showConfirmationDialog(request, module);
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Settings settings = project.getService(Settings.class);
        e.getPresentation().setEnabledAndVisible(!settings.isAssigneesEnabled() && !settings.isEnableMergeRequestToFavoriteAssignee());
    }
}
