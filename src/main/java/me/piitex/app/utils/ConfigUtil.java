package me.piitex.app.utils;

import me.piitex.app.App;

import java.io.*;
import java.util.*;

public class ConfigUtil {
    private Map<String, Object> configData;
    private final File configFile;

    /**
     * Constructs a ConfigUtil instance by reading and parsing the configuration content
     * from the specified File object.
     *
     * @param configFile The File object representing the configuration file.
     * @throws IOException If an I/O error occurs during file reading or parsing,
     * or if the configuration format is invalid.
     */
    public ConfigUtil(File configFile) throws IOException {
        this.configFile = configFile;
        // Check if the file exists and is readable before attempting to read.
        if (!configFile.exists()) {
            throw new IOException("Configuration file not found: " + configFile.getAbsolutePath());
        }
        if (!configFile.isFile()) {
            throw new IOException("Path is not a file: " + configFile.getAbsolutePath());
        }
        if (!configFile.canRead()) {
            throw new IOException("Cannot read configuration file: " + configFile.getAbsolutePath());
        }

        StringBuilder configContentBuilder = new StringBuilder();
        try (BufferedReader fileReader = new BufferedReader(new FileReader(configFile))) {
            String line;
            while ((line = fileReader.readLine()) != null) {
                configContentBuilder.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new IOException("Failed to read configuration file: " + configFile.getAbsolutePath() + ". " + e.getMessage(), e);
        }
        this.configData = parseConfig(configContentBuilder.toString());
    }

    /**
     * Parses the raw configuration content into a nested Map structure.
     *
     * @param configContent The string containing the configuration data.
     * @return A Map representing the parsed configuration.
     * @throws IOException If the configuration format is invalid (e.g., incorrect indentation, missing colon).
     */
    private Map<String, Object> parseConfig(String configContent) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();

        Stack<Map.Entry<Map<String, Object>, Integer>> contextStack = new Stack<>();
        contextStack.push(new HashMap.SimpleEntry<>(root, -1));

        BufferedReader reader = new BufferedReader(new StringReader(configContent));
        String line;
        int lineNumber = 0;

        while ((line = reader.readLine()) != null) {
            lineNumber++;
            String originalLine = line;
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) {
                continue;
            }

            int currentIndentation = getIndentation(originalLine);
            String content = originalLine.trim();

            int colonIndex = content.indexOf(':');
            if (colonIndex == -1) {
                throw new IOException("Invalid format: Missing colon at line " + lineNumber + ". Line: '" + originalLine + "'");
            }

            String key = content.substring(0, colonIndex).trim();
            String valuePart = content.substring(colonIndex + 1).trim();

            while (!contextStack.isEmpty() && currentIndentation <= contextStack.peek().getValue()) {
                contextStack.pop();
            }

            if (contextStack.isEmpty()) {
                throw new IOException("Invalid indentation: line " + lineNumber + " has no valid parent scope. Line: '" + originalLine + "'");
            }

            Map<String, Object> currentMap = contextStack.peek().getKey();

            if (valuePart.isEmpty()) {
                Map<String, Object> newSection = new LinkedHashMap<>();
                currentMap.put(key, newSection);
                contextStack.push(new HashMap.SimpleEntry<>(newSection, currentIndentation));
            } else if (valuePart.startsWith("[") && valuePart.endsWith("]")) {
                currentMap.put(key, parseList(valuePart.substring(1, valuePart.length() - 1)));
            } else {
                currentMap.put(key, parseValue(valuePart));
            }
        }
        return root;
    }

    /**
     * Calculates the indentation level (number of leading spaces) of a given line.
     *
     * @param line The line to check.
     * @return The number of leading spaces.
     */
    private int getIndentation(String line) {
        int indentation = 0;
        for (char c : line.toCharArray()) {
            if (c == ' ') {
                indentation++;
            } else {
                break;
            }
        }
        return indentation;
    }

    /**
     * Parses a string value and attempts to convert it to an Integer, Double, Float, or Boolean.
     * If none of these conversions are successful, it returns the original string.
     *
     * @param value The string value to parse.
     * @return An Object representing the parsed value (Integer, Double, Float, Boolean, or String).
     */
    private Object parseValue(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {

        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {

        }

        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
        }

        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException ignored) {
        }

        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }

        return value;
    }

    /**
     * Parses a comma-separated string of list items into a List of Objects.
     * Each item in the list content is also parsed using `parseValue` to determine its type.
     *
     * @param listContent The content inside the square brackets of a list (e.g., "Item1, 123, true").
     * @return A List of Objects representing the parsed list items.
     */
    private List<Object> parseList(String listContent) {
        List<Object> list = new ArrayList<>();
        String[] items = listContent.split(",");
        for (String item : items) {
            list.add(parseValue(item.trim()));
        }
        return list;
    }

    /**
     * Retrieves a value from the configuration using a dot-separated key path.
     *
     * @param keyPath The dot-separated path to the configuration value (e.g., "Test.key", "Test.section.warning").
     * @param type The expected class type of the value.
     * @param <T> The generic type of the value.
     * @return The value cast to the specified type, or null if the key path does not exist.
     * @throws ClassCastException If the value found at the path is not assignable to the specified type.
     */
    public <T> T get(String keyPath, Class<T> type) {
        Object value = getValueByPath(keyPath);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        } else {
            throw new ClassCastException("Value at path '" + keyPath + "' is of type " + value.getClass().getName() + " but requested type was " + type.getName());
        }
    }

    /**
     * Retrieves a String value from the configuration.
     *
     * @param keyPath The dot-separated path to the String value.
     * @return The String value, or null if not found.
     * @throws ClassCastException If the value found at the path is not a String.
     */
    public String getString(String keyPath) {
        return get(keyPath, String.class);
    }

    /**
     * Retrieves an Integer value from the configuration.
     *
     * @param keyPath The dot-separated path to the Integer value.
     * @return The Integer value, or null if not found.
     * @throws ClassCastException If the value found at the path is not an Integer.
     */
    public Integer getInt(String keyPath) {
        return get(keyPath, Integer.class);
    }

    /**
     * Retrieves a Double value from the configuration.
     *
     * @param keyPath The dot-separated path to the Double value.
     * @return The Double value, or null if not found.
     * @throws ClassCastException If the value found at the path is not a Double.
     */
    public Double getDouble(String keyPath) {
        return get(keyPath, Double.class);
    }

    /**
     * Retrieves a Float value from the configuration.
     *
     * @param keyPath The dot-separated path to the Float value.
     * @return The Float value, or null if not found.
     * @throws ClassCastException If the value found at the path is not a Float.
     */
    public Float getFloat(String keyPath) {
        return get(keyPath, Float.class);
    }

    public Long getLong(String keyPath) {
        return get(keyPath, Long.class);
    }

    /**
     * Retrieves a Boolean value from the configuration.
     *
     * @param keyPath The dot-separated path to the Boolean value.
     * @return The Boolean value, or null if not found.
     * @throws ClassCastException If the value found at the path is not a Boolean.
     */
    public Boolean getBoolean(String keyPath) {
        return get(keyPath, Boolean.class);
    }

    /**
     * Retrieves a configuration section (represented as a Map) from the configuration.
     *
     * @param keyPath The dot-separated path to the section.
     * @return The Map representing the section, or null if not found.
     * @throws ClassCastException If the value found at the path is not a Map.
     */
    @SuppressWarnings("unchecked") // Cast is safe due to type checking in get()
    public Map<String, Object> getSection(String keyPath) {
        return get(keyPath, Map.class);
    }

    /**
     * Retrieves a List of Objects from the configuration.
     *
     * @param keyPath The dot-separated path to the List.
     * @return The List of Objects, or null if not found.
     * @throws ClassCastException If the value found at the path is not a List.
     */
    @SuppressWarnings("unchecked") // Cast is safe due to type checking in get()
    public List<Object> getList(String keyPath) {
        return get(keyPath, List.class);
    }

    public boolean has(String key) {
        return getRawConfigData().containsKey(key);
    }

    /**
     * Internal helper method to traverse the parsed configuration map based on a dot-separated key path.
     * This method now handles keys that contain the path separator ('.') by attempting to match
     * the longest possible key segment from the current map's keys.
     *
     * @param keyPath The dot-separated path (e.g., "parent.child.value" or "key.with.dot.subsection").
     * @return The object found at the end of the path, or null if any part of the path is invalid or not found.
     */
    private Object getValueByPath(String keyPath) {
        Map<String, Object> current = this.configData;
        Object result = null;
        String remainingPath = keyPath;

        while (!remainingPath.isEmpty()) {
            boolean foundSegment = false;
            String bestMatchKey = null;
            int bestMatchLength = -1;
            for (String mapKey : current.keySet()) {
                if (remainingPath.startsWith(mapKey)) {
                    if (remainingPath.length() == mapKey.length() || remainingPath.charAt(mapKey.length()) == '.') {
                        if (mapKey.length() > bestMatchLength) {
                            bestMatchLength = mapKey.length();
                            bestMatchKey = mapKey;
                        }
                    }
                }
            }

            if (bestMatchKey != null) {
                foundSegment = true;
                result = current.get(bestMatchKey);
                if (remainingPath.length() == bestMatchLength) {
                    remainingPath = "";
                } else {
                    remainingPath = remainingPath.substring(bestMatchLength + 1);
                    if (!(result instanceof Map)) {
                        return null;
                    }
                    current = (Map<String, Object>) result;
                }
            }

            if (!foundSegment) {
                return null;
            }
        }
        return result;
    }

    /**
     * Sets a value in the configuration at the specified dot-separated key path.
     * This method will create intermediate sections (Maps) if they do not exist.
     *
     * @param keyPath The dot-separated path to set the value (e.g., "section.subsection.key").
     * @param value The value to set. Null values will remove the key if it exists, but will not create new sections.
     */
    @SuppressWarnings("unchecked")
    public void set(String keyPath, Object value) {
        if (keyPath == null || keyPath.isEmpty()) {
            throw new IllegalArgumentException("Key path cannot be null or empty.");
        }

        String[] pathParts = keyPath.split("\\.");
        Map<String, Object> currentMap = this.configData;

        for (int i = 0; i < pathParts.length - 1; i++) {
            String part = pathParts[i];
            Object next = currentMap.get(part);

            if (next == null) {
                Map<String, Object> newSection = new LinkedHashMap<>();
                currentMap.put(part, newSection);
                currentMap = newSection;
            } else if (next instanceof Map) {
                currentMap = (Map<String, Object>) next;
            } else {
                throw new IllegalArgumentException("Cannot set value at path '" + keyPath + "'. Intermediate key '" + part + "' is a value, not a section.");
            }
        }

        String finalKey = pathParts[pathParts.length - 1];
        if (value == null) {
            currentMap.remove(finalKey);
        } else {
            currentMap.put(finalKey, value);
        }

        try {
            save();
        } catch (IOException e) {
            App.logger.error("Could not write to file.");
        }
    }

    /**
     * Saves the current in-memory configuration data back to the original file.
     * The file will be overwritten with the new configuration.
     *
     * @throws IOException If an I/O error occurs during file writing.
     */
    public void save() throws IOException {
        if (!configFile.exists()) {
            configFile.createNewFile();
        }
        if (!configFile.canWrite()) {
            throw new IOException("Cannot write to configuration file: " + configFile.getAbsolutePath());
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(configFile, true))) {
            writeMapToFile(configData, writer, 0);
        } catch (IOException e) {
            throw new IOException("Failed to save configuration file: " + configFile.getAbsolutePath() + ". " + e.getMessage(), e);
        }
    }

    /**
     * Recursively writes the map content to the PrintWriter with proper indentation.
     *
     * @param map The map to write.
     * @param writer The PrintWriter to write to.
     * @param indentation The current indentation level (number of spaces).
     */
    @SuppressWarnings("unchecked")
    private void writeMapToFile(Map<String, Object> map, PrintWriter writer, int indentation) {
        String indentString = " ".repeat(indentation);

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            writer.print(indentString);
            writer.print(entry.getKey());
            writer.print(": ");

            Object value = entry.getValue();
            if (value instanceof Map) {
                writer.println(); // New line for nested section
                writeMapToFile((Map<String, Object>) value, writer, indentation + 4); // Increase indentation for nested map
            } else if (value instanceof List) {
                writer.println(formatList((List<Object>) value));
            } else {
                writer.println(formatValue(value));
            }
        }
    }

    /**
     * Formats a List of Objects into a string suitable for saving (e.g., "[item1, item2]").
     *
     * @param list The list to format.
     * @return A string representation of the list.
     */
    private String formatList(List<Object> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            sb.append(formatValue(list.get(i)));
            if (i < list.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Formats an Object value into its string representation for saving.
     * This handles special cases like booleans and numbers.
     *
     * @param value The object to format.
     * @return The string representation of the value.
     */
    private String formatValue(Object value) {
        if (value instanceof String) {
            return (String) value; // Assuming no special escaping needed for simple strings
        } else if (value instanceof Boolean) {
            return value.toString();
        } else if (value instanceof Number) {
            // Numbers are fine as-is
            return value.toString();
        }
        // Fallback for any other object type
        return value.toString();
    }

    /**
     * For debugging and testing purposes: allows access to the raw parsed configuration data.
     *
     * @return The internal Map representing the parsed configuration.
     */
    public Map<String, Object> getRawConfigData() {
        return configData;
    }
}