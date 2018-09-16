package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.gitlab.User;
import com.intellij.util.containers.Convertor;
import org.jdesktop.swingx.renderer.StringValue;

public class UserToStringConverter implements StringValue, Convertor<Object, String> {
    @Override
    public String getString(Object o) {
        if (o instanceof User) {
            User user = (User) o;
            if (user.getName() != null) {
                return user.getName();
            }
            return String.valueOf(user.getId());
        }
        return o.toString();
    }

    @Override
    public String convert(Object o) {
        return getString(o);
    }
}
