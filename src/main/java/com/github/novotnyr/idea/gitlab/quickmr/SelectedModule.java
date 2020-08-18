package com.github.novotnyr.idea.gitlab.quickmr;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

public class SelectedModule {
    private Project project;

    private VirtualFile file;

    public static SelectedModule fromEvent(AnActionEvent event) {
        Project project = event.getProject();
        if (project == null) {
            return null;
        }
        VirtualFile file = event.getData(CommonDataKeys.VIRTUAL_FILE);
        if (file == null) {
            return null;
        }
        SelectedModule selectedModule = new SelectedModule();
        selectedModule.file = file;
        selectedModule.project = project;
        return selectedModule;
    }

    public Project getProject() {
        return project;
    }

    public VirtualFile getFile() {
        return file;
    }
}
