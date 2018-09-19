package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.gitlab.User;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CreateMergeRequestAndAssignToActionGroup extends ActionGroup {
    public CreateMergeRequestAndAssignToActionGroup() {
        super("Quick Merge Request Assigned to", true);
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        if (anActionEvent == null) {
            return new AnAction[0];
        }
        Project project = anActionEvent.getProject();
        if (project == null) {
            return new AnAction[0];
        }
        Settings settings = ServiceManager.getService(project, Settings.class);

        List<AnAction> actions = new ArrayList<>();
        for (User assignee : settings.getDefaultAssignees()) {
            CreateMergeRequestAction action = new CreateMergeRequestAction("" + assignee.getName());
            action.setAssignee(assignee);

            actions.add(action);
        }
        return actions.toArray(new AnAction[0]);
    }
}
