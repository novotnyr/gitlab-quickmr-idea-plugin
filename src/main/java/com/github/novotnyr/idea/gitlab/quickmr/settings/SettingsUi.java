package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.GitLab;
import com.github.novotnyr.idea.gitlab.GitLabHttpResponseException;
import com.github.novotnyr.idea.gitlab.User;
import com.github.novotnyr.idea.gitlab.quickmr.IllegalGitLabUrlException;
import com.google.gson.JsonSyntaxException;
import com.intellij.icons.AllIcons;
import com.intellij.ide.browsers.BrowserLauncher;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
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
import com.squareup.okhttp.HttpUrl;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpResponseException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;

public class SettingsUi implements Configurable {
    private final Project project;
    private final BrowserLauncher browserLauncher;

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
    private JCheckBox enableAssigneesCheckBox;
    private JButton openAccessTokenUrlButton;
    private JCheckBox showConfirmationDialogCheckBox;
    private JCheckBox insecureTLSCheckBox;
    private JTextArea mergeRequestDescriptionTextArea;
    private JBTextField labelsTextField;

    private CollectionListModel<User> assigneeListModel = new CollectionListModel<>();

    private Settings settings;

    private boolean serverUrlValidated = true;

    /**
     * Cached hashcode of access token to speed up isModified()
     */
    private int accessTokenHashCode;

    public SettingsUi(Project project) {
        this.project = project;
        this.browserLauncher = BrowserLauncher.getInstance();

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

        this.labelsTextField.getEmptyText().setText("labels are comma separated");

        this.validateServerButton.addActionListener(this::onValidateServerButtonClicked);

        this.enableAssigneesCheckBox.addItemListener(this::onDisableAssigneesItemStateChanged);

        this.openAccessTokenUrlButton.addActionListener(this::onOpenAccessTokenUrlButtonClicked);
    }

    private void bindToComponents(Settings settings) {
        this.urlTextField.setText(settings.getGitLabUri());
        this.accessTokenTextField.setText(settings.getAccessToken());
        this.targetBranchTextField.setText(settings.getDefaultTargetBranch());
        this.mergeRequestTitleTextField.setText(settings.getDefaultTitle());
        this.mergeRequestDescriptionTextArea.setText(settings.getDefaultDescription());
        this.labelsTextField.setText(settings.getDefaultLabels());
        this.assigneeListModel.replaceAll(settings.getDefaultAssignees());
        this.enableDefaultAssigneeActionCheckBox.setSelected(settings.isEnableMergeRequestToFavoriteAssignee());
        this.removeSourceBranchCheckbox.setSelected(settings.isRemoveSourceBranchOnMerge());
        this.enableAssigneesCheckBox.setSelected(settings.isAssigneesEnabled());
        this.assigneeList.setEnabled(settings.isAssigneesEnabled());
        this.showConfirmationDialogCheckBox.setSelected(settings.isShowConfirmationDialog());
        this.insecureTLSCheckBox.setSelected(settings.isInsecureTls());
    }

