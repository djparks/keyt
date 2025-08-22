package org.openjfx;

import javafx.application.Application;

/**
 * Plain launcher class to avoid JavaFX launcher checks that require module-path.
 */
public final class Main {
    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
