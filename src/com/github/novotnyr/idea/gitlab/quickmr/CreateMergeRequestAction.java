package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.git.GitService;
import com.github.novotnyr.idea.gitlab.DuplicateMergeRequestException;
import com.github.novotnyr.idea.gitlab.MergeRequestResponse;
import com.github.novotnyr.idea.gitlab.User;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.github.novotnyr.idea.gitlab.quickmr.settings.SettingsUi;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.event.HyperlinkEvent;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class CreateMergeRequestAction extends AnAction {
    private User assignee;

    private final GitService gitService = ServiceManager.getService(GitService.class);

    public CreateMergeRequestAction() {
    }

    public CreateMergeRequestAction(Icon icon) {
        super(icon);
    }

    public CreateMergeRequestAction(@Nullable String text) {
        super(text);
    }

    public CreateMergeRequestAction(@Nullable String text, @Nullable String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void actionPerformed(AnActionEvent event) {
        SelectedModule selectedModule = SelectedModule.fromEvent(event);
        if (selectedModule == null) {
            return;
        }

        Project project = event.getProject();
        Settings settings = ServiceManager.getService(project, Settings.class);

        try {
            String gitLabProjectId = getProjectName(selectedModule);
            MergeRequestService mergeRequestService = new MergeRequestService(this.gitService);
            NewMergeRequest mergeRequest = new NewMergeRequest();
            mergeRequest.setAssignee(this.assignee);
            mergeRequest.setGitLabProjectId(gitLabProjectId);
            mergeRequest.setSourceBranch(getSourceBranch(selectedModule));

            mergeRequestService.createMergeRequest(mergeRequest, settings)
                    .thenAccept(mergeRequestResponse -> createNotification(mergeRequestResponse, project, gitLabProjectId, settings))
                    .exceptionally(this::createErrorNotification);

        } catch (SourceAndTargetBranchCannotBeEqualException e) {
            this.createErrorNotification(e);
        } catch (SettingsNotInitializedException e) {
            Notification notification = new Notification("quickmr", "Quick Merge Request are not configured",
                    "Quick Merge Requests are not configured<br/> <a href='link'>Configure</a>",
                    NotificationType.INFORMATION,
                    new NotificationListener() {
                        @Override
                        public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
                            ShowSettingsUtil.getInstance().showSettingsDialog(project, SettingsUi.class);
                            notification.expire();
                        }
                    }
            );
            Notifications.Bus.notify(notification);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) {
            return;
        }
        Settings settings = ServiceManager.getService(project, Settings.class);
        if (this.assignee == null) {
            // assignee will be deduced from settings, enable according to preferences
            e.getPresentation().setEnabledAndVisible(settings.isEnableMergeRequestToFavoriteAssignee());
            return;
        }
        e.getPresentation().setEnabledAndVisible(true);
    }

    private String getProjectName(SelectedModule selectedModule) {
        String projectGitUrl = this.gitService.getProjectGitUrl(selectedModule);
        if (projectGitUrl == null) {
            return null;
        }
        return this.gitService.getRepoPathWithoutDotGit(projectGitUrl);
    }

    @NotNull
    private String getSourceBranch(SelectedModule selectedModule) {
        return this.gitService.getCurrentBranch(selectedModule);
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
        Notification notification = new Notification("quickmr", title,
                assigneeMessage + "<a href='mr'>View in GitLab</a>",
                NotificationType.INFORMATION,
                new NotificationListener() {
                    @Override
                    public void hyperlinkUpdate(@NotNull Notification notification, @NotNull HyperlinkEvent hyperlinkEvent) {
                        Desktop desktop = Desktop.getDesktop();
                        try {
                            desktop.browse(new URI(mergeRequestResponse.getWebUrl()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (URISyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
        );
        Notifications.Bus.notify(notification);
    }

    private Void createErrorNotification(Throwable t) {
        String title = "Merge Request Failed";
        String message = "Failed to create merge request: " + t.getMessage();
        NotificationType notificationType = NotificationType.ERROR;
        if (t.getCause() instanceof DuplicateMergeRequestException) {
            title = "Merge Request Already Exists";
            message = "Merge Request has already been submitted";
            notificationType = NotificationType.WARNING;
        }

        Notification notification = new Notification("quickmr", title, message, notificationType);

        Notifications.Bus.notify(notification);
        return null;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }
}
