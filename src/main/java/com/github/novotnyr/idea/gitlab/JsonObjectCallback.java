package com.github.novotnyr.idea.gitlab;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.concurrent.CompletableFuture;

public class JsonObjectCallback<T> extends TypedJsonHttpResponseCallback<T> {

    private final TypeToken<T> typeToken;

    private final CompletableFuture<T> result;

    public static <T> JsonObjectCallback<T> of(Class<T> resultClass, CompletableFuture<T> result, Gson gson) {
        return new JsonObjectCallback<>(resultClass, result, gson);
    }

    public JsonObjectCallback(Class<T> resultClass, CompletableFuture<T> result, Gson gson) {
        super(gson);
        this.typeToken = TypeToken.get(resultClass);
        this.result = result;
    }

    @Override
    protected void complete(T resultValue) {
        this.result.complete(resultValue);
    }

    @Override
    protected void completeExceptionally(Throwable throwable) {
        this.result.completeExceptionally(throwable);
    }

    @Override
    protected TypeToken<T> getTypeToken() {
        return this.typeToken;
    }
}
