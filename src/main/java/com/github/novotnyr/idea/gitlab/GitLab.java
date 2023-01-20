package com.github.novotnyr.idea.gitlab;

import com.github.novotnyr.idea.gitlab.http.HttpClientFactory;
import com.google.gson.Gson;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class GitLab {
    private String baseUri;

    private String privateToken;

    private OkHttpClient httpClient;

    private Gson gson = new Gson();

    public GitLab(String baseUri, String privateToken) {
        this(baseUri, privateToken, false);
    }

    public GitLab(String baseUri, String privateToken, boolean allowSelfSignedTls) {
        this.baseUri = baseUri;
        this.privateToken = privateToken;

        HttpClientFactory httpClientFactory = HttpClientFactory.getInstance();
        this.httpClient = allowSelfSignedTls ? httpClientFactory.getInsecureHttpClient() : httpClientFactory.getHttpClient();
    }

    public CompletableFuture<VersionResponse> version() {
        String url = this.baseUri + "/version";

        CompletableFuture<VersionResponse> result = new CompletableFuture<>();
        if (HttpUrl.parse(url) == null) {
            result.completeExceptionally(new HttpResponseException(500, "Incorrect GitLab URL"));
            return result;
        }
        if (this.baseUri.endsWith("/")) {
            result.completeExceptionally(new HttpResponseException(500, "Remove last slash from URL"));
            return result;
        }

        Request request = prepareRequest("/version").build();
        this.httpClient.newCall(request)
                .enqueue(new JsonHttpResponseCallback<VersionResponse>(VersionResponse.class, result, this.gson) {
                    @Override
                    protected void onRawResponseBody(Response response, String rawResponseBodyString) {
                        if (response.code() != 200) {
                            String contentType = getContentType(response);
                            result.completeExceptionally(new GitLabHttpResponseException(response.code(), response.message(), rawResponseBodyString, contentType));
                        } else {
                            super.onRawResponseBody(response, rawResponseBodyString);
                        }
                    }

                });
        return result;
    }

    public CompletableFuture<List<GitLabProject>> searchProject(String projectName) {
        String url = this.baseUri + "/projects?search=" + projectName;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Private-Token", this.privateToken)
                .get()
                .build();

        CompletableFuture<List<GitLabProject>> result = new CompletableFuture<>();

        Call call = httpClient.newCall(request);
        call.enqueue(JsonHttpResponseCallback.ofList(result, gson));
        return result;
    }

    public CompletableFuture<List<User>> searchUsers2(String username, int batchSize, CommandExecutor commandExecutor, ProgressIndicator progressIndicator) {
        SearchUsersGitLabCommand command = new SearchUsersGitLabCommand(this.baseUri, this.privateToken, this.httpClient, this.gson, progressIndicator, username);
        return commandExecutor.execute(command);
    }

    public CompletableFuture<List<User>> searchUsers(String username) {
        Request request = prepareRequest("/users?username=" + username + "&per_page=300&active=true")
                .build();

        CompletableFuture<List<User>> result = new CompletableFuture<>();
        Call call = httpClient.newCall(request);
        call.enqueue(JsonHttpResponseCallback.ofList(result, gson));
        return result;
    }

    public CompletableFuture<List<User>> listUsers() {
        Request request = prepareRequest("/users?per_page=300")
                .build();

        CompletableFuture<List<User>> result = new CompletableFuture<>();

        Call call = httpClient.newCall(request);
        call.enqueue(JsonHttpResponseCallback.ofList(result, gson));
        return result;
    }

    public CompletableFuture<User> findUserByName(String username) {
        Request request = prepareRequest("/users?username=" + username)
                .build();

        CompletableFuture<List<User>> result = new CompletableFuture<>();

        httpClient
                .newCall(request)
                .enqueue(JsonHttpResponseCallback.ofList(result, this.gson));
        return result.thenApply(users -> users.get(0));
    }

    public CompletableFuture<MergeRequestResponse> createMergeRequest(String gitLabProjectId, MergeRequestRequest mergeRequestRequest) {
        return doCreateMergeRequest(ProjectId.of(gitLabProjectId), mergeRequestRequest);
    }

    public CompletableFuture<MergeRequestResponse> createMergeRequest(long projectId, MergeRequestRequest mergeRequestRequest) {
        return doCreateMergeRequest(ProjectId.of(projectId), mergeRequestRequest);
    }

    protected CompletableFuture<MergeRequestResponse> doCreateMergeRequest(ProjectId projectId, MergeRequestRequest mergeRequestRequest) {
        String url = this.baseUri + "/projects/" + projectId.getUrlEncoded() + "/merge_requests";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Private-Token", this.privateToken)
                .post(RequestBody.create(MediaType.parse("application/json"), this.gson.toJson(mergeRequestRequest)))
                .build();

        CompletableFuture<MergeRequestResponse> result = new CompletableFuture<>();

        Call call = httpClient.newCall(request);
        call.enqueue(new JsonHttpResponseCallback<MergeRequestResponse>(MergeRequestResponse.class, result, this.gson) {
            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 409) {
                    completeExceptionally(result, new DuplicateMergeRequestException(), response);
                    return;
                }
                if (response.code() == 400) {
                    completeExceptionally(result, new BadMergeRequestException(response.message()), response);
                    return;
                }
                if (response.code() == 403 || response.code() == 401) {
                    completeExceptionally(result, new AccessDeniedException(), response);
                    return;
                }

                if (!isJson(response)) {
                    completeExceptionally(result, new UnsupportedContentTypeException("GitLab API is misconfigured. Only JSON replies are supported"), response);
                    return;
                }
                try(ResponseBody body = response.body()) {
                    String json = body.string();
                    logRawResponseBody(response, json);
                    if (isGitLabProjectNotFound(response, json)) {
                        result.completeExceptionally(GitLabProjectNotFoundException.of(projectId));
                        return;
                    }
                    MergeRequestResponse mergeRequestResponse = gson.fromJson(json, MergeRequestResponse.class);
                    result.complete(mergeRequestResponse);
                }
            }
        });

        return result;
    }

    private boolean isGitLabProjectNotFound(Response response, String json) {
        // {"message":"404 Project Not Found"}
        return 404 == response.code() && json.contains("Project Not Found");
    }

    private boolean isJson(Response response) {
        String contentTypeHeader = response.header("Content-Type");
        MediaType contentType = MediaType.parse(contentTypeHeader);
        return "application".equals(contentType.type()) && "json".equals(contentType.subtype());
    }

    protected Request.Builder prepareRequest(String urlSuffix) {
        return new Request.Builder()
                .url(this.baseUri + urlSuffix)
                .addHeader("Private-Token", this.privateToken);
    }

    public static String getBaseUrl(String gitLabRestApiUrl) {
        String url = gitLabRestApiUrl;
        url = StringUtils.removeEnd(url, "/");
        url = StringUtils.removeEnd(url, "/api/v4");
        return url;
    }

    protected String getContentType(Response response) {
        List<String> headers = response.headers("Content-Type");
        if (headers.isEmpty()) {
            return "application/octet-stream";
        }
        return headers.get(0);
    }

    public static String getAccessTokenWebPageUrl(String gitLabBaseUrl) {
        return gitLabBaseUrl + "/profile/personal_access_tokens";
    }
}
