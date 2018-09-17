package com.github.novotnyr.idea.gitlab.quickmr.settings;

import com.github.novotnyr.idea.gitlab.User;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

import java.util.ArrayList;
import java.util.List;

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
                && this.state.defaultAssignees != null && ! this.state.defaultAssignees.isEmpty()
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

        public List<User> defaultAssignees = new ArrayList<>();

        public String defaultTargetBranch;

        public String defaultTitle;
    }
}
