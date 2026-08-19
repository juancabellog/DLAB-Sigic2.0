package com.sisgic.tools;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.Scanner;

/**
 * Herramienta de administración para cambiar la contraseña de un usuario
 * en la tabla {@code users_2_0}, usando el mismo BCrypt que el login de la app.
 *
 * <p>Uso (desde el directorio {@code backend}):</p>
 * <pre>
 *   ./change-password.sh
 *   # o
 *   mvn -q exec:java -Dexec.mainClass=com.sisgic.tools.ChangeUserPasswordTool
 * </pre>
 *
 * <p>Opcionalmente puedes pasar JDBC por argumentos:</p>
 * <pre>
 *   --url=jdbc:mysql://host/db?...
 *   --user=root
 *   --password=secret
 * </pre>
 */
public final class ChangeUserPasswordTool {

    private static final String DEFAULT_URL =
        "jdbc:mysql://192.168.2.220/sigicprod_2_0?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "Mariadb2022";

    private ChangeUserPasswordTool() {}

    public static void main(String[] args) {
        System.out.println();
        System.out.println("=== SISGIC · Cambio de contraseña de usuario ===");
        System.out.println();

        DbConfig db = resolveDbConfig(args);
        Scanner scanner = new Scanner(System.in);

        try {
            String username = promptLine(scanner, "Usuario (username): ");
            if (username.isBlank()) {
                fail("El usuario es obligatorio.");
                return;
            }

            char[] passwordChars = promptPassword(scanner, "Nueva contraseña: ");
            char[] confirmChars = promptPassword(scanner, "Confirmar contraseña: ");

            String password = new String(passwordChars);
            String confirm = new String(confirmChars);
            clearChars(passwordChars);
            clearChars(confirmChars);

            if (password.isBlank()) {
                fail("La contraseña no puede estar vacía.");
                return;
            }
            if (!password.equals(confirm)) {
                fail("Las contraseñas no coinciden.");
                return;
            }
            if (password.length() < 6) {
                fail("La contraseña debe tener al menos 6 caracteres.");
                return;
            }

            System.out.println();
            System.out.print("¿Confirmar cambio de clave para '" + username + "'? [s/N]: ");
            String answer = scanner.nextLine().trim().toLowerCase();
            if (!answer.equals("s") && !answer.equals("si") && !answer.equals("y") && !answer.equals("yes")) {
                System.out.println("Operación cancelada.");
                return;
            }

            changePassword(db, username.trim(), password);
            System.out.println();
            System.out.println("OK: contraseña actualizada para el usuario '" + username.trim() + "'.");
        } catch (SQLException e) {
            fail("Error de base de datos: " + e.getMessage());
        } catch (Exception e) {
            fail("Error: " + e.getMessage());
        }
    }

    private static void changePassword(DbConfig db, String username, String rawPassword) throws SQLException {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode(rawPassword);

        try (Connection connection = DriverManager.getConnection(db.url, db.user, db.password)) {
            Long userId;
            String email;
            Boolean enabled;

            try (PreparedStatement find = connection.prepareStatement(
                "SELECT id, email, enabled FROM users_2_0 WHERE username = ?")) {
                find.setString(1, username);
                try (ResultSet rs = find.executeQuery()) {
                    if (!rs.next()) {
                        throw new SQLException("No existe el usuario '" + username + "' en users_2_0.");
                    }
                    userId = rs.getLong("id");
                    email = rs.getString("email");
                    enabled = rs.getBoolean("enabled");
                }
            }

            System.out.println();
            System.out.println("Usuario encontrado:");
            System.out.println("  id      = " + userId);
            System.out.println("  email   = " + email);
            System.out.println("  enabled = " + enabled);

            try (PreparedStatement update = connection.prepareStatement(
                "UPDATE users_2_0 SET password = ?, updated_at = NOW() WHERE id = ?")) {
                update.setString(1, encoded);
                update.setLong(2, userId);
                int updated = update.executeUpdate();
                if (updated != 1) {
                    throw new SQLException("No se pudo actualizar la contraseña (filas afectadas: " + updated + ").");
                }
            }
        }
    }

    private static String promptLine(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    private static char[] promptPassword(Scanner scanner, String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] value = console.readPassword("%s", prompt);
            return value != null ? value : new char[0];
        }
        // Fallback cuando no hay consola real (p. ej. algunos IDEs / pipes).
        System.out.print(prompt);
        System.out.flush();
        String line = scanner.nextLine();
        return line != null ? line.toCharArray() : new char[0];
    }

    private static void clearChars(char[] chars) {
        if (chars == null) {
            return;
        }
        for (int i = 0; i < chars.length; i++) {
            chars[i] = '\0';
        }
    }

    private static void fail(String message) {
        System.err.println();
        System.err.println("ERROR: " + message);
        System.exit(1);
    }

    private static DbConfig resolveDbConfig(String[] args) {
        Properties yamlProps = tryLoadDatasourceFromYaml();

        String url = firstNonBlank(
            argValue(args, "--url"),
            System.getenv("SISGIC_DB_URL"),
            yamlProps.getProperty("url"),
            DEFAULT_URL
        );
        String user = firstNonBlank(
            argValue(args, "--user"),
            System.getenv("SISGIC_DB_USER"),
            yamlProps.getProperty("username"),
            DEFAULT_USER
        );
        String password = firstNonBlank(
            argValue(args, "--password"),
            System.getenv("SISGIC_DB_PASSWORD"),
            yamlProps.getProperty("password"),
            DEFAULT_PASSWORD
        );

        System.out.println("Conexión DB: " + maskUrl(url) + "  (user=" + user + ")");
        return new DbConfig(url, user, password);
    }

    private static Properties tryLoadDatasourceFromYaml() {
        Properties props = new Properties();
        Path[] candidates = new Path[] {
            Path.of("src/main/resources/application.yml"),
            Path.of("backend/src/main/resources/application.yml"),
            Path.of("application.yml")
        };

        for (Path path : candidates) {
            if (!Files.isRegularFile(path)) {
                continue;
            }
            try {
                String content = Files.readString(path);
                String url = extractYamlScalar(content, "url");
                String username = extractYamlScalar(content, "username");
                String password = extractYamlScalar(content, "password");
                if (url != null && url.startsWith("jdbc:")) {
                    props.setProperty("url", url);
                }
                if (username != null && !username.isBlank()) {
                    props.setProperty("username", username);
                }
                if (password != null) {
                    props.setProperty("password", password);
                }
                if (!props.isEmpty()) {
                    System.out.println("Config DB leída de: " + path.toAbsolutePath().normalize());
                    return props;
                }
            } catch (IOException ignored) {
                // keep defaults
            }
        }
        return props;
    }

    /**
     * Extracción mínima de escalares bajo spring.datasource (suficiente para este tool).
     */
    private static String extractYamlScalar(String yaml, String key) {
        String[] lines = yaml.split("\\R");
        boolean inDatasource = false;
        for (String raw : lines) {
            String line = raw.replace("\t", "    ");
            if (line.matches("^\\s*datasource:\\s*$")) {
                inDatasource = true;
                continue;
            }
            if (inDatasource && line.matches("^[a-zA-Z].*")) {
                inDatasource = false;
            }
            if (!inDatasource) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.startsWith(key + ":")) {
                String value = trimmed.substring((key + ":").length()).trim();
                if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                int comment = value.indexOf(" #");
                if (comment >= 0) {
                    value = value.substring(0, comment).trim();
                }
                return value;
            }
        }
        return null;
    }

    private static String argValue(String[] args, String name) {
        if (args == null) {
            return null;
        }
        String prefix = name + "=";
        for (String arg : args) {
            if (arg != null && arg.startsWith(prefix)) {
                return arg.substring(prefix.length());
            }
        }
        return null;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String maskUrl(String url) {
        if (url == null) {
            return "";
        }
        int scheme = url.indexOf("://");
        if (scheme < 0) {
            return url;
        }
        int slash = url.indexOf('/', scheme + 3);
        if (slash < 0) {
            return url.substring(0, scheme + 3) + "***";
        }
        return url.substring(0, scheme + 3) + "***" + url.substring(slash);
    }

    private static final class DbConfig {
        private final String url;
        private final String user;
        private final String password;

        private DbConfig(String url, String user, String password) {
            this.url = url;
            this.user = user;
            this.password = password;
        }
    }
}
