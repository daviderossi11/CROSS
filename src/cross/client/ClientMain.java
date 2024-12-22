package cross.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public class ClientMain {

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
        Gson gson = new Gson();

        try (Socket socket = new Socket("localhost", 1024)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            while (true) {
                System.out.println("Choose action or type 'exit' to quit: ");
                String action = scanner.nextLine();

                if (action.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting client...");
                    break;
                }

                JsonObject request = new JsonObject();

                switch (action) {
                    case "register" -> {
                        System.out.println("Enter username: ");
                        String regUsername = scanner.nextLine();
                        System.out.println("Enter password: ");
                        String regPassword = scanner.nextLine();
                        request.addProperty("action", "register");
                        request.addProperty("username", regUsername);
                        request.addProperty("password", regPassword);
                    }
                    case "login" -> {
                        System.out.println("Enter username: ");
                        String logUsername = scanner.nextLine();
                        System.out.println("Enter password: ");
                        String logPassword = scanner.nextLine();
                        request.addProperty("action", "login");
                        request.addProperty("username", logUsername);
                        request.addProperty("password", logPassword);
                    }
                    case "logout" -> {
                        scanner.nextLine(); // consume newline
                        request.addProperty("action", "logout");
                    }
                    case "updateCredentials" -> {
                        System.out.println("Enter username: ");
                        String upUsername = scanner.nextLine();
                        System.out.println("Enter password: ");
                        String upPassword = scanner.nextLine();
                        System.out.println("Enter new password: ");
                        String upNewPassword = scanner.nextLine();
                        request.addProperty("action", "updateCredentials");
                        request.addProperty("username", upUsername);
                        request.addProperty("old_password", upPassword);
                        request.addProperty("new_password", upNewPassword);
                    }
                    default -> {
                        System.out.println("Invalid action");
                        continue;
                    }
                }

                out.println(gson.toJson(request));
                String response = in.readLine();
                JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                System.out.println(jsonResponse.get("status").getAsString() + " - " + jsonResponse.get("message").getAsString());
            }

        } catch (IOException e) {
            System.err.println("Error: " + e);
        }
    }

    }

}
