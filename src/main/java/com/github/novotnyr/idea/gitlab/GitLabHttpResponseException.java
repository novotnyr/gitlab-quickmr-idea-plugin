package com.github.novotnyr.idea.gitlab;

import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

public class GitLabHttpResponseException extends RuntimeException {
    private final int statusCode;

    private final String responseMessage;

    private final GitLabHttpResponseBody responseBody;

    private final String contentType;

    public GitLabHttpResponseException(int statusCode, String responseMessage, String responseBody, String contentType) {
        super(responseMessage);
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
        this.responseBody = new GitLabHttpResponseBody(responseBody, contentType);
        this.contentType = contentType;
    }

    public GitLabHttpResponseException(int statusCode, String responseMessage, String responseBody, String contentType, Throwable cause) {
        super(responseMessage, cause);
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
        this.responseBody = new GitLabHttpResponseBody(responseBody, contentType);
        this.contentType = contentType;
    }

    public static GitLabHttpResponseException ofNullResponse(@NotNull Response response) {
        return new GitLabHttpResponseException(response.code(), response.message(), null, null);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public GitLabHttpResponseBody getResponseBody() {
        return responseBody;
    }

    public String getContentType() {
        return contentType;
    }

    public static boolean isHtmlContentType(String contentType) {
        return contentType != null && (
                contentType.startsWith("text/html")
                || contentType.startsWith("application/xhtml+xml")
        );
    }
}
