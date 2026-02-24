package com.github.novotnyr.idea.gitlab.quickmr.settings;

import javax.swing.text.JTextComponent;
import java.util.Objects;

public abstract class SettingUtils {
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
