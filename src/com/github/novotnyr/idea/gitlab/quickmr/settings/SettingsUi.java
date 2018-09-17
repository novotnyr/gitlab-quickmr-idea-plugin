package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.GitLab;
import com.github.novotnyr.idea.gitlab.User;
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

    private CollectionListModel<User> assigneeListModel = new CollectionListModel<>();

    public SettingsUi(Project project) {
        this.project = project;
        Settings settings = ServiceManager.getService(this.project, Settings.class);

        this.urlTextField.getEmptyText().setText("https://gitlab.com/api/v4");

        this.assigneeList = new JBList<>();
        this.assigneeList.setModel(this.assigneeListModel);
        this.assigneeList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                User user = (User) value;
                String renderedText = user.getName() + " (" + user.getUsername() + ")";
                if (index == 0) {
                    renderedText = renderedText + " [favourite assignee]";
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

        this.urlTextField.setText(settings.getGitLabUri());
        this.accessTokenTextField.setText(settings.getAccessToken());
        this.targetBranchTextField.setText(settings.getDefaultTargetBranch());
        this.mergeRequestTitleTextField.setText(settings.getDefaultTitle());
        this.assigneeListModel.replaceAll(settings.getDefaultAssignees());
        this.enableDefaultAssigneeActionCheckBox.setSelected(settings.isEnableMergeRequestToFavoriteAssignee());
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
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder("Server is not available. Please check URL or access token", MessageType.ERROR, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(this.validateServerButton),
                                    Balloon.Position.atRight);

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
                    JBPopupFactory.getInstance()
                            .createHtmlTextBalloonBuilder("Server is not available. Please check URL or access token", MessageType.ERROR, null)
                            .setFadeoutTime(7500)
                            .createBalloon()
                            .show(RelativePoint.getNorthWestOf(this.assigneeList),
                                    Balloon.Position.atRight);
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
            validationErrors.add("Missing Gitlab URI");
        }
        if (!StringUtils.isNotEmpty(this.targetBranchTextField.getText())) {
            validationErrors.add("Missing default target branch");
        }
        if (this.assigneeListModel == null || this.assigneeListModel.isEmpty()) {
            validationErrors.add("Please set at least one assignee");
        }
        return validationErrors;
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
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        List<String> validationErrors = validate();
        if (!validationErrors.isEmpty()) {
            throw new ConfigurationException("<li>" + String.join("<li>", validationErrors));
        }

        Settings settings = ServiceManager.getService(this.project, Settings.class);

        settings.setGitLabUri(this.urlTextField.getText());
        settings.setAccessToken(String.valueOf(this.accessTokenTextField.getPassword()));
        settings.setDefaultTargetBranch(this.targetBranchTextField.getText());
        settings.setDefaultAssignees(this.assigneeListModel.getItems());
        settings.setDefaultTitle(this.mergeRequestTitleTextField.getText());
        settings.setEnableMergeRequestToFavoriteAssignee(this.enableDefaultAssigneeActionCheckBox.isSelected());
    }

    public static class ConfigurableProvider implements VcsConfigurableProvider {
        @Override
        public Configurable getConfigurable(Project project) {
            return new SettingsUi(project);
        }
    }

}
