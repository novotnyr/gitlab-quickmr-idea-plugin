package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.gitlab.MergeRequestRequest;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ConfirmMergeRequestDialog extends DialogWrapper {
    private JPanel rootPanel;

    private JTextField titleTextField;
    private JTextField targetBranchTextField;
    private JTextField sourceBranchTextField;

    public ConfirmMergeRequestDialog(MergeRequestRequest request) {
        super(true);
        init();
        setTitle("Create Merge Request");
        this.titleTextField.setText(request.getTitle());
        this.sourceBranchTextField.setText(request.getSourceBranch());
        this.targetBranchTextField.setText(request.getTargetBranch());
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
}
