package com.github.novotnyr.idea.gitlab;

import com.google.gson.Gson;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import static com.github.novotnyr.idea.gitlab.GitLab.PRIVATE_TOKEN_HEADER;

public abstract class AbstractGitLabCommand<T> implements Callable<CompletableFuture<T>>  {
    protected final String baseUri;

    protected final String privateToken;

    protected final OkHttpClient httpClient;

    protected final Gson gson;

    public AbstractGitLabCommand(String baseUri, String privateToken, OkHttpClient httpClient, Gson gson) {
        this.baseUri = baseUri;
        this.privateToken = privateToken;
        this.httpClient = httpClient;
        this.gson = gson;
    }

    protected Request.Builder prepareRequest(String urlSuffix) {
        return new Request.Builder()
                .url(this.baseUri + urlSuffix)
                .addHeader(PRIVATE_TOKEN_HEADER, this.privateToken);
    }

    @Override
    public abstract CompletableFuture<T> call();
}
