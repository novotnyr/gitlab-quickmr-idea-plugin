package com.github.novotnyr.idea.gitlab.quickmr.settings;

import javax.swing.text.JTextComponent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public abstract class SettingUtils {
    public static <T> boolean hasSameUniqueElements(Collection<T> collection1, Collection<T> collection2) {
        if (collection1 == collection2) {
            return true;
        }
        if (collection1 == null || collection2 == null) {
            return false;
        }
        Set<T> set1 = new HashSet<>(collection1);
        Set<T> set2 = new HashSet<>(collection2);
        return set1.equals(set2);
    }

    public static boolean equals(JTextComponent textComponent, String expectedValue) {
        String textValue = textComponent.getText();
        if (textValue == null) {
            textValue = "";
        }
        String expected = expectedValue;
        if (expected == null) {
            expected = "";
        }
        return Objects.equals(textValue, expected);
    }
}
