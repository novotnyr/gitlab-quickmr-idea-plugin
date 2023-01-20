package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.User;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@State(name = Settings.NAME, storages = @Storage("gitlab-quickmr.xml"))
public class Settings implements PersistentStateComponent<Settings.State> {
    public static final String NAME = "gitlab-quickmr";

    private State state = new State();

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public boolean isInitialized() {
        boolean initialized = this.state.gitLabUri != null
                && this.getAccessToken() != null
                && this.state.defaultTargetBranch != null
                && this.state.defaultTitle != null
                && this.state.defaultDescription != null
                && this.state.defaultLabels != null
                ;
        if(this.state.assigneesEnabled) {
            return initialized && (this.state.defaultAssignees != null && !this.state.defaultAssignees.isEmpty());
        }
        return initialized;
    }

    public void reset() {
        this.state = new State();
    }

    public String getDefaultTitle() {
        return this.state.defaultTitle;
    }

    public void setDefaultTitle(String defaultTitle) {
        this.state.defaultTitle = defaultTitle;
    }

    public String getDefaultDescription() {
        return this.state.defaultDescription;
    }

    public void setDefaultDescription(String description) {
        this.state.defaultDescription = description;
    }

    public User getDefaultAssignee() {
        if (this.state.defaultAssignees == null || this.state.defaultAssignees.isEmpty()) {
            return null;
        }
        return this.state.defaultAssignees.get(0);
    }

    public Long getDefaultAssigneeId() {
        User defaultAssignee = getDefaultAssignee();
        if (defaultAssignee != null) {
            return defaultAssignee.getId();
        } else {
            return null;
        }
    }

    public List<User> getDefaultAssignees() {
        return this.state.defaultAssignees != null ? this.state.defaultAssignees : new ArrayList<>();
    }

    public void setDefaultAssignees(List<User> defaultAssignees) {
        this.state.defaultAssignees = defaultAssignees;
    }

    public boolean isSquashCommits() {
        return this.state.squashCommits;
    }

    public void setSquashCommits(boolean squashCommits) {
        this.state.squashCommits = squashCommits;
    }

    public String getGitLabUri() {
        return this.state.gitLabUri;
    }

    public void setGitLabUri(String gitLabUri) {
        this.state.gitLabUri = gitLabUri;
    }

    public String getAccessToken() {
        CredentialAttributes credentialAttributes = getCredentialAttributes();
        if (credentialAttributes == null) {
            return null;
        }
        Credentials credentials = PasswordSafe.getInstance().get(credentialAttributes);
        if (credentials == null) {
            return null;
        }
        return credentials.getPasswordAsString();
    }

    public void setAccessToken(String accessToken) {
        CredentialAttributes credentialAttributes = getCredentialAttributes();
        if (credentialAttributes == null) {
            return;
        }
        PasswordSafe.getInstance().setPassword(credentialAttributes, accessToken);
    }

    public String getDefaultTargetBranch() {
        return this.state.defaultTargetBranch;
    }

    public void setDefaultTargetBranch(String defaultTargetBranch) {
        this.state.defaultTargetBranch = defaultTargetBranch;
    }

    public String getDefaultLabels() {
        return this.state.defaultLabels;
    }

    public void setDefaultLabels(String defaultLabels) {
        this.state.defaultLabels = defaultLabels;
    }

    public boolean isEnableMergeRequestToFavoriteAssignee() {
        return this.state.enableMergeRequestToFavoriteAssignee;
    }

    public void setEnableMergeRequestToFavoriteAssignee(boolean enableMergeRequestToFavoriteAssignee) {
        this.state.enableMergeRequestToFavoriteAssignee = enableMergeRequestToFavoriteAssignee;
    }

    public boolean isRemoveSourceBranchOnMerge() {
        return this.state.removeSourceBranchOnMerge;
    }

    public void setRemoveSourceBranchOnMerge(boolean removeSourceBranchOnMerge) {
        this.state.removeSourceBranchOnMerge = removeSourceBranchOnMerge;
    }

    public boolean isAssigneesEnabled() {
        return this.state.assigneesEnabled;
    }

    public void setAssigneesEnabled(boolean assigneesEnabled) {
        this.state.assigneesEnabled = assigneesEnabled;
    }

    public boolean isShowConfirmationDialog() {
        return this.state.showConfirmationDialog;
    }

    public void setShowConfirmationDialog(boolean showConfirmationDialog) {
        this.state.showConfirmationDialog = showConfirmationDialog;
    }

    public boolean isInsecureTls() {
        return this.state.insecureTls;
    }

    public void setInsecureTls(boolean insecureTls) {
        this.state.insecureTls = insecureTls;
    }


    private CredentialAttributes getCredentialAttributes() {
        if (getGitLabUri() == null) {
            return null;
        }
        String serviceName = NAME + "\t" + getGitLabUri();
        String userName = getGitLabUri();
        return new CredentialAttributes(serviceName, userName, this.getClass(), false);
    }

    public static class State {
        public String gitLabUri;

        public List<User> defaultAssignees = new ArrayList<>();

        public String defaultTargetBranch;

        public String defaultTitle;

        public String defaultDescription;

        public String defaultLabels;

        public boolean enableMergeRequestToFavoriteAssignee = true;

        public boolean removeSourceBranchOnMerge;

        public boolean squashCommits = false;

        public boolean assigneesEnabled = true;

        public boolean showConfirmationDialog = false;

        public boolean insecureTls;

    }
}
