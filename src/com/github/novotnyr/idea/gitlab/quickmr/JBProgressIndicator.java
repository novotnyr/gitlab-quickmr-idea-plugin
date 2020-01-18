package com.github.novotnyr.idea.gitlab.quickmr;

import com.github.novotnyr.idea.gitlab.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;

public class JBProgressIndicator implements ProgressIndicator {
    @Override
    public void setPercents(float percents) {
        ProgressManager.getInstance().getProgressIndicator().setFraction(percents);
    }

    @Override
    public boolean isCancelled() {
        return ProgressManager.getInstance().getProgressIndicator().isCanceled();
    }
}
