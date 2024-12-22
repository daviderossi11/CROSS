package cross.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class ClientMain {
    
    public static void main(String[] args) {
        Scanner scanner = null;
        Scanner in = null;

        try(Socket socket = new Socket("localhost", 1024)) {
            scanner = new Scanner(System.in);
            in = new Scanner(socket.getInputStream());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Enter your username: ");
            String username = scanner.nextLine();
            System.out.println("Enter your password: ");
            String password = scanner.nextLine();

            out.println(username);
            out.println(password);

            String response = in.nextLine();
            if(response.equals("Login successful")) {
                System.out.println("Login successful");
            } else {
                System.out.println("Login failed");
            }
        } catch (IOException e) {
            e.printStackTrace();
    }
    
}
