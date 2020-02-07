package com.github.novotnyr.idea.gitlab;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.diagnostic.Logger;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JsonHttpResponseCallback<T> implements Callback {
    protected final Logger log = Logger.getInstance("#" + JsonHttpResponseCallback.class.getName());

    private Type typeToken;

    private final CompletableFuture<T> result;

    private final Gson gson;

    private JsonHttpResponseCallback(CompletableFuture<T> result, Gson gson) {
        this.result = result;
        this.gson = gson;
    }

    public JsonHttpResponseCallback(Class<T> resultClass, CompletableFuture<T> result, Gson gson) {
        this.typeToken = new TypeToken<T>(){}.getType();
        this.result = result;
        this.gson = gson;
    }

    public static <E> JsonHttpResponseCallback ofList(CompletableFuture<List<E>> result, Gson gson) {
        JsonHttpResponseCallback callback = new JsonHttpResponseCallback(result, gson);
        callback.typeToken = new TypeToken<List<E>>(){}.getType();

        return callback;
    }


    public void onFailure(Request request, IOException e) {
        if (e instanceof SocketTimeoutException) {
            result.completeExceptionally(new GitLabIOException("GitLab network connectivity failed: " + e.getMessage(), e));
            return;
        }
        result.completeExceptionally(e);
    }

    public void onResponse(Response response) throws IOException {
        try(ResponseBody body = response.body()) {
            String json = response.body().string();
            onRawResponseBody(response, json);
            Type typeToken = new TypeToken<T>(){}.getType();
            T deserializedJson = this.gson.fromJson(json, typeToken);
            result.complete(handleResponse(response, body, json, deserializedJson));
        } catch (JsonSyntaxException e) {
            result.completeExceptionally(e);
        }
    }

    protected void onRawResponseBody(Response response, String rawResponseBodyString) {
        logRawResponseBody(response, rawResponseBodyString);
    }

    protected void logRawResponseBody(Response response, String rawResponseBodyString) {
        log.debug("HTTP " + response.code() + "\n" + rawResponseBodyString);
    }

    protected void logAndConsumeRawResponseBody(Response response) {
        try(ResponseBody body = response.body()) {
            String bodyPayload = body.string();
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
}