    private void onValidateServerButtonClicked(ActionEvent event) {
        if (StringUtils.isEmpty(this.urlTextField.getText())) {
            warnInvalidServer(new IllegalGitLabUrlException("GitLab URL cannot be empty"));
            return;
        }

        GitLab gitLab = new GitLab(this.urlTextField.getText(), String.valueOf(accessTokenTextField.getPassword()), insecureTLSCheckBox.isSelected());
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


    private void onDisableAssigneesItemStateChanged(ItemEvent event) {
        switch (event.getStateChange()) {
            case ItemEvent.SELECTED:
                this.assigneeList.setEnabled(true);
                break;
            case ItemEvent.DESELECTED:
                this.assigneeList.setEnabled(false);
                break;
        }
    }

    public JPanel getRootPanel() {
        return rootPanel;
    }

    private void onOpenAccessTokenUrlButtonClicked(ActionEvent e) {
        String url = this.urlTextField.getText();
        if (HttpUrl.parse(url) == null) {
            warnInvalidServer(new IllegalGitLabUrlException("Incorrect format of GitLab URL"));
        } else {
            String baseUrl = GitLab.getBaseUrl(url);
            String accessTokenUrl = GitLab.getAccessTokenWebPageUrl(baseUrl);
            this.browserLauncher.browse(URI.create(accessTokenUrl));
        }
    }

    //-------

    public void onAddAssignee(AnActionButton anActionButton) {
        GitLab gitLab = new GitLab(this.urlTextField.getText(), String.valueOf(accessTokenTextField.getPassword()), insecureTLSCheckBox.isSelected());
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
                    ApplicationManager.getApplication().invokeLater(() -> warnInvalidServer(t));
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

        if (this.enableAssigneesCheckBox.isSelected() && (this.assigneeListModel == null || this.assigneeListModel.isEmpty())) {
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
                .show(RelativePoint.getNorthWestOf(this.validateServerButton),
                        Balloon.Position.atRight);
    }

    private String getInvalidServerErrorMessage(Throwable throwable) {
        String defaultErrorMessage = "GitLab is not available. Please check URL or access token.";
        StringBuilder additionalErrorMessage = new StringBuilder("\n");
        Throwable cause = throwable;
        if (throwable instanceof CompletionException) {
            cause = throwable.getCause();
        }
        if (cause instanceof SSLPeerUnverifiedException) {
            defaultErrorMessage = "";
            additionalErrorMessage.setLength(0);
            additionalErrorMessage.append("SSL/TLS certificate is not valid.\nIf you are using a self-signed TLS certificate on GitLab, please check the 'Insecure TLS' checkbox");
        }

        if (cause instanceof IllegalGitLabUrlException) {
            defaultErrorMessage = "";
            additionalErrorMessage.setLength(0);
            additionalErrorMessage.append(cause.getMessage());
        }
        if (cause instanceof JsonSyntaxException) {
            defaultErrorMessage = "";
            additionalErrorMessage.setLength(0);
            additionalErrorMessage.append("This is not a valid GitLab V4 REST API URL\nServer URL must end with /api/v4. Example: http://gitlab.com/api/v4");
        }
        if (cause instanceof HttpResponseException) {
            HttpResponseException httpResponseException = (HttpResponseException) cause;
            additionalErrorMessage
                    .append("HTTP Status: ").append(httpResponseException.getStatusCode()).append("\n")
                    .append("HTTP Reply: ").append(httpResponseException.getMessage());
        }
        if (cause instanceof GitLabHttpResponseException) {
            GitLabHttpResponseException gitLabHttpResponseException = (GitLabHttpResponseException) cause;
            String responseBody = gitLabHttpResponseException.getResponseBody();
            if (gitLabHttpResponseException.getStatusCode() == 404) {
                defaultErrorMessage = "";
                additionalErrorMessage.append("GitLab V4 REST API not found in this URL\n");
                if (gitLabHttpResponseException.isHtmlContentType()) {
                    responseBody = StringEscapeUtils.escapeHtml(responseBody);
                }
            }
            if (responseBody.length() >= 128) {
                responseBody = responseBody.substring(0, 128) + "...";
            }
            additionalErrorMessage
                    .append("HTTP Status: ").append(gitLabHttpResponseException.getStatusCode()).append("\n")
                    .append("HTTP Reply: ").append(gitLabHttpResponseException.getMessage()).append("\n")
                    .append("HTTP Response: ").append(responseBody);
        }
        return defaultErrorMessage + additionalErrorMessage;
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
        boolean unmodified = SettingUtils.equals(this.urlTextField, settings.getGitLabUri())
                && !isAccessTokenModified()
                && SettingUtils.equals(this.targetBranchTextField, settings.getDefaultTargetBranch())
                && SettingUtils.equals(this.mergeRequestTitleTextField, settings.getDefaultTitle())
                && SettingUtils.equals(this.mergeRequestDescriptionTextArea, settings.getDefaultDescription())
                && SettingUtils.equals(this.labelsTextField, settings.getDefaultLabels())
                && this.enableDefaultAssigneeActionCheckBox.isSelected() == (settings.isEnableMergeRequestToFavoriteAssignee())
                && SettingUtils.hasSameUniqueElements(
                        this.assigneeListModel.getItems(),
                        settings.getDefaultAssignees())
                && this.enableAssigneesCheckBox.isSelected() == (settings.isAssigneesEnabled())
                && this.removeSourceBranchCheckbox.isSelected() == (settings.isRemoveSourceBranchOnMerge())
                && this.showConfirmationDialogCheckBox.isSelected() == (settings.isShowConfirmationDialog())
                && this.insecureTLSCheckBox.isSelected() == (settings.isInsecureTls())
                ;

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
        settings.setDefaultDescription(this.mergeRequestDescriptionTextArea.getText());
        settings.setDefaultLabels(this.labelsTextField.getText());
        settings.setEnableMergeRequestToFavoriteAssignee(this.enableDefaultAssigneeActionCheckBox.isSelected());
        settings.setRemoveSourceBranchOnMerge(this.removeSourceBranchCheckbox.isSelected());
        settings.setAssigneesEnabled(this.enableAssigneesCheckBox.isSelected());
        settings.setShowConfirmationDialog(this.showConfirmationDialogCheckBox.isSelected());
        settings.setInsecureTls(this.insecureTLSCheckBox.isSelected());
    }

    public static class ConfigurableProvider implements VcsConfigurableProvider {
        @Override
        public Configurable getConfigurable(Project project) {
            return new SettingsUi(project);
        }
    }
}
