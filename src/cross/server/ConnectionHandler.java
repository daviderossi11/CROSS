package cross.server;

import java.net.Socket;

import cross.order.OrderBook;
import cross.user.User;
import cross.user.UserManagement;
import java.io.PrintWriter;


public class ConnectionHandler implements Runnable {

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final UserManagement userManagement;
    private final OrderBook orderBook;
    private User user;


    public ConnectionHandler(Socket socket, UserManagement userManagement, OrderBook orderBook) {
        this.socket = socket;
        this.userManagement = userManagement;
        this.orderBook = orderBook;
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void run(){
        while(socket.isConnected()){
            try{
                String username = in.readLine();
                String password = in.readLine();
                int response = userManagement.login(username, password);
                if(response == 100){
                    user = userManagement.getUser(username);
                    out.println("Login successful");
                }else{
                    out.println("Login failed");
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }

    }
}