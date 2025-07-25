package com.github.novotnyr.idea.gitlab;

import com.intellij.xml.util.XmlStringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GitLabHttpResponseBody {
    public enum Type {
        HTML, UNKNOWN
    }

    @NotNull
    private String body;

    private boolean isUnavailable;

    private Type type;

    public GitLabHttpResponseBody(@Nullable String responseBody, String contentType) {
        this(responseBody, isHtmlContentType(contentType) ? Type.HTML : Type.UNKNOWN);
    }


    public GitLabHttpResponseBody(@Nullable String responseBody, Type type) {
        this.type = type;
        if (responseBody == null) {
            this.isUnavailable = true;
            this.body = "";
        } else {
            this.body = responseBody;
        }
    }

    public String asHtml() {
        if (this.isUnavailable) {
            return "<Missing Response Body>";
        }
        if (this.body.isBlank()) {
            return "<No Response Body>";
        }
        String html = this.body;
        if (this.type == Type.HTML) {
            html = XmlStringUtil.escapeString(html);
        }
        if (this.body.length() >= 128) {
            html = html.substring(0, 128) + "...";
        }
        return "<pre>" + html + "</pre>";
    }

    public boolean containsCaseInsensitive(String value) {
        return this.body.toLowerCase().contains(value.toLowerCase());
    }

    public static boolean isHtmlContentType(String contentType) {
        return contentType != null && (
                contentType.startsWith("text/html")
                        || contentType.startsWith("application/xhtml+xml")
        );
    }
}
