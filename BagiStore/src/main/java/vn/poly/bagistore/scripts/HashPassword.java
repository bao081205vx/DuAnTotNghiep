package vn.poly.bagistore.scripts;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.io.Console;
import java.util.Scanner;

/**
 * Small utility to hash a password with SHA-256 and print the hex digest.
 * Usage:
 *  - Compile: javac HashPassword.java
 *  - Run with argument: java HashPassword myPassword
 *  - Run interactively: java HashPassword  (you'll be prompted to type)
 *
 * Output is lowercase hex (matches project's hashPasswordSha256 implementation).
 */
public class HashPassword {
    public static String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("Hashing failed", ex);
        }
    }

    public static void main(String[] args) {
        String pwd = null;
        if (args != null && args.length > 0) {
            pwd = args[0];
        } else {
            Console console = System.console();
            if (console != null) {
                char[] pass = console.readPassword("Enter password: ");
                pwd = new String(pass);
            } else {
                // fallback for environments where Console is not available (IDE/VSCode)
                System.out.print("Enter password: ");
                Scanner sc = new Scanner(System.in);
                pwd = sc.nextLine();
            }
        }

        if (pwd == null) {
            System.err.println("No password provided");
            System.exit(2);
        }

        String hashed = hashSha256(pwd);
        System.out.println(hashed);
    }
}

