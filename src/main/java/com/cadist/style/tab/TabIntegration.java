package com.cadist.style.tab;

import com.cadist.style.config.LinePatternRegistry;

/**
 * Optional integration with NEZNAMY/TAB.
 * The implementation class is loaded reflectively only when TAB is present,
 * keeping the main plugin free of compile-time TAB dependencies.
 */
public interface TabIntegration {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    boolean isTabListEnabled();

    void setTabListEnabled(boolean enabled);

    boolean isScoreboardEnabled();

    void setScoreboardEnabled(boolean enabled);

    LinePatternRegistry getTabLineRegistry();

    LinePatternRegistry getScoreboardLineRegistry();

    /**
     * Re-reads TAB's current configuration and removes stored gradients for lines
     * that no longer exist.
     */
    void refreshKnownLines();
}
