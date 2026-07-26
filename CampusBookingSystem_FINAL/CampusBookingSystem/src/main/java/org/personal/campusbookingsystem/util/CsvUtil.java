package org.personal.campusbookingsystem.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Small, dependency-free CSV reader/writer helper.
 *
 * Unlike a naive String.split(",") approach, this class understands basic
 * RFC-4180 quoting: any field containing a comma, a double-quote, or a
 * newline is wrapped in double quotes on write, and embedded quotes are
 * escaped by doubling them (" -> ""). This means resource/booking fields
 * such as a name or purpose can safely contain a comma without corrupting
 * the file - previously that had to be worked around by hand (e.g.
 * Booking.purpose replacing "," with ";" before saving).
 *
 * Every Manager class (ResourceManager, UserManager, BookingManager) can
 * use the same two methods - readRows(...) / writeRows(...) - to load and
 * persist its own CSV file, and is only responsible for turning its own
 * model objects into a String[] row and back again.
 */
public final class CsvUtil {

    private CsvUtil() {
    }

    /** Reads every row of an on-disk CSV file. Returns an empty list if the file does not exist yet. */
    public static List<String[]> readRows(Path path) {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException e) {
            System.out.println("Could not read " + path + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** Reads every row of a CSV file bundled on the classpath (e.g. a seed/starter file packaged in resources/). */
    public static List<String[]> readRowsFromClasspath(Class<?> anchor, String classpathLocation) {
        try (InputStream in = anchor.getResourceAsStream(classpathLocation)) {
            if (in == null) {
                return new ArrayList<>();
            }
            return parse(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read bundled CSV " + classpathLocation, e);
        }
    }

    /** Overwrites an on-disk CSV file with the given rows, creating parent directories if needed. */
    public static void writeRows(Path path, List<String[]> rows) {
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            StringBuilder sb = new StringBuilder();
            for (String[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    if (i > 0) {
                        sb.append(',');
                    }
                    sb.append(escape(row[i]));
                }
                sb.append('\n');
            }
            Files.writeString(path, sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Could not write " + path + ": " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Internal parsing / escaping
    // ---------------------------------------------------------------

    private static String escape(String field) {
        if (field == null) {
            return "";
        }
        boolean needsQuoting = field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r");
        if (!needsQuoting) {
            return field;
        }
        return "\"" + field.replace("\"", "\"\"") + "\"";
    }

    /** A small hand-rolled RFC-4180 parser: handles quoted fields, escaped quotes, and embedded commas/newlines. */
    private static List<String[]> parse(Reader reader) throws IOException {
        String content = readAll(reader);

        List<String[]> rows = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;

        int i = 0;
        int len = content.length();
        while (i < len) {
            char ch = content.charAt(i);

            if (inQuotes) {
                if (ch == '"') {
                    boolean isEscapedQuote = i + 1 < len && content.charAt(i + 1) == '"';
                    if (isEscapedQuote) {
                        field.append('"');
                        i += 2;
                    } else {
                        inQuotes = false;
                        i++;
                    }
                } else {
                    field.append(ch);
                    i++;
                }
                continue;
            }

            switch (ch) {
                case '"' -> {
                    inQuotes = true;
                    i++;
                }
                case ',' -> {
                    currentRow.add(field.toString());
                    field.setLength(0);
                    i++;
                }
                case '\r' -> i++; // ignore; '\n' (or EOF) ends the row
                case '\n' -> {
                    currentRow.add(field.toString());
                    field.setLength(0);
                    rows.add(currentRow.toArray(new String[0]));
                    currentRow = new ArrayList<>();
                    i++;
                }
                default -> {
                    field.append(ch);
                    i++;
                }
            }
        }

        // last line with no trailing newline
        if (field.length() > 0 || !currentRow.isEmpty()) {
            currentRow.add(field.toString());
            rows.add(currentRow.toArray(new String[0]));
        }
        return rows;
    }

    private static String readAll(Reader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = reader.read(buf)) != -1) {
            sb.append(buf, 0, n);
        }
        return sb.toString();
    }
}
