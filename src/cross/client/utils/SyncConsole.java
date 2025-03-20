package cross.client.utils;




/*
 * SyncConsole class per la sincronizzazione della stampa su console
 */
public class SyncConsole {
    public static final Object lock = new Object();
 

    public static void print(String message) {
        synchronized (lock) {
            System.out.println(message);
        }
    }
    
    
}
