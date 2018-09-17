package com.github.novotnyr.idea.gitlab;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;
import com.squareup.okhttp.internal.Util;
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
        this.baseUri = baseUri;
        this.privateToken = privateToken;

        this.httpClient = new OkHttpClient();
    }

    public CompletableFuture<Boolean> version() {
        CompletableFuture<Boolean> result = new CompletableFuture<>();

        String url = this.baseUri + "/version";

        if (HttpUrl.parse(url) == null) {
            result.completeExceptionally(new HttpResponseException(500, "Incorrect Gitlab URL"));
            return result;
        }

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Private-Token", this.privateToken)
                .get()
                .build();


        Call call = httpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                result.completeExceptionally(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String json = response.body().string();
                if (response.code() == 200) {
                    result.complete(true);
                } else {
                    result.completeExceptionally(new HttpResponseException(response.code(), response.message()));
                }
            }
        });

        return result;
    }

    public CompletableFuture<GitLabProject> searchProject(String projectName) {
        String url = this.baseUri + "/projects?search=" + projectName;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Private-Token", this.privateToken)
                .get()
                .build();

        CompletableFuture<GitLabProject> result = new CompletableFuture<>();

        Call call = httpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                result.completeExceptionally(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                String json = response.body().string();
                GitLabProject[] projects = gson.fromJson(json, GitLabProject[].class);
                if (projects.length > 0) {
                    result.complete(projects[0]);
                } else {
                    result.complete(new GitLabProject());
                }
            }
        });

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
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                result.completeExceptionally(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    String json = response.body().string();
                    User[] users = gson.fromJson(json, User[].class);
                    result.complete(Arrays.asList(users));
                } catch (JsonSyntaxException e) {
                    result.completeExceptionally(e);
                }
            }
        });
        return result;
    }

    public CompletableFuture<List<User>> listUsers() {
        Request request = prepareRequest("/users?per_page=300")
                .build();

        CompletableFuture<List<User>> result = new CompletableFuture<>();

        Call call = httpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                result.completeExceptionally(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    String json = response.body().string();
                    User[] users = gson.fromJson(json, User[].class);
                    result.complete(Arrays.asList(users));
                } catch (JsonSyntaxException e) {
                    result.completeExceptionally(e);
                }
            }
        });
        return result;
    }

    public CompletableFuture<User> findUserByName(String username) {
        Request request = prepareRequest("/users?username=" + username)
                .build();

        CompletableFuture<User> result = new CompletableFuture<>();

        Call call = httpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                result.completeExceptionally(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                try {
                    String json = response.body().string();
                    User[] users = gson.fromJson(json, User[].class);
                    if (users.length > 0) {
                        result.complete(users[0]);
                    } else {
                        result.complete(new User());
                    }
                } catch (JsonSyntaxException e) {
                    result.completeExceptionally(e);
                }
            }
        });
        return result;
    }

    public CompletableFuture<MergeRequestResponse> createMergeRequest(String projectId, MergeRequestRequest mergeRequestRequest) {
        try {
            String urlEncodedProjectId = URLEncoder.encode(projectId, "UTF-8");
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
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                result.completeExceptionally(e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 409) {
                    result.completeExceptionally(new DuplicateMergeRequestException());
                    return;
                }
                String json = response.body().string();
                System.out.println(json);
                MergeRequestResponse mergeRequestResponse = gson.fromJson(json, MergeRequestResponse.class);
                result.complete(mergeRequestResponse);
            }
        });

        return result;
    }

    protected Request.Builder prepareRequest(String urlSuffix) {
        return new Request.Builder()
                .url(this.baseUri + urlSuffix)
                .addHeader("Private-Token", this.privateToken);
    }
}
