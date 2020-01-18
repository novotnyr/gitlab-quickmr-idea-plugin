package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.gitlab.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;

public class JBProgressIndicator implements ProgressIndicator {
    @Override
    public void setPercents(float percents) {
        com.intellij.openapi.progress.ProgressIndicator progressIndicator = ProgressManager.getInstance()
                .getProgressIndicator();
        if (percents < 0f) {
            progressIndicator.setIndeterminate(true);
        } else {
            progressIndicator.setIndeterminate(false);
            progressIndicator.setFraction(percents);
        }

    }

    @Override
    public boolean isCancelled() {
        return ProgressManager.getInstance().getProgressIndicator().isCanceled();
    }
}
