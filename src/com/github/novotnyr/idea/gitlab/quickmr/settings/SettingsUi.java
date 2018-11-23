package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.GitLab;
import com.github.novotnyr.idea.gitlab.GitLabHttpResponseException;
import com.github.novotnyr.idea.gitlab.User;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vcs.VcsConfigurableProvider;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

public class SettingsUi implements Configurable {
    private final Project project;

    private JBTextField urlTextField;
    private JBPasswordField accessTokenTextField;
    private JBTextField targetBranchTextField;
    private JBTextField mergeRequestTitleTextField;
    private JButton validateServerButton;
    private JLabel defaultAssigneeLabel;
    private JBList<User> assigneeList;
    private JPanel assigneeListPlaceHolder;
    private JPanel rootPanel;
    private JCheckBox enableDefaultAssigneeActionCheckBox;
    private JCheckBox removeSourceBranchCheckbox;

    private CollectionListModel<User> assigneeListModel = new CollectionListModel<>();

    private Settings settings;

    /**
     * Cached hashcode of access token to speed up isModified()
     */
    private int accessTokenHashCode;

    public SettingsUi(Project project) {
        this.project = project;

        this.urlTextField.getEmptyText().setText("https://gitlab.com/api/v4");

        this.assigneeList = new JBList<>();
        this.assigneeList.setModel(this.assigneeListModel);
        this.assigneeList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                User user = (User) value;
                String renderedText = user.getName() + " (" + user.getUsername() + ")";
                if (index == 0) {
                    Component component = super.getListCellRendererComponent(list, renderedText, index, isSelected, cellHasFocus);
                    if (component instanceof JLabel) {
                        JLabel label = (JLabel) component;
                        label.setIcon(AllIcons.Toolwindows.ToolWindowFavorites);
                        return label;
                    }
                }
                return super.getListCellRendererComponent(list, renderedText, index, isSelected, cellHasFocus);
            }
        });

        this.assigneeListPlaceHolder.setLayout(new BorderLayout());
        this.assigneeListPlaceHolder.add(
                ToolbarDecorator
                        .createDecorator(this.assigneeList)
                        .setAddAction(this::onAddAssignee)
                        .setAddActionUpdater(this::isAddAssigneeEnabled)
                        .setRemoveAction(this::onRemoveAssignee)
                        .createPanel()
        );

        this.validateServerButton.addActionListener(this::onValidateServerButtonClicked);
    }

    private void bindToComponents(Settings settings) {
        this.urlTextField.setText(settings.getGitLabUri());
        this.accessTokenTextField.setText(settings.getAccessToken());
        this.targetBranchTextField.setText(settings.getDefaultTargetBranch());
        this.mergeRequestTitleTextField.setText(settings.getDefaultTitle());
        this.assigneeListModel.replaceAll(settings.getDefaultAssignees());
        this.enableDefaultAssigneeActionCheckBox.setSelected(settings.isEnableMergeRequestToFavoriteAssignee());
        this.removeSourceBranchCheckbox.setSelected(settings.isRemoveSourceBranchOnMerge());
    }

    private void onValidateServerButtonClicked(ActionEvent event) {
        GitLab gitLab = new GitLab(this.urlTextField.getText(), String.valueOf(accessTokenTextField.getPassword()));
                gitLab.version().thenRun(() -> {
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder("GitLab connection successful", MessageType.INFO, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(this.validateServerButton),
                                    Balloon.Position.atRight);

                })
                .exceptionally(t -> {
                    warnInvalidServer(t);
                    return null;
                });
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    //-------

    public void onAddAssignee(AnActionButton anActionButton) {
        GitLab gitLab = new GitLab(this.urlTextField.getText(), String.valueOf(accessTokenTextField.getPassword()));
        gitLab.version().thenRun(() -> {
                    ApplicationManagerEx.getApplicationEx().invokeLater(() -> {
                        SelectAssigneeDialog dialog = new SelectAssigneeDialog(this.project, gitLab);
                        if (dialog.showAndGet()) {
                            User assignee = dialog.getAssignee();
                            if (assignee != null) {
                                this.assigneeListModel.add(assignee);
                            }
                        }
                    }, ModalityState.any());
                }
        )
                .exceptionally(t -> {
                    warnInvalidServer(t);
                    return null;
                });
    }

    private boolean isAddAssigneeEnabled(AnActionEvent event) { ;
        return StringUtils.isNotEmpty(this.urlTextField.getText());
    }

    private void onRemoveAssignee(AnActionButton anActionButton) {
        int selectedIndex = this.assigneeList.getSelectedIndex();
        if (selectedIndex >= 0) {
            this.assigneeListModel.remove(selectedIndex);
        }
    }

    public List<String> validate() {
        List<String> validationErrors = new ArrayList<>();
        if (!StringUtils.isNotEmpty(this.urlTextField.getText())) {
            validationErrors.add("Missing GitLab URI");
        }
        if (!StringUtils.isNotEmpty(this.targetBranchTextField.getText())) {
            validationErrors.add("Missing default target branch");
        }
        if (!StringUtils.isNotEmpty(this.mergeRequestTitleTextField.getText())) {
            validationErrors.add("Missing default Merge Request title");
        }

        if (this.assigneeListModel == null || this.assigneeListModel.isEmpty()) {
            validationErrors.add("Please set at least one assignee");
        }
        return validationErrors;
    }

    private void warnInvalidServer(Throwable throwable) {
        String errorMessage = getInvalidServerErrorMessage(throwable);
        JBPopupFactory.getInstance()
                .createHtmlTextBalloonBuilder(errorMessage, MessageType.ERROR, null)
                .setFadeoutTime(7500)
                .createBalloon()
                .show(RelativePoint.getNorthWestOf(this.assigneeList),
                        Balloon.Position.atRight);
    }

    private String getInvalidServerErrorMessage(Throwable throwable) {
        StringBuilder additionalErrorMessage = new StringBuilder("\n");
        Throwable cause = throwable;
        if (throwable instanceof CompletionException) {
            cause = throwable.getCause();
        }
        if (cause instanceof HttpResponseException) {
            HttpResponseException httpResponseException = (HttpResponseException) cause;
            additionalErrorMessage
                    .append("HTTP Status: ").append(httpResponseException.getStatusCode()).append("\n")
                    .append("HTTP Reply: ").append(httpResponseException.getMessage());
        }
        if (cause instanceof GitLabHttpResponseException) {
            GitLabHttpResponseException gitLabHttpResponseException = (GitLabHttpResponseException) cause;
            additionalErrorMessage
                    .append("HTTP Status: ").append(gitLabHttpResponseException.getStatusCode()).append("\n")
                    .append("HTTP Reply: ").append(gitLabHttpResponseException.getMessage()).append("\n")
                    .append("HTTP Response: ").append(gitLabHttpResponseException.getResponseBody());
        }
        return "Server is not available. Please check URL or access token." + additionalErrorMessage;
    }

    //-------

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "GitLab Quick Merge Request";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return this.rootPanel;
    }

    @Override
    public boolean isModified() {
        boolean unmodified = this.urlTextField.getText().equals(settings.getGitLabUri())
                && ! isAccessTokenModified()
                && this.targetBranchTextField.getText().equals(settings.getDefaultTargetBranch())
                && this.mergeRequestTitleTextField.getText().equals(settings.getDefaultTitle())
                && this.enableDefaultAssigneeActionCheckBox.isSelected() == (settings.isEnableMergeRequestToFavoriteAssignee())
                && this.removeSourceBranchCheckbox.isSelected() == (settings.isRemoveSourceBranchOnMerge());

        return !unmodified;
    }

    private boolean isAccessTokenModified() {
        int accessTokenHash = new String(this.accessTokenTextField.getPassword()).hashCode();
        String storedAccessToken = settings.getAccessToken();
        if (storedAccessToken == null) {
            storedAccessToken = "";
        }
        int storedAccessTokenHash = storedAccessToken.hashCode();
        return accessTokenHash != storedAccessTokenHash;
    }

    @Override
    public void reset() {
        this.settings = ServiceManager.getService(this.project, Settings.class);
        bindToComponents(settings);
        cacheAccessToken();
    }

    private void cacheAccessToken() {
        String accessToken = this.settings.getAccessToken();
        if (accessToken == null) {
            accessToken = "";
        }
        this.accessTokenHashCode = accessToken.hashCode();
    }

    @Override
    public void apply() throws ConfigurationException {
        List<String> validationErrors = validate();
        if (!validationErrors.isEmpty()) {
            throw new ConfigurationException("<li>" + String.join("<li>", validationErrors));
        }

        settings.setGitLabUri(this.urlTextField.getText());
        settings.setAccessToken(String.valueOf(this.accessTokenTextField.getPassword()));
        settings.setDefaultTargetBranch(this.targetBranchTextField.getText());
        settings.setDefaultAssignees(this.assigneeListModel.getItems());
        settings.setDefaultTitle(this.mergeRequestTitleTextField.getText());
        settings.setEnableMergeRequestToFavoriteAssignee(this.enableDefaultAssigneeActionCheckBox.isSelected());
        settings.setRemoveSourceBranchOnMerge(this.removeSourceBranchCheckbox.isSelected());
    }

    public static class ConfigurableProvider implements VcsConfigurableProvider {
        @Override
        public Configurable getConfigurable(Project project) {
            return new SettingsUi(project);
        }
    }

}
