package cross.client;

import java.io.*;
import java.net.*;
import java.util.Properties;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ClientMain {
    public static void main(String[] args) {
        try {
            Properties config = ReadConfig("client.properties");
            String host = config.getProperty("host", "localhost");
            int port = Integer.parseInt(config.getProperty("port", "8080"));

            UdpHandler udpHandler = new UdpHandler();
            Thread notificationThread = new Thread(udpHandler);
            notificationThread.start();

            try (Socket socket = new Socket(host, port);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 Scanner scanner = new Scanner(System.in)) {

                Gson gson = new Gson();
                boolean isLoggedIn = false;
                while (true) {
                    System.out.println("Choose action or type 'exit' to quit: ");
                    String action = scanner.nextLine();

                    if ("exit".equalsIgnoreCase(action)) {
                        System.out.println("Exiting client...");
                        break;
                    }

                    udpHandler.saveConsoleOutput("> " + action + "\n");
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
                            request.addProperty("action", "logout");
                            isLoggedIn = false;
                        }
                        case "updateCredentials" -> {
                            System.out.println("Enter username: ");
                            String username = scanner.nextLine();
                            System.out.println("Enter current password: ");
                            String oldPassword = scanner.nextLine();
                            System.out.println("Enter new password: ");
                            String newPassword = scanner.nextLine();
                            request.addProperty("action", "updateCredentials");
                            request.addProperty("username", username);
                            request.addProperty("old_password", oldPassword);
                            request.addProperty("new_password", newPassword);
                        }
                        case "InsertLimitOrder", "InsertMarketOrder", "InsertStopOrder" -> {
                            if (!isLoggedIn) {
                                System.out.println("You must be logged in to perform this action.");
                                continue;
                            }
                            System.out.println("Enter type (ask/bid): ");
                            String type = scanner.nextLine();

                            if (!type.equals("ask") && !type.equals("bid")) {
                                System.out.println("Invalid type. Must be 'ask' or 'bid'.");
                                continue;
                            }

                            System.out.println("Enter size: ");
                            int size = scanner.nextInt();
                            scanner.nextLine(); // Clear buffer

                            if (size <= 0 || size >= Integer.MAX_VALUE) {
                                System.out.println("Invalid size. Must be > 0 and < 2^31.");
                                continue;
                            }

                            request.addProperty("action", action);
                            request.addProperty("type", type);
                            request.addProperty("size", size);
                            
                            if (!action.equals("InsertMarketOrder")) {
                                System.out.println("Enter price: ");
                                int price = scanner.nextInt();
                                scanner.nextLine(); // Clear buffer

                                if (price <= 0 || price >= Integer.MAX_VALUE) {
                                    System.out.println("Invalid price. Must be > 0 and < 2^31.");
                                    continue;
                                }
                                request.addProperty("price", price);
                            }
                        }
                        case "getPriceHistory" -> {
                            if (!isLoggedIn) {
                                System.out.println("You must be logged in to perform this action.");
                                continue;
                            }
                            System.out.println("Enter month and year (MMYYYY): ");
                            String monthYear = scanner.nextLine();
                            request.addProperty("action", "getPriceHistory");
                            request.addProperty("monthYear", monthYear);

                            out.println(gson.toJson(request)); // Send request to server
                            String response = in.readLine();
                            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);

                            if (jsonResponse.has("error") && jsonResponse.get("error").getAsInt() == -1) { // Check error
                                System.out.println("Data not available for the selected month.");
                            } else { // Display received data
                                System.out.println("Price history received:");
                                JsonArray data = jsonResponse.getAsJsonArray("data");
                                data.forEach(item -> {
                                    JsonObject entry = item.getAsJsonObject();
                                    System.out.println("Date: " + entry.get("date").getAsString());
                                    System.out.println("Open: " + entry.get("openPrice").getAsInt());
                                    System.out.println("Close: " + entry.get("closePrice").getAsInt());
                                    System.out.println("High: " + entry.get("maxPrice").getAsInt());
                                    System.out.println("Low: " + entry.get("minPrice").getAsInt());
                                });
                            }
                        }
                        case "cancelOrder" -> {
                            if (!isLoggedIn) {
                                System.out.println("You must be logged in to perform this action.");
                                continue;
                            }
                            System.out.println("Enter order ID: ");
                            int orderId = scanner.nextInt();
                            scanner.nextLine(); // Clear buffer
                            request.addProperty("action", "cancelOrder");
                            request.addProperty("orderId", orderId);
                        }
                        default -> {
                            System.out.println("Invalid action. Please try again.");
                            continue;
                        }
                    }

                    out.println(gson.toJson(request));
                    String response = in.readLine();
                    System.out.println("Server response: " + response);

                    if (action.equals("login")) {
                        JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
                        if (jsonResponse.get("code").getAsInt() == 100) {
                            isLoggedIn = true;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                udpHandler.stop();
                System.out.println("Client terminated.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Properties ReadConfig(String fileName) throws IOException {
        Properties properties = new Properties();
        try (FileReader reader = new FileReader(fileName)) {
            properties.load(reader);
        }
        return properties;
    }
}
