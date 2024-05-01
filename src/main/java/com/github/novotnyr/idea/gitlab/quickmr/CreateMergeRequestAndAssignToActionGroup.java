package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.gitlab.User;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CreateMergeRequestAndAssignToActionGroup extends ActionGroup {
    public CreateMergeRequestAndAssignToActionGroup() {
        super("Quick Merge Request Assigned to", true);
    }

    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        if (anActionEvent == null) {
            return new AnAction[0];
        }
        Project project = anActionEvent.getProject();
        if (project == null) {
            return new AnAction[0];
        }
        Settings settings = project.getService(Settings.class);

        List<AnAction> actions = new ArrayList<>();
        for (User assignee : settings.getDefaultAssignees()) {
            CreateMergeRequestAction action = new CreateMergeRequestAction("" + assignee.getName());
            action.setAssignee(assignee);

            actions.add(action);
        }
        return actions.toArray(new AnAction[0]);
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Settings settings = project.getService(Settings.class);
        e.getPresentation().setVisible(settings.isAssigneesEnabled());
    }
}
