package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.GitLab;
import com.github.novotnyr.idea.gitlab.User;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.ui.components.JBTextField;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXButton;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

public class SettingsUi implements Configurable {
    private JBTextField uriTextField = new JBTextField();
    private JBPasswordField accessTokenTextField = new JBPasswordField();
    private JBLabel defaultAssigneeLabel = new JBLabel("none");
    private JXButton selectDefaultAssigneeButton = new JXButton("Select Default Assignee");
    private JBTextField defaultTargetBranchTextField = new JBTextField();
    private JBTextField defaultTitleTextField = new JBTextField();
    private JBLabel serverValidatedLabel = new JBLabel();

    private Project project;

    private User defaultAssignee;

    private boolean needsServerValidation = true;

    private DocumentListener needsServerValidationDocumentListener = new DocumentAdapter() {
        @Override
        protected void textChanged(DocumentEvent documentEvent) {
            SettingsUi.this.needsServerValidation = true;
            SettingsUi.this.serverValidatedLabel.setText("");
        }
    };

    public SettingsUi(Project project) {
        this.project = project;
    }

    @Nls(capitalization = Nls.Capitalization.Title)
    @Override
    public String getDisplayName() {
        return "GitLab Quick MR";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        Settings settings = ServiceManager.getService(this.project, Settings.class);
        this.defaultAssignee = settings.getDefaultAssignee();

        JBPanel panel = new JBPanel(new MigLayout("wrap 2", "[][grow, fill]"));

        panel.add(new JLabel("GitLab URL"));
        uriTextField.setText(settings.getGitLabUri());
        uriTextField.getEmptyText().setText("https://gitlab.com/api/v4");
        uriTextField.getDocument().addDocumentListener(this.needsServerValidationDocumentListener);
        panel.add(uriTextField);

        panel.add(new JLabel("Access Token"));
        accessTokenTextField.setText(settings.getAccessToken());
        accessTokenTextField.getDocument().addDocumentListener(this.needsServerValidationDocumentListener);
        panel.add(accessTokenTextField);

        panel.add(new JLabel());

        panel.add(this.serverValidatedLabel, "split 2");

        JButton validateButton = new JButton("Validate");
        validateButton.addActionListener(this::onValidateButtonClicked);
        panel.add(validateButton, "right, growx 0");

        panel.add(new JLabel("Default Assignee"));
        JBPanel assigneePanel = new JBPanel(new BorderLayout());
        panel.add(assigneePanel);

        if (settings.getDefaultAssignee() != null) {
            defaultAssigneeLabel.setText(settings.getDefaultAssignee().getName());
        }
        assigneePanel.add(defaultAssigneeLabel, BorderLayout.CENTER);

        selectDefaultAssigneeButton.addActionListener(this::onSelectDefaultAssigneeButtonActionPerformed);
        assigneePanel.add(selectDefaultAssigneeButton, BorderLayout.LINE_END);

        panel.add(new JLabel("Default Target Branch"));
        defaultTargetBranchTextField.setText(String.valueOf(settings.getDefaultTargetBranch()));
        panel.add(defaultTargetBranchTextField);

        panel.add(new JBLabel("Default Merge Request Title"));
        this.defaultTitleTextField.setText(settings.getDefaultTitle());
        panel.add(this.defaultTitleTextField);

        return panel;
    }

    private void onValidateButtonClicked(ActionEvent event) {
        GitLab gitLab = new GitLab(this.uriTextField.getText(), String.valueOf(accessTokenTextField.getPassword()));
        gitLab.version().thenRun(() -> {
                    this.serverValidatedLabel.setText("Server validated");
                })
                .exceptionally(t -> {
                    this.serverValidatedLabel.setText("Server credentials failed");
                    return null;
                });
    }

    @Override
    public void disposeUIResources() {
        uriTextField.getDocument().removeDocumentListener(this.needsServerValidationDocumentListener);
        accessTokenTextField.getDocument().removeDocumentListener(this.needsServerValidationDocumentListener);
    }

    public void onSelectDefaultAssigneeButtonActionPerformed(ActionEvent event) {
        GitLab gitLab = new GitLab(this.uriTextField.getText(), String.valueOf(accessTokenTextField.getPassword()));
        gitLab.version().thenRun(() -> {
                    ApplicationManagerEx.getApplicationEx().invokeLater(() -> {
                        SelectAssigneeDialog dialog = new SelectAssigneeDialog(this.project, gitLab);
                        if (dialog.showAndGet()) {
                            User assignee = dialog.getAssignee();
                            if (assignee != null) {
                                this.defaultAssignee = assignee;
                                defaultAssigneeLabel.setText(assignee.getName());
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
                            .show(RelativePoint.getNorthWestOf(this.selectDefaultAssigneeButton),
                                    Balloon.Position.atRight);
                    return null;
                });
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        List<String> validationErrors = new ArrayList<>();

        Settings settings = ServiceManager.getService(this.project, Settings.class);

        if (StringUtils.isNotEmpty(this.uriTextField.getText())) {
            settings.setGitLabUri(this.uriTextField.getText());
        } else {
            validationErrors.add("Missing Gitlab URI");
        }
        settings.setAccessToken(String.valueOf(this.accessTokenTextField.getPassword()));
        if (StringUtils.isNotEmpty(this.defaultTargetBranchTextField.getText())) {
            settings.setDefaultTargetBranch(this.defaultTargetBranchTextField.getText());
        } else {
            validationErrors.add("Missing default target branch");
        }
        if (this.defaultAssignee != null) {
            settings.setDefaultAssignee(this.defaultAssignee);
        } else {
            validationErrors.add("Default Assignee must be set");
        }
        settings.setDefaultTitle(this.defaultTitleTextField.getText());

        if (!validationErrors.isEmpty()) {
            throw new ConfigurationException("* " + String.join("<br />* ", validationErrors));
        }
    }
}
