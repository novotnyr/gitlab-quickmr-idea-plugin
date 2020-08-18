package com.github.novotnyr.idea.gitlab;

public interface ProgressIndicator {
    void setPercents(float percents);
    boolean isCancelled();

    ProgressIndicator NONE = new ProgressIndicator() {
        @Override
        public void setPercents(float percents) {
            /* do nothing */
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    };
}
