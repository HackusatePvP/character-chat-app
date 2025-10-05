package me.piitex.app;

import javafx.application.Application;

public class Main {
    public static boolean run = false;

    public static void main(String[] args) {
        // Sets a flag which indicates the program was launched through the jar file.
        // When testing with the IDEA launch the application with App.class
        run = true;
        new App();
        Application.launch(App.class);
    }
}
