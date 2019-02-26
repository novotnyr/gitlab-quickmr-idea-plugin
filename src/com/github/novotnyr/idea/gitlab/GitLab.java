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
import com.squareup.okhttp.internal.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.HttpResponseException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

        Request request = prepareRequest("/version").build();
        this.httpClient.newCall(request)
                .enqueue(new JsonHttpResponseCallback<VersionResponse>(VersionResponse.class, result, this.gson) {
                    @Override
                    protected void onRawResponseBody(Response response, String rawResponseBodyString) {
                        if (response.code() != 200) {
                            result.completeExceptionally(new GitLabHttpResponseException(response.code(), response.message(), rawResponseBodyString));
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

    public CompletableFuture<List<User>> searchUsers2(String username, int batchSize) {
        CompletableFuture<List<User>> result = new CompletableFuture<>();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                List<UserBatch> allUsers = new ArrayList<>();
                int page = 1;
                while (true) {
                    int perPage = 5;
                    Request request = prepareRequest(String.format("/users?search=%s&page=%s&per_page=%s&active=true", username, page, perPage))
                            .build();

                    System.out.println(request);

                    Call call = httpClient.newCall(request);
                    ResponseBody body = null;
                    UserBatch userBatch = null;
                    try {
                        Response response = call.execute();
                        if (response.code() != 200) {
                            result.completeExceptionally(new IOException("Wrong API call"));
                            return;
                        }
                        body = response.body();
                        String json = body.string();
                        User[] users = gson.fromJson(json, User[].class);
                        page = Integer.parseInt(response.header("X-Page"));
                        int totalPages = Integer.parseInt(response.header("X-Total-Pages"));
                        userBatch = new UserBatch(users, page, totalPages);
                        allUsers.add(userBatch);
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    } finally {
                        Util.closeQuietly(body);
                    }
                    if (userBatch != null) {
                        if (userBatch.isLastPage()) {
                            break;
                        }
                    }
                    page++;
                }

                List<User> r = new ArrayList<>();
                for (UserBatch allUser : allUsers) {
                    r.addAll(allUser.getUsers());
                }

                result.complete(r);
            }
        });

        return result;
    }

    private static class UserBatch {
        private List<User> users = new ArrayList<>();

        private int page;

        private int totalPages;

        public UserBatch(User[] users, int page, int totalPages) {
            this.users = Arrays.asList(users);
            this.page = page;
            this.totalPages = totalPages;
        }

        public List<User> getUsers() {
            return users;
        }

        public boolean isLastPage() {
            return this.page == this.totalPages;
        }
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
        try {
            String urlEncodedProjectId = URLEncoder.encode(gitLabProjectId, "UTF-8");
            return doCreateMergeRequest(urlEncodedProjectId, mergeRequestRequest);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException("Unable to url encode input", e);
        }
    }

    public CompletableFuture<MergeRequestResponse> createMergeRequest(long projectId, MergeRequestRequest mergeRequestRequest) {
        return doCreateMergeRequest(String.valueOf(projectId), mergeRequestRequest);
    }

    protected CompletableFuture<MergeRequestResponse> doCreateMergeRequest(String urlEncodedProjectId, MergeRequestRequest mergeRequestRequest) {
        String url = this.baseUri + "/projects/" + urlEncodedProjectId + "/merge_requests";

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
                    result.completeExceptionally(new DuplicateMergeRequestException());
                    return;
                }
                if (response.code() == 400) {
                    result.completeExceptionally(new BadMergeRequestException(response.message()));
                }
                try(ResponseBody body = response.body()) {
                    String json = body.string();
                    MergeRequestResponse mergeRequestResponse = gson.fromJson(json, MergeRequestResponse.class);
                    result.complete(mergeRequestResponse);
                }
            }
        });

        return result;
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

    public static String getAccessTokenWebPageUrl(String gitLabBaseUrl) {
        return gitLabBaseUrl + "/profile/personal_access_tokens";
    }
}
