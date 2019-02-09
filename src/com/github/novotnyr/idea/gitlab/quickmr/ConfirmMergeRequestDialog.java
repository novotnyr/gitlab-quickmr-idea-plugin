package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.git.GitService;
import com.github.novotnyr.idea.gitlab.MergeRequestRequest;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import git4idea.GitLocalBranch;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ConfirmMergeRequestDialog extends DialogWrapper {
    private JPanel rootPanel;

    private JTextField titleTextField;
    private JTextField sourceBranchTextField;
    private JComboBox<String> targetBranchComboBox;

    private GitService gitService = new GitService();

    public ConfirmMergeRequestDialog(MergeRequestRequest request, SelectedModule module) {
        super(true);
        init();
        setTitle("Create Merge Request");
        this.titleTextField.setText(request.getTitle());
        this.sourceBranchTextField.setText(request.getSourceBranch());

        DefaultComboBoxModel<String> targetBranchComboBoxModel = new DefaultComboBoxModel<>();
        for (GitLocalBranch localBranch : this.gitService.getLocalBranches(module.getProject(), module.getFile())) {
            targetBranchComboBoxModel.addElement(localBranch.getName());
        }
        this.targetBranchComboBox.setModel(targetBranchComboBoxModel);
        this.targetBranchComboBox.setSelectedItem(request.getTargetBranch());
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

    public String getTargetBranch() {
        return (String) this.targetBranchComboBox.getSelectedItem();
    }
}
