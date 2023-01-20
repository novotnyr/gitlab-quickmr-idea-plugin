package com.github.novotnyr.idea.gitlab;

import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

public abstract class HttpUtils {
    @NotNull
    public static ResponseBody requireBody(Response response, ResponseBody body) {
        if (body == null) {
            throw GitLabHttpResponseException.ofNullResponse(response);
        }
        return body;
    }
}
