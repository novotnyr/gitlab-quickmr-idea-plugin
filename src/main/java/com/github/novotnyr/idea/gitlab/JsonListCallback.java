package com.github.novotnyr.idea.gitlab;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class JsonListCallback<T> extends TypedJsonHttpResponseCallback<List<T>> {
    private final TypeToken<List<T>> typeToken;

    private final CompletableFuture<List<T>> result;

    public JsonListCallback(Class<T> resultClass, CompletableFuture<List<T>> result, Gson gson) {
        super(gson);
        //noinspection unchecked
        this.typeToken = (TypeToken<List<T>>) TypeToken.getParameterized(List.class, resultClass);
        this.result = result;
    }

    @Override
    protected void complete(List<T> resultValue) {
        this.result.complete(resultValue);
    }

    @Override
    protected void completeExceptionally(Throwable throwable) {
        this.result.completeExceptionally(throwable);
    }

    @Override
    protected TypeToken<List<T>> getTypeToken() {
        return typeToken;
    }
}
