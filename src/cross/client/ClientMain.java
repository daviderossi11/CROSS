package cross.client;

public class ClientMain {

    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        if (args.length == 2) {
            host = args[0];
            port = Integer.parseInt(args[1]);
        }

        CROSSClient client = new CROSSClient(host, port);
        client.start();
    }
}
