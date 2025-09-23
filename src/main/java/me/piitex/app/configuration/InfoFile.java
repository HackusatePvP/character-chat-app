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

    /**
     * Constructs a new {@code InfoFile} instance for storing data in memory.
     * <p>
     * This constructor does not create or associate with a physical file.
     */
    public InfoFile() {
        this.file = null;
        this.encrypt = false;
    }

    /**
     * Constructs a new {@code InfoFile} instance and associates it with a physical file.
     * <p>
     * If the file does not exist, it will be created. If the file exists, its contents
     * will be loaded into memory. The file's contents will be decrypted if
     * encryption is enabled.
     *
     * @param file    the file to read from and write to.
     * @param encrypt a boolean indicating whether the file should be encrypted.
     * @throws RuntimeException if an {@link IOException} or {@link FileNotFoundException} occurs during file handling.
     */
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

    /**
     * Checks if the file is configured to be encrypted.
     *
     * @return true if encryption is enabled, false otherwise.
     */
    public boolean isEncrypted() {
        return encrypt;
    }

    /**
     * Returns the file associated with this {@code InfoFile} instance.
     *
     * @return the {@link File} object.
     */
    public File getFile() {
        return file;
    }

    /**
     * Retrieves the string value associated with the specified key.
     *
     * @param key the key of the value to get.
     * @return the string value, or null if the key is not found.
     */
    public String get(String key) {
        return entryMap.get(key);
    }

    /**
     * Retrieves the boolean value associated with the specified key.
     *
     * @param key the key of the value to get.
     * @return the boolean value.
     */
    public Boolean getBoolean(String key) {
        return Boolean.parseBoolean(get(key));
    }

    /**
     * Retrieves the integer value associated with the specified key.
     *
     * @param key the key of the value to get.
     * @return the integer value.
     */
    public Integer getInteger(String key) {
        return Integer.parseInt(get(key));
    }

    /**
     * Retrieves the double value associated with the specified key.
     *
     * @param key the key of the value to get.
     * @return the double value.
     */
    public Double getDouble(String key) {
        return Double.parseDouble(get(key));
    }

    /**
     * Retrieves the long value associated with the specified key.
     *
     * @param key the key of the value to get.
     * @return the long value.
     */
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

    /**
     * Retrieves a list of strings associated with the specified key.
     * <p>
     * The values in the list are expected to be delimited by "!@!".
     *
     * @param key the key of the value to get.
     * @return a {@link List} of strings.
     */
    public List<String> getList(String key) {
        String value = entryMap.get(key);
        List<String> toReturn = new ArrayList<>();
        if (!value.contains("!@!")) {
            return toReturn;
        }

        toReturn.addAll(Arrays.asList(value.split("!@!")));

        return toReturn;
    }

    /**
     * Retrieves a linked list of strings associated with the specified key.
     * <p>
     * The values in the linked list are expected to be delimited by "!@!".
     *
     * @param key the key of the value to get.
     * @return a {@link LinkedList} of strings.
     */
    public LinkedList<String> getLinkedList(String key) {
        String value = entryMap.get(key);
        LinkedList<String> toReturn = new LinkedList<>();
        if (!value.contains("!@!")) {
            return toReturn;
        }

        toReturn.addAll(Arrays.asList(value.split("!@!")));

        return toReturn;
    }

    /**
     * Retrieves a map of strings associated with the specified key.
     * <p>
     * The map is expected to be stored using the "!&'!" and "@!@" delimiters.
     *
     * @param key the key of the map to get.
     * @return a {@link Map} of strings.
     */
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

    /**
     * Checks if a key exists in the entry map.
     *
     * @param key the key to check for.
     * @return true if the key exists, false otherwise.
     */
    public boolean hasKey(String key) {
        return entryMap.containsKey(key);
    }

    /**
     * Sets a key-value pair.
     * <p>
     * Newline characters in the value will be replaced with the "!@!" delimiter.
     *
     * @param key   the key to set.
     * @param value the string value to set.
     */
    public void set(String key, String value) {
        value = value.replace("\n", "!@!");
        entryMap.put(key, value);
        update();
    }

    /**
     * Sets a key-value pair with a double value.
     *
     * @param key   the key to set.
     * @param value the double value to set.
     */
    public void set(String key, double value) {
        entryMap.put(key, value + "");
        update();
    }

    /**
     * Sets a key-value pair with an integer value.
     *
     * @param key   the key to set.
     * @param value the integer value to set.
     */
    public void set(String key, int value) {
        entryMap.put(key, value + "");
        update();
    }

    /**
     * Sets a key-value pair with a boolean value.
     *
     * @param key   the key to set.
     * @param value the boolean value to set.
     */
    public void set(String key, boolean value) {
        entryMap.put(key, value + "");
        update();
    }

    /**
     * Sets a key-value pair with a long value.
     *
     * @param key   the key to set.
     * @param value the long value to set.
     */
    public void set(String key, long value) {
        entryMap.put(key, value + "");
        update();
    }

    /**
     * Sets a key-value pair with a list of strings.
     * <p>
     * The list will be stored as a single string delimited by "!@!".
     *
     * @param key    the key to set.
     * @param values the list of strings to set.
     */
    public void set(String key, List<String> values) {
        StringBuilder appender = new StringBuilder();
        for (String s : values) {
            appender.append("!@!").append(s);
        }
        entryMap.put(key, appender.toString());
        update();
    }

    /**
     * Sets a key-value pair with a map of strings.
     * <p>
     * The map will be stored as a single string using the "!&'!" and "@!@" delimiters.
     *
     * @param key the key to set.
     * @param map the map of strings to set.
     */
    public void set(String key, Map<String, String> map) {
        StringBuilder appender = new StringBuilder();
        map.forEach((s, s2) -> {
            appender.append("!&'!").append(s).append("@!@").append(s2);
        });
        entryMap.put(key, appender.toString());
        update();
    }

    /**
     * Writes the current entry map to the associated file.
     * <p>
     * This method is called automatically after every {@link #set(String, String)} operation.
     * The file will be encrypted if encryption is enabled.
     *
     * @throws RuntimeException if an {@link IOException} occurs during file writing.
     */
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
