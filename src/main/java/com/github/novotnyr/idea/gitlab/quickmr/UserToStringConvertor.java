package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.gitlab.User;
import com.intellij.util.containers.Convertor;

public class UserToStringConvertor implements Convertor<User, String> {
    private final UserToStringConverter delegate = new UserToStringConverter();

    @Override
    public String convert(User user) {
        return delegate.getString(user);
    }
}
