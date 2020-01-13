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

    public MergeRequestRequest prepare(NewMergeRequest newMergeRequest, Settings settings) throws SourceAndTargetBranchCannotBeEqualException {
        if (!settings.isInitialized()) {
            throw new SettingsNotInitializedException();
        }

        String sourceBranch = newMergeRequest.getSourceBranch();
        String targetBranch = settings.getDefaultTargetBranch();
        if (Objects.equals(sourceBranch, targetBranch)) {
            throw new SourceAndTargetBranchCannotBeEqualException(sourceBranch);
        }

        MergeRequestRequest request = new MergeRequestRequest();
        request.setSourceBranch(sourceBranch);
        request.setTargetBranch(targetBranch);
        setAssignee(request, newMergeRequest, settings);
        setTitle(request, newMergeRequest, settings);
        setDescription(request, newMergeRequest, settings);
        setLabels(request, newMergeRequest, settings);
        request.setRemoveSourceBranch(settings.isRemoveSourceBranchOnMerge());
        return request;
    }

    public CompletableFuture<MergeRequestResponse> submit(String gitLabProjectId, MergeRequestRequest mergeRequestRequest, Settings settings) throws SourceAndTargetBranchCannotBeEqualException {
        GitLab gitLab = createGitLab(settings);
        return gitLab.createMergeRequest(gitLabProjectId, mergeRequestRequest);
    }

    public CompletableFuture<MergeRequestResponse> createMergeRequest(NewMergeRequest newMergeRequest, Settings settings) throws SourceAndTargetBranchCannotBeEqualException, SettingsNotInitializedException {
        MergeRequestRequest request = prepare(newMergeRequest, settings);
        return submit(newMergeRequest.getGitLabProjectId(), request, settings);
    }

    private void setTitle(MergeRequestRequest request, NewMergeRequest newMergeRequest, Settings settings) {
        String title = resolvePlaceHolders(request, newMergeRequest, settings.getDefaultTitle(), settings);
        if (title != null) {
            request.setTitle(title);
        }
    }

    private void setDescription(MergeRequestRequest request, NewMergeRequest newMergeRequest, Settings settings) {
        String description = resolvePlaceHolders(request, newMergeRequest, settings.getDefaultDescription(), settings);
        if (description != null) {
            request.setDescription(description);
        }
    }

    private String resolvePlaceHolders(MergeRequestRequest request, NewMergeRequest newMergeRequest, String template, Settings settings) {
        if (template == null) {
            return template;
        }
        String resolvedTemplate = template;
        resolvedTemplate = resolvedTemplate.replaceAll("\\{\\{sourceBranch}}", newMergeRequest.getSourceBranch());
        resolvedTemplate = resolvedTemplate.replaceAll("\\{\\{targetBranch}}", settings.getDefaultTargetBranch());

        return resolvedTemplate;
    }

    private void setAssignee(MergeRequestRequest request, NewMergeRequest newMergeRequest, Settings settings) {
        if (settings.isAssigneesEnabled()) {
            if (newMergeRequest.getAssignee() == null) {
                request.setAssigneeId(settings.getDefaultAssigneeId());
            } else {
                request.setAssigneeId(newMergeRequest.getAssignee().getId());
            }
        }
    }

    private void setLabels(MergeRequestRequest request, NewMergeRequest newMergeRequest, Settings settings) {
        request.setLabels(settings.getDefaultLabels());
    }


    @NotNull
    protected GitLab createGitLab(Settings settings) {
        return new GitLab(settings.getGitLabUri(), settings.getAccessToken(), settings.isInsecureTls());
    }

    @NotNull
    private String getSourceBranch(SelectedModule selectedModule) {
        return this.gitService.getCurrentBranch(selectedModule);
    }


}
