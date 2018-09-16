package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.User;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@State(name = "gitlab-quickmr", storages = @Storage("gitlab-quickmr.xml"))
public class Settings implements PersistentStateComponent<Settings.State> {
    private State state = new State();

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
                && this.state.accessToken != null
                && this.state.defaultAssignee != null
                && this.state.defaultTargetBranch != null
                && this.state.defaultTitle != null;
    }

    public String getDefaultTitle() {
        return this.state.defaultTitle;
    }

    public void setDefaultTitle(String defaultTitle) {
        this.state.defaultTitle = defaultTitle;
    }

    public User getDefaultAssignee() {
        return this.state.defaultAssignee;
    }

    public Long getDefaultAssigneeId() {
        User defaultAssignee = this.state.defaultAssignee;
        if (defaultAssignee != null) {
            return defaultAssignee.getId();
        } else {
            return null;
        }
    }

    public void setDefaultAssignee(User defaultAssignee) {
        this.state.defaultAssignee = defaultAssignee;
    }

    public String getGitLabUri() {
        return this.state.gitLabUri;
    }

    public void setGitLabUri(String gitLabUri) {
        this.state.gitLabUri = gitLabUri;
    }

    public String getAccessToken() {
        return this.state.accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.state.accessToken = accessToken;
    }

    public String getDefaultTargetBranch() {
        return this.state.defaultTargetBranch;
    }

    public void setDefaultTargetBranch(String defaultTargetBranch) {
        this.state.defaultTargetBranch = defaultTargetBranch;
    }

    public static class State {
        public String gitLabUri;

        public String accessToken;

        public User defaultAssignee;

        public String defaultTargetBranch;

        public String defaultTitle;

    }
}
