package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.git.GitService;
import com.github.novotnyr.idea.git.NoSuchGitRemoteException;
import com.github.novotnyr.idea.gitlab.AccessDeniedException;
import com.github.novotnyr.idea.gitlab.DuplicateMergeRequestException;
import com.github.novotnyr.idea.gitlab.MergeRequestRequest;
import com.github.novotnyr.idea.gitlab.MergeRequestResponse;
import com.github.novotnyr.idea.gitlab.User;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.github.novotnyr.idea.gitlab.quickmr.settings.SettingsUi;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.Strings;
import com.intellij.util.ModalityUiUtil;
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static java.util.concurrent.CompletableFuture.completedFuture;

public class CreateMergeRequestAction extends AnAction {

    private User assignee;

    protected CreateMergeRequestAction() {
        // no-op
    }

    public CreateMergeRequestAction(@Nullable String text) {
        super(text);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        SelectedModule selectedModule = SelectedModule.fromEvent(event);
        if (selectedModule == null) {
            return;
        }
        getProjectName(selectedModule).thenAccept(gitLabProjectId -> {
            createMergeRequestAsync(selectedModule, gitLabProjectId);
        });
    }

    @RequiresBackgroundThread
    private void createMergeRequestAsync(SelectedModule selectedModule, String gitLabProjectId) {
        Project project = selectedModule.getProject();
        try {
            Settings settings = project.getService(Settings.class);
            GitService git = GitService.getInstance();

            PlaceholderResolver placeholderResolver = new PlaceholderResolver(git, project, settings);
            MergeRequestService mergeRequestService = new MergeRequestService(git, placeholderResolver);
            NewMergeRequest mergeRequest = new NewMergeRequest();
            mergeRequest.setAssignee(this.assignee);
            mergeRequest.setGitLabProjectId(gitLabProjectId);
            mergeRequest.setSourceBranch(getSourceBranch(selectedModule));

            MergeRequestRequest request = mergeRequestService.prepare(mergeRequest, settings);
            validate(request, selectedModule, settings)
                    .thenCompose(__ -> mergeRequestService.submit(mergeRequest.getGitLabProjectId(), request, settings))
                    .thenAccept(mergeRequestResponse -> createNotification(mergeRequestResponse, project, gitLabProjectId, settings))
                    .exceptionally(this::createErrorNotification);

        } catch (SourceAndTargetBranchCannotBeEqualException e) {
            this.createErrorNotification(e);
        } catch (SettingsNotInitializedException e) {
            new Notification("GitLab Merge Request",
                    "GitLab Quick Merge Requests are not configured",
                    "",
                    NotificationType.INFORMATION)
                    .addAction(NotificationAction.createSimpleExpiring("Configure", () ->
                            ShowSettingsUtil.getInstance()
                                            .showSettingsDialog(project, SettingsUi.class)))
                    .notify(project);
        }
    }

    // Runs on nonEDT
    protected CompletableFuture<Boolean> validate(MergeRequestRequest request, SelectedModule module, Settings settings) {
        if (!settings.isShowConfirmationDialog()) {
            return completedFuture(true);
        } else {
            return showConfirmationDialog(request, module);
        }
    }

