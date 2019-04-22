package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.git.GitService;
import com.github.novotnyr.idea.gitlab.MergeRequestRequest;
import com.github.novotnyr.idea.gitlab.User;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.ListCellRendererWrapper;
import git4idea.GitLocalBranch;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.Objects;
import java.util.Optional;

public class ConfirmMergeRequestDialog extends DialogWrapper {
    private static final boolean VERTICAL = true;

    private JPanel rootPanel;

    private JTextField titleTextField;
    private JComboBox<String> targetBranchComboBox;
    private JComboBox<User> assigneeComboBox;
    private JLabel sourceBranchLabel;
    private JTextArea descriptionTextArea;
    private JPanel hideableDescriptionPanel;

    private GitService gitService = new GitService();

    public ConfirmMergeRequestDialog(MergeRequestRequest request, SelectedModule module) {
        super(true);
        init();
        setTitle("Create Merge Request");
        this.titleTextField.setText(request.getTitle());
        this.sourceBranchLabel.setText(String.format("<html>Merge from <b>%s</b> to</html>", request.getSourceBranch()));

        setTargetBranchComboBoxModel(request, module);
        setAssigneeComboBoxModel(request, module);
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
        if (this.titleTextField.getText().isEmpty()) {
            return new ValidationInfo("Merge Request title must be provided", this.titleTextField);
        }
        return super.doValidate();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.rootPanel;
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
        return getClass().getName();
    }

    public String getMergeRequestTitle() {
        return this.titleTextField.getText();
    }

    public Optional<String> getMergeRequestDescription() {
        String text = this.descriptionTextArea.getText();
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(text);
        }
    }

    public String getTargetBranch() {
        return (String) this.targetBranchComboBox.getSelectedItem();
    }

    public Optional<User> getAssignee() {
        User assignee = (User) this.assigneeComboBox.getSelectedItem();
        if (User.NONE.equals(assignee)) {
            return Optional.empty();
        }
        return Optional.ofNullable(assignee);
    }

    private void setTargetBranchComboBoxModel(MergeRequestRequest request, SelectedModule module) {
        DefaultComboBoxModel<String> targetBranchComboBoxModel = new DefaultComboBoxModel<>();
        for (GitLocalBranch localBranch : this.gitService.getLocalBranches(module.getProject(), module.getFile())) {
            targetBranchComboBoxModel.addElement(localBranch.getName());
        }
        this.targetBranchComboBox.setModel(targetBranchComboBoxModel);
        this.targetBranchComboBox.setSelectedItem(request.getTargetBranch());
    }

    private void setAssigneeComboBoxModel(MergeRequestRequest request, SelectedModule module) {
        DefaultComboBoxModel<User> model = new DefaultComboBoxModel<>();
        Settings settings = ServiceManager.getService(module.getProject(), Settings.class);
        model.addElement(User.NONE);
        User implicitAssignee = User.NONE;
        for (User defaultAssignee : settings.getDefaultAssignees()) {
            model.addElement(defaultAssignee);
            if (Objects.equals(request.getAssigneeId(), defaultAssignee.getId())) {
                implicitAssignee = defaultAssignee;
            }
        }
        this.assigneeComboBox.setModel(model);
        this.assigneeComboBox.setSelectedItem(implicitAssignee);
        this.assigneeComboBox.setRenderer(new ListCellRendererWrapper<User>() {
            @Override
            public void customize(JList list, User user, int index, boolean selected, boolean hasFocus) {
                if (User.NONE.equals(user)) {
                    setText("- none -");
                    return;
                }
                String renderedText = user.getName() + " (" + user.getUsername() + ")";
                setText(renderedText);
            }
        });
    }
}
