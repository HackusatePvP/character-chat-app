package me.piitex.app.backend;

import me.piitex.app.App;
import me.piitex.app.utils.FileCrypter;

import javax.crypto.IllegalBlockSizeException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedList;

public class Chat {
    private final File file;

    private LinkedList<String> lines = new LinkedList<>();

    private Response response;

    private final boolean dev = false;

    public Chat(File file) {
        this.file = file;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        // Vibe coded with Gemini. It somehow works. If it doesn't work bring it with CEO of google.
        } else if (file.length() > 0 && !dev) { // Only attempt decryption if the file has content
            File out = new File(file.getParent(), "out.dat");
            try {
                FileCrypter.decryptFile(file, out);
                lines.addAll(Files.readAllLines(out.toPath()));
            } catch (IOException | IllegalBlockSizeException e) {
                App.logger.error("Error decrypting file: ", e);
            } finally {
                if (out.exists()) {
                    out.delete();
                }
            }
        } else {
            try {
                lines.addAll(Files.readAllLines(file.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public File getFile() {
        return file;
    }

    public Role getSender(String line) {
        //user:Hello
        if (line.startsWith("assistant:")) {
            return Role.ASSISTANT;
        } else {
            return Role.USER;
        }
    }

    public String getContent(String line) {
        line = line.replace("!@!", "\n");
        if (line.startsWith("user:")) {
            line = line.replaceFirst("user:", "");
        }
        if (line.startsWith("assistant:")) {
            line = line.replaceFirst("assistant:", "");
        }
        return line;
    }

    public void addLine(Role role, String content) {
        // Special character replacements
        // Sometimes models will produce special characters like ”
        content = content.replace("”", "\"");

        content = content.replace("\n", "!@!");
        lines.add(role.name().toLowerCase() + ":" + content);
        update();
    }

    public LinkedList<String> getLines() {
        LinkedList<String> toReturn = new LinkedList<>();
        lines.forEach(s -> {
            s = s.replace("!@!", "\n");
            toReturn.add(s);
        });

        return toReturn;
    }

    public LinkedList<String> getRawLines() {
        return lines;
    }

    public int getIndex(String line) {
        return lines.indexOf(line);
    }

    public String getLastLine(int index) {
        return lines.get(index - 1);
    }

    public void removeLine(int index) {
        lines.remove(index);
    }

    public String getLine(int index) {
        return lines.get(index);
    }

    public void replaceLine(String line, String newLine) {
        line = line.replace("\n", "!@!");
        newLine = newLine.replace("\n", "!@!");
        int index = lines.indexOf(line);
        lines.set(index, newLine);
    }

    public boolean containsLine(String line) {
        for (String s : getRawLines()) {
            if (s.toLowerCase().contains(line.toLowerCase())) {
                return true;
            }
        }

        return false;
    }

    public void update() {
        File tempIn = new File(file.getParent(), "temp_in.dat"); // Use a different temporary file
        if (dev) {
            tempIn = file;
        }
        try {
            FileWriter writer = new FileWriter(tempIn);
            for (String s : lines) {
                writer.write(s + "\n");
            }
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (!dev) {
                FileCrypter.encryptFile(tempIn, file);
            }
            if (tempIn.exists() && !dev) {
                tempIn.delete(); // Delete the temporary input file
            }
        }
    }

    public Response getResponse() {
        return response;
    }

    public void setResponse(Response response) {
        this.response = response;
    }
}
