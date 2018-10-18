package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.User;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.Credentials;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import java.util.ArrayList;
import java.util.List;

@State(name = Settings.NAME, storages = @Storage("gitlab-quickmr.xml"))
public class Settings implements PersistentStateComponent<Settings.State> {
    public static final String NAME = "gitlab-quickmr";

    private State state = new State();

    private PasswordSafe passwordSafe = PasswordSafe.getInstance();

    @Override
    public State getState() {
        return this.state;
    }

    @Override
    public void loadState(State state) {
        this.state = state;
    }

    public boolean isInitialized() {
        return this.state.gitLabUri != null
                && this.getAccessToken() != null
                && this.state.defaultAssignees != null && ! this.state.defaultAssignees.isEmpty()
                && this.state.defaultTargetBranch != null
                && this.state.defaultTitle != null;
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
        Credentials credentials = this.passwordSafe.get(credentialAttributes);
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
        this.passwordSafe.setPassword(credentialAttributes, accessToken);
    }

    public String getDefaultTargetBranch() {
        return this.state.defaultTargetBranch;
    }

    public void setDefaultTargetBranch(String defaultTargetBranch) {
        this.state.defaultTargetBranch = defaultTargetBranch;
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

        public boolean enableMergeRequestToFavoriteAssignee = true;

        public boolean removeSourceBranchOnMerge;
    }
}