    protected CompletableFuture<Boolean> showConfirmationDialog(MergeRequestRequest request, SelectedModule module) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        ModalityUiUtil.invokeLaterIfNeeded(ModalityState.defaultModalityState(), () -> {
            if (!isAcceptedByUser(request, module)) {
                result.completeExceptionally(new RequestCannotBeSubmittedException());
            } else {
                result.complete(true);
            }
        });
        return result;
    }

    private boolean isAcceptedByUser(MergeRequestRequest request, SelectedModule module) {
        ConfirmMergeRequestDialog dialog = new ConfirmMergeRequestDialog(request, module);
        if (!dialog.showAndGet()) {
            return false;
        }
        request.setTargetBranch(dialog.getTargetBranch());
        request.setTitle(dialog.getMergeRequestTitle());
        request.setRemoveSourceBranch(dialog.isSourceBranchDeletedOnAccept());
        dialog.getMergeRequestDescription().ifPresent(request::setDescription);
        dialog.getMergeRequestLabels().ifPresent(request::setLabels);
        Optional<User> maybeAssignee = dialog.getAssignee();
        if (maybeAssignee.isPresent()) {
            maybeAssignee
                    .map(User::getId)
                    .ifPresent(request::setAssigneeId);
        } else {
            request.setAssigneeId(null);
        }
        return true;
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Settings settings = project.getService(Settings.class);
        if (this.assignee == null) {
            // assignee will be deduced from settings, enable according to preferences
            e.getPresentation().setEnabledAndVisible(settings.isEnableMergeRequestToFavoriteAssignee());
            return;
        }
        e.getPresentation().setEnabledAndVisible(true);
    }

    private CompletableFuture<String> getProjectName(SelectedModule selectedModule) {
        CompletableFuture<String> result = new CompletableFuture<>();
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                GitService git = GitService.requireInstance();
                String projectGitUrl = git.getProjectGitUrl(selectedModule);
                if (projectGitUrl == null) {
                    result.completeExceptionally(new NoSuchGitRemoteException(selectedModule));
                }
                String gitLabProjectId = git.getRepoPathWithoutDotGit(projectGitUrl);
                result.complete(gitLabProjectId);
            } catch (Exception e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    @NotNull
    private String getSourceBranch(SelectedModule selectedModule) {
        return GitService.requireInstance().getCurrentBranch(selectedModule);
    }

    private void createNotification(MergeRequestResponse mergeRequestResponse, Project project, String projectName, Settings settings) {
        String number = mergeRequestResponse.getNumber();
        String assignee = mergeRequestResponse.getAssigneeName();
        String assigneeMessage;
        if (assignee != null) {
            assigneeMessage = "Assigned in <i>" + projectName + "</i> to " + assignee + "<br/><br/>";
        } else {
            assigneeMessage = "Created in <i>" + projectName + "</i><br/><br/>";
        }


        String title = "Merge Request #" + number + " Created";

        NotificationListener notificationListener = (notification, event) -> {};
        String webUrl = mergeRequestResponse.getWebUrl();
        if (Strings.isNotEmpty(webUrl)) {
            assigneeMessage += "<a href='mr'>View in GitLab</a>";
            notificationListener = (notification, hyperlinkEvent) -> {
                try {
                    BrowserLauncher.getInstance().browse(new URI(webUrl));
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            };
        }

        var notification = new Notification("GitLab Merge Request", title,
                assigneeMessage, NotificationType.INFORMATION)
                .setListener(notificationListener);
        Notifications.Bus.notify(notification);
    }

    private Void createErrorNotification(Throwable t) {
        String title = "Merge Request Failed";
        String messagePrefix = "Failed to create merge request: ";
        Throwable exception = unwrapCompletionException(t);
        String message = messagePrefix + exception.getMessage();
        NotificationType notificationType = null;
        if (exception instanceof RequestCannotBeSubmittedException) {
            // user cancelled
            return null;
        }
        if (exception instanceof DuplicateMergeRequestException) {
            title = "Merge Request Already Exists";
            message = "Merge Request has already been submitted";
            notificationType = NotificationType.WARNING;
        }
        if (exception instanceof AccessDeniedException) {
            title = "GitLab Access Denied";
            message = "Please check Access Token in Preferences.";
            notificationType = NotificationType.ERROR;
        }

        Notification notification = new Notification("GitLab Merge Request", title, message, notificationType);

        Notifications.Bus.notify(notification);
        return null;
    }

    private Throwable unwrapCompletionException(Throwable t) {
        return t instanceof CompletionException ? t.getCause() : t;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    private static class RequestCannotBeSubmittedException extends RuntimeException {
        // empty body
    }

}
