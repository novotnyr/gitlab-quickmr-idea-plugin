package com.github.novotnyr.idea.gitlab;

public class VersionResponse {
    private String version;

    private String revision;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }
}
