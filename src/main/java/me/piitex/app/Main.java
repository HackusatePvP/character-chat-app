package me.piitex.app;

import javafx.application.Application;

import java.util.Arrays;

public class Main {
    public static boolean run = false;
    public static boolean app = false;

    public static void main(String[] args) {
        // run and app are flags which dictate how the running directory should be set.
        // If the application is launched with the -app flag, it is executed through jpackage executable.
        // If run is passed, the application will use the current running directory.
        if (Arrays.asList(args).contains("-app")) {
            app = true;
        } else {
            run = true;
        }
        new App();
        Application.launch(App.class);
    }
}
