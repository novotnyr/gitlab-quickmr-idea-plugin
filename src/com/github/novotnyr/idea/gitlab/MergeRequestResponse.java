package com.github.novotnyr.idea.gitlab;

import com.google.gson.annotations.SerializedName;

public class MergeRequestResponse {
    @SerializedName("web_url")
    private String webUrl;

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }
}
