package com.github.novotnyr.idea.gitlab;

import com.google.gson.Gson;
import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.internal.Util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static com.github.novotnyr.idea.gitlab.HttpUtils.assertHasBody;

public class SearchUsersGitLabCommand extends AbstractGitLabCommand<List<User>> {

    private final ProgressIndicator progressIndicator;

    private final String username;

    public SearchUsersGitLabCommand(String baseUri, String privateToken, OkHttpClient httpClient, Gson gson, ProgressIndicator progressIndicator, String username) {
        super(baseUri, privateToken, httpClient, gson);
        this.progressIndicator = progressIndicator;
        this.username = username;
    }

    @Override
    public CompletableFuture<List<User>> call() {
        CompletableFuture<List<User>> result = new CompletableFuture<>();
        this.progressIndicator.setPercents(0);

        List<UserBatch> allUsers = new ArrayList<>();
        int page = 1;
        while (true) {
            if (this.progressIndicator.isCancelled()) {
                return emptyResult();
            }

            int perPage = 25;
            Request request = prepareRequest(String.format("/users?search=%s&page=%s&per_page=%s&active=true", this.username, page, perPage))
                    .build();

            Call call = httpClient.newCall(request);
            ResponseBody body = null;
            UserBatch userBatch = null;
            try {
                Response response = call.execute();
                if (response.code() != 200) {
                    result.completeExceptionally(new IOException("Wrong API call"));
                    return result;
                }
                body = assertHasBody(response, response.body());
                String json = body.string();
                User[] users = gson.fromJson(json, User[].class);
                //noinspection DataFlowIssue
                page = Integer.parseInt(response.header("X-Page", "1"));
                String xTotalPages = response.header("X-Total-Pages");
                int totalPages = Integer.MAX_VALUE;
                if (xTotalPages != null) {
                    totalPages = Integer.parseInt(xTotalPages);
                    this.progressIndicator.setPercents(page/(float) totalPages);
                } else {
                    this.progressIndicator.setPercents(-1);
                }
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
        return result;
    }

    private CompletableFuture<List<User>> emptyResult() {
        CompletableFuture<List<User>> result = new CompletableFuture<>();
        result.complete(Collections.emptyList());
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
}
