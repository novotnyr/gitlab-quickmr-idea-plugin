package com.github.novotnyr.idea.gitlab;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JsonHttpResponseCallback<T> implements Callback {
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
        // do nothing
    }

    protected T handleResponse(Response response, ResponseBody body, String json, T object) {
        return object;
    }
}
