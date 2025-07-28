package com.github.novotnyr.idea.gitlab;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.concurrent.CompletableFuture;

import static com.github.novotnyr.idea.gitlab.HttpUtils.assertHasBody;

public abstract class TypedJsonHttpResponseCallback<T> implements Callback {
    protected final Logger log = Logger.getInstance("#" + TypedJsonHttpResponseCallback.class.getName());

    private final Gson gson;

    public TypedJsonHttpResponseCallback(Gson gson) {
        this.gson = gson;
    }

    @Override
    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
        try(ResponseBody body = response.body()) {
            String json = assertHasBody(response, body).string();
            onRawResponseBody(response, json);

            T deserializedJson = this.gson.fromJson(json, getTypeToken());
            complete(handleResponse(response, body, json, deserializedJson));
        } catch (JsonSyntaxException e) {
            completeExceptionally(e);
        }
    }

    @Override
    public void onFailure(@NotNull Call call, @NotNull IOException e) {
        if (e instanceof SocketTimeoutException) {
            completeExceptionally(new GitLabIOException("GitLab network connectivity failed: " + e.getMessage(), e));
            return;
        }
        completeExceptionally(e);
    }

    protected void onRawResponseBody(Response response, String rawResponseBodyString) {
        logRawResponseBody(response, rawResponseBodyString);
    }

    protected void logRawResponseBody(Response response, String rawResponseBodyString) {
        log.debug("HTTP " + response.code() + "\n" + rawResponseBodyString);
    }

    protected void logAndConsumeRawResponseBody(Response response) {
        try(ResponseBody body = response.body()) {
            String bodyPayload = assertHasBody(response, body).string();
            logRawResponseBody(response, bodyPayload);
        } catch (IOException e) {
            log.debug("Cannot log and consume response body", e);
        }
    }

    protected void completeExceptionally(CompletableFuture<T> result, Throwable throwable, Response response) {
        logAndConsumeRawResponseBody(response);
        result.completeExceptionally(throwable);
    }

    protected T handleResponse(Response response, ResponseBody body, String json, T object) {
        return object;
    }

    protected abstract void complete(T resultValue);

    protected abstract void completeExceptionally(Throwable throwable);

    protected abstract TypeToken<T> getTypeToken();
}
