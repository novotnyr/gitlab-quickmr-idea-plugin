package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.GitLab;
import com.github.novotnyr.idea.gitlab.User;
import com.github.novotnyr.idea.gitlab.quickmr.UserToStringConverter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ListSpeedSearch;
import com.intellij.ui.components.JBList;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import org.jdesktop.swingx.renderer.DefaultListRenderer;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.util.List;

public class SelectAssigneeDialog extends DialogWrapper {
    private final Project project;
    private final GitLab gitLab;

    private JPanel innerPanel;
    private JTextField userTextField;
    private JButton searchButton;
    private JLabel userLabel;
    private JBList userListView;

    private CollectionListModel<User> listModel = new CollectionListModel<User>();
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
        this.gitLab.searchUsers2(this.userTextField.getText(), 100)
                .thenAccept(this::updateUsers)
                .exceptionally(t -> {
                    this.userListView.getEmptyText().setText("Unable to load users");
                    this.userListView.setPaintBusy(false);
                    return null;
                });
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
