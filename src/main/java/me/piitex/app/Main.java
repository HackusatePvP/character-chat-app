package me.piitex.app;

public class Main {
    public static boolean run = false;

    public static void main(String[] args) {
        // Sets a flag which indicates the program was launched through the jar file.
        // When testing with the IDEA launch the application with JavaFXLoad.main()
        run = true;
        new App();
    }
}
