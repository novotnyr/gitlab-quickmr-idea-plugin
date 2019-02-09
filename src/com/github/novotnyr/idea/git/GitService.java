package com.github.novotnyr.idea.git;

import com.github.novotnyr.idea.gitlab.quickmr.SelectedModule;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.branch.GitBranchesCollection;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitService {
    public Collection<GitLocalBranch> getLocalBranches(Project project, VirtualFile file) {
        GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
        GitRepository repo = repositoryManager.getRepositoryForFile(file);
        if (repo == null) {
            return null;
        }
        GitBranchesCollection branches = repo.getBranches();
        return branches.getLocalBranches();
    }

    public String getCurrentBranch(SelectedModule module) {
        return getCurrentBranch(module.getProject(), module.getFile());
    }

    public String getCurrentBranch(Project project, VirtualFile file) {
        GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
        GitRepository repo = repositoryManager.getRepositoryForFile(file);
        if (repo == null) {
            return null;
        }
        GitLocalBranch currentBranch = repo.getCurrentBranch();
        if (currentBranch == null) {
            return null;
        }
        return currentBranch.getName();
    }

    public String getProjectGitUrl(SelectedModule selectedModule) {
        GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(selectedModule.getProject());
        GitRepository repo = repositoryManager.getRepositoryForFile(selectedModule.getFile());
        if (repo == null) {
            return null;
        }
        for (GitRemote remote : repo.getRemotes()) {
            return remote.getFirstUrl();
        }
        return null;
    }

    public static final Pattern pattern = Pattern.compile("(?:git|ssh|https?|git@[-\\w.]+):(//)?(.*?)(\\.git)(/?|#[-\\d\\w._]+?)$");

    public String getRepoPathWithoutDotGit(String url) {
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }
}
