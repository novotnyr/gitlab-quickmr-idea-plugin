package com.github.novotnyr.idea.gitlab.quickmr;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AssigneeGroup extends ActionGroup {
    public AssigneeGroup() {
        super("Assignees", true);
    }

    @NotNull
    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent anActionEvent) {
        return new AnAction[] {
            new AnAction("One") {
                @Override
                public void actionPerformed(AnActionEvent anActionEvent) {

                }
            },
            new AnAction("Two") {
                @Override
                public void actionPerformed(AnActionEvent anActionEvent) {

                }
            }
        };
    }
}
