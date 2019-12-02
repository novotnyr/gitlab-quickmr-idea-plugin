package com.github.novotnyr.idea.gitlab;

public class GitLabHttpResponseException extends RuntimeException {
    private final int statusCode;

    private final String responseMessage;

    private final String responseBody;

    private final String contentType;

    public GitLabHttpResponseException(int statusCode, String responseMessage, String responseBody, String contentType) {
        super(responseMessage);
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
        this.responseBody = responseBody;
        this.contentType = contentType;
    }

    public GitLabHttpResponseException(int statusCode, String responseMessage, String responseBody, String contentType, Throwable cause) {
        super(responseMessage, cause);
        this.statusCode = statusCode;
        this.responseMessage = responseMessage;
        this.responseBody = responseBody;
        this.contentType = contentType;
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

    public String getContentType() {
        return contentType;
    }

    public boolean isHtmlContentType() {
        return this.contentType != null && (
                this.contentType.startsWith("text/html")
                || this.contentType.startsWith("application/xhtml+xml")
        );
    }
}
