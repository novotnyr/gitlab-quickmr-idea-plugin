package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.git.GitService;
import com.github.novotnyr.idea.gitlab.MergeRequestRequest;
import com.github.novotnyr.idea.gitlab.User;
import com.github.novotnyr.idea.gitlab.quickmr.settings.Settings;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.CollectionComboBoxModel;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.components.JBTextField;
import git4idea.GitLocalBranch;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class ConfirmMergeRequestDialog extends DialogWrapper {
    private static final boolean VERTICAL = true;

    private JPanel rootPanel;

    private JTextField titleTextField;
    private JComboBox<String> targetBranchComboBox;
    private JComboBox<User> assigneeComboBox;
    private JLabel sourceBranchLabel;
    private JTextArea descriptionTextArea;
    private JBTextField labelsTextField;
    private JCheckBox squashCommitsCheckBox;
    private JPanel hideableDescriptionPanel;

    private GitService gitService = new GitService();

    public ConfirmMergeRequestDialog(MergeRequestRequest request, SelectedModule module) {
        super(true);
        init();
        setTitle("Create Merge Request");
        this.titleTextField.setText(request.getTitle());
        this.descriptionTextArea.setText(request.getDescription());
        this.sourceBranchLabel.setText(String.format("<html>Merge from <b>%s</b> to</html>", request.getSourceBranch()));

        this.labelsTextField.setText(request.getLabels());
        this.labelsTextField.getEmptyText().setText("labels,are,comma,separated");

        setTargetBranchComboBoxModel(request, module);
        setAssigneeComboBoxModel(request, module);

        this.squashCommitsCheckBox.setSelected(request.isSquash());
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

    public Optional<String> getMergeRequestLabels() {
        String text = this.labelsTextField.getText();
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
        Task.Modal task = new Task.Modal(module.getProject(), "Retrieving Git branch info", true) {
            final List<String> localBranches = new CopyOnWriteArrayList<>();

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                GitService git = ConfirmMergeRequestDialog.this.gitService;
                for (GitLocalBranch localBranch : git.getLocalBranches(module.getProject(), module.getFile())) {
                    localBranches.add(localBranch.getName());
                }
            }

            @Override
            public void onSuccess() {
                JComboBox<String> component = ConfirmMergeRequestDialog.this.targetBranchComboBox;
                component.setModel(new CollectionComboBoxModel<>(localBranches, request.getTargetBranch()));
            }
        };
        task.queue();
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
        this.assigneeComboBox.setRenderer(new ListCellRendererWrapper<>() {
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
