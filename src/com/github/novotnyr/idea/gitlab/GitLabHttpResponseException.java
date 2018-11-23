package com.github.novotnyr.idea.gitlab;

public class GitLabHttpResponseException extends RuntimeException {
    private final int statusCode;

    private final String responseMessage;

    private final String responseBody;

    public GitLabHttpResponseException(int statusCode, String responseMessage, String responseBody) {
        super(responseMessage);
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
        this.responseBody = responseBody;
    }

    public GitLabHttpResponseException(int statusCode, String responseMessage, String responseBody, Throwable cause) {
        super(responseMessage, cause);
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseMessage() {
        return responseMessage;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
