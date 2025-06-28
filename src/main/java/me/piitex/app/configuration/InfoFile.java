package me.piitex.app.configuration;


import me.piitex.app.utils.FileCrypter;

import javax.crypto.IllegalBlockSizeException;
import java.io.*;
import java.util.*;

public class InfoFile {
    private final File file;
    private final boolean encrypt;
    private final boolean dev = false; // When set to true it disables encryption even if encryption is true.

    private Map<String, String> entryMap = new HashMap<>();

    // Nothing will be saved!!!
    public InfoFile() {
        this.file = null;
        this.encrypt = false;
    }

    public InfoFile(File file, boolean encrypt) {
        this.file = file;
        this.encrypt = encrypt;
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            // Load file
            Scanner scanner = null;
            try {
                File output = new File("output.info");
                if (encrypt && !dev) {
                    // Decrypt file
                    try {
                        FileCrypter.decryptFile(file, output);
                        scanner = new Scanner(new FileInputStream(output));
                    } catch (IllegalBlockSizeException | IOException ignored) {
                        scanner = new Scanner(new FileInputStream(file));
                    }
                } else {
                    scanner = new Scanner(new FileInputStream(file));
                }
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.split("=").length > 1) {
                        entryMap.put(line.split("=")[0], line.split("=")[1].replace("!@!", "\n"));
                    }
                }
                output.delete();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                if (scanner != null) {
                    scanner.close();
                }
            }
        }
    }

    public boolean isEncrypted() {
        return encrypt;
    }

    public File getFile() {
        return file;
    }

    public String get(String key) {
        return entryMap.get(key);
    }

    public Boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    public Integer getInteger(String key) {
        return Integer.parseInt(get(key));
    }

    public Double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    public Long getLong(String key) {
        return Long.parseLong(get(key));
    }

    public String getOrDefault(String key, String defaultValue) {
        return entryMap.getOrDefault(key, defaultValue);
    }

    public Boolean getBooleanOrDefault(String key, Boolean defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    public Integer getIntegerOrDefault(String key, Integer defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    public Double getDoubleOrDefault(String key, Double defaultValue) {
        String value = get(key);
        return value == null ? defaultValue : Double.parseDouble(value);
    }

    public List<String> getList(String key) {
        String value = entryMap.get(key);
        List<String> toReturn = new ArrayList<>();
        if (!value.contains("!@!")) {
            return toReturn;
        }

        toReturn.addAll(Arrays.asList(value.split("!@!")));

        return toReturn;
    }

    public LinkedList<String> getLinkedList(String key) {
        String value = entryMap.get(key);
        LinkedList<String> toReturn = new LinkedList<>();
        if (!value.contains("!@!")) {
            return toReturn;
        }

        toReturn.addAll(Arrays.asList(value.split("!@!")));

        return toReturn;
    }

    public Map<String, String> getStringMap(String key) {
        Map<String, String> toReturn = new HashMap<>();
        // !@!key@!@value!@!key@!@value2!@!

        String value = entryMap.get(key);
        if (!value.contains("!&'!")) {
            return toReturn;
        }

        for (String s : value.split("!&'!")) {
            // s = key@!@value
            String[] split = s.split("@!@");
            if (split.length > 1) {
                toReturn.put(split[0], split[1]);
            }
        }

        return toReturn;
    }

    public boolean hasKey(String key) {
        return entryMap.containsKey(key);
    }

    public void set(String key, String value) {
        value = value.replace("\n", "!@!");
        entryMap.put(key, value);
        update();
    }

    public void set(String key, double value) {
        entryMap.put(key, value + "");
        update();
    }

    public void set(String key, int value) {
        entryMap.put(key, value + "");
        update();
    }

    public void set(String key, boolean value) {
        entryMap.put(key, value + "");
        update();
    }

    public void set(String key, long value) {
        entryMap.put(key, value + "");
        update();
    }

    public void set(String key, List<String> values) {
        StringBuilder appender = new StringBuilder();
        for (String s : values) {
            appender.append("!@!").append(s);
        }
        entryMap.put(key, appender.toString());
        update();
    }

    public void set(String key, Map<String, String> map) {
        StringBuilder appender = new StringBuilder();
        map.forEach((s, s2) -> {
            appender.append("!&'!").append(s).append("@!@").append(s2);
        });
        entryMap.put(key, appender.toString());
        update();
    }

    public void update() {
        if (file == null) return;
        try {
            FileWriter writer;
            File output = new File("output.info");
            if (encrypt && !dev) {
                writer = new FileWriter(output);
            } else {
                writer = new FileWriter(file);
            }
            entryMap.forEach((s, s2) -> {
                s2 = s2.replace("\n", "!@!");
                try {
                    writer.write(s + "=" + s2 + "\n");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            writer.close();
            if (encrypt && !dev) {
                // Replace output into the file
                FileCrypter.encryptFile(output, file);
            }
            if (output.exists()) {
                output.delete();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, String> getEntryMap() {
        return entryMap;
    }

    public void setEntryMap(Map<String, String> entryMap) {
        this.entryMap = entryMap;
    }
}
