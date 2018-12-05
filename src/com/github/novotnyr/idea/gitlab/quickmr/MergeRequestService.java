package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.git.GitService;
import com.github.novotnyr.idea.gitlab.GitLab;
import com.github.novotnyr.idea.gitlab.MergeRequestRequest;
import com.github.novotnyr.idea.gitlab.MergeRequestResponse;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MergeRequestService {
    private final GitService gitService;

    public MergeRequestService(GitService gitService) {
        this.gitService = gitService;
    }

    public CompletableFuture<MergeRequestResponse> createMergeRequest(NewMergeRequest newMergeRequest, Settings settings) throws SourceAndTargetBranchCannotBeEqualException, SettingsNotInitializedException {
        if (!settings.isInitialized()) {
            throw new SettingsNotInitializedException();
        }

        GitLab gitLab = createGitLab(settings);

        String sourceBranch = newMergeRequest.getSourceBranch();
        String targetBranch = settings.getDefaultTargetBranch();
        if (Objects.equals(sourceBranch, targetBranch)) {
            throw new SourceAndTargetBranchCannotBeEqualException(sourceBranch);
        }

        MergeRequestRequest request = new MergeRequestRequest();
        request.setSourceBranch(sourceBranch);
        request.setTargetBranch(targetBranch);
        if (settings.isAssigneesEnabled()) {
            if (newMergeRequest.getAssignee() == null) {
                request.setAssigneeId(settings.getDefaultAssigneeId());
            } else {
                request.setAssigneeId(newMergeRequest.getAssignee().getId());
            }
        }
        request.setTitle(settings.getDefaultTitle());
        request.setRemoveSourceBranch(settings.isRemoveSourceBranchOnMerge());

        return gitLab.createMergeRequest(newMergeRequest.getGitLabProjectId(), request);
    }

    @NotNull
    protected GitLab createGitLab(Settings settings) {
        return new GitLab(settings.getGitLabUri(), settings.getAccessToken());
    }

    @NotNull
    private String getSourceBranch(SelectedModule selectedModule) {
        return this.gitService.getCurrentBranch(selectedModule);
    }


}
