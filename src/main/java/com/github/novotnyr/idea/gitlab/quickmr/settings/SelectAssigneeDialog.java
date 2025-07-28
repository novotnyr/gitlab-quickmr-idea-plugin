package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.AbstractGitLabCommand;
import com.github.novotnyr.idea.gitlab.GitLab;
import com.github.novotnyr.idea.gitlab.User;
import com.github.novotnyr.idea.gitlab.quickmr.JBProgressIndicator;
import com.github.novotnyr.idea.gitlab.quickmr.UserToStringConverter;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.components.JBList;
import org.jdesktop.swingx.renderer.DefaultListRenderer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

public class SelectAssigneeDialog extends DialogWrapper {
    private final Project project;
    private final GitLab gitLab;

    private JPanel innerPanel;
    private JTextField userTextField;
    private JButton searchButton;
    private JLabel userLabel;
    private JBList userListView;

    private CollectionListModel<User> listModel = new CollectionListModel<>();
    private UserToStringConverter userToStringConverter = new UserToStringConverter();

    @SuppressWarnings("unchecked")
    public SelectAssigneeDialog(Project project, GitLab gitLab) {
        super(project, false);
        this.project = project;
        this.gitLab = gitLab;

        init();

        this.userListView.setModel(this.listModel);
        this.userListView.setCellRenderer(new DefaultListRenderer(userToStringConverter));
        new ListSpeedSearch(this.userListView, this.userToStringConverter);

        this.searchButton.addActionListener(this::onSearchButtonActionPerformed);

        getRootPane().setDefaultButton(this.searchButton);
    }

    public void onSearchButtonActionPerformed(ActionEvent event) {
        if (this.userTextField.getText() == null) {
            this.userListView.getEmptyText().setText("Username cannot be empty!");
            return;
        }
        this.userListView.getEmptyText().setText("");
        this.userListView.setPaintBusy(true);
        this.listModel.removeAll();
        this.gitLab.searchUsers(this.userTextField.getText(), 100, this::onProgressManager, new JBProgressIndicator())
                .thenAccept(this::updateUsers)
                .exceptionally(t -> {
                    this.userListView.getEmptyText().setText("Unable to load users");
                    this.userListView.setPaintBusy(false);
                    return null;
                });
    }

    private <T> CompletableFuture<T> onProgressManager(AbstractGitLabCommand<T> command) {
        final AtomicReference<CompletableFuture<T>> resultWrapper = new AtomicReference<>();

        Runnable runnable = () -> resultWrapper.set(command.call());
        try {
            ProgressManager.getInstance().runProcessWithProgressSynchronously(runnable, "Searching Users", true, this.project, this.getContentPanel());
            return resultWrapper.get();
        } catch (Exception e) {
            CompletableFuture<T> exceptionalFuture = new CompletableFuture<>();
            exceptionalFuture.completeExceptionally(e);
            return exceptionalFuture;
        }
    }

    private void updateUsers(List<User> users) {
        this.listModel.replaceAll(users);
        this.userListView.setPaintBusy(false);
    }

    @Override
    protected JComponent createCenterPanel() {
        return this.innerPanel;
    }

    public User getAssignee() {
        return (User) this.userListView.getSelectedValue();
    }

}
