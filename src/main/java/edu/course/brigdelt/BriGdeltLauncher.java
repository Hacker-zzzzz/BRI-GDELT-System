package edu.course.brigdelt;

import javafx.application.Application;

/**
 * Plain Java launcher used by packaged distributions.
 */
public final class BriGdeltLauncher {

    private BriGdeltLauncher() {
    }

    public static void main(String[] args) {
        Application.launch(BriGdeltApplication.class, args);
    }
}
