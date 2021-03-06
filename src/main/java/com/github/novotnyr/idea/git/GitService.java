package com.github.novotnyr.idea.git;

import com.github.novotnyr.idea.gitlab.quickmr.SelectedModule;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.history.VcsRevisionDescription;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.branch.GitBranchesCollection;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitService {
    private static final Logger LOG = Logger.getInstance("#com.github.novotnyr.idea.git.GitService");

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
            String repoId = matcher.group(2);
            if (url.startsWith("http")) {
                // Let's extract away hostname.
                // E. g. gitlab.com/example/dummy-project is transformed to
                // example/dummy-project
                repoId = repoId.substring(repoId.indexOf("/") + 1);
            }
            return repoId;
        }
        return null;
    }

    public Optional<String> getLastCommitMessage(Project project) {
        try {
            return Optional.ofNullable(GitHistoryUtils.getCurrentRevisionDescription(project, VcsUtil
                    .getFilePath(project.getBaseDir())))
                    .map(VcsRevisionDescription::getCommitMessage);
        } catch (VcsException e) {
            LOG.error("Unable to load last commit message", e);
            return Optional.empty();
        }
    }

    public Optional<String> getLastCommitMessageSubject(Project project) {
        Optional<String> lastCommitMessage = getLastCommitMessage(project);
        if (lastCommitMessage.isPresent()) {
            String commitMessage = lastCommitMessage.get();
            if (commitMessage.contains(System.lineSeparator())) {
                return Optional.of(commitMessage.substring(0, commitMessage.indexOf(System.lineSeparator())).trim());
            }
        }
        return lastCommitMessage;
    }

    public Optional<String> getLastCommitMessageBody(Project project) {
        Optional<String> lastCommitMessage = getLastCommitMessage(project);
        if (lastCommitMessage.isPresent()) {
            String commitMessage = lastCommitMessage.get();
            if (commitMessage.contains(System.lineSeparator())) {
                return Optional.of(commitMessage.substring(commitMessage.indexOf(System.lineSeparator())).trim());
            }
        }
        return lastCommitMessage;
    }
}
