package com.github.novotnyr.idea.git;

import com.github.novotnyr.idea.gitlab.quickmr.SelectedModule;

public class NoSuchGitRemoteException extends RuntimeException {
    public NoSuchGitRemoteException(SelectedModule selectedModule) {
        super("No Git remote for '" + selectedModule.getFile()
                + "' in project '" + selectedModule.getProject() + "'");
    }
}