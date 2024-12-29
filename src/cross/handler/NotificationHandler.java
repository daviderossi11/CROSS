package cross.handler;

import cross.util.Session;
import java.util.concurrent.PriorityBlockingQueue;

public class NotificationHandler implements Runnable {
    private final PriorityBlockingQueue<Session> queue;

    public NotificationHandler(PriorityBlockingQueue<Session> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        while (Thread.currentThread().isAlive()) {
            try {
                Session session = queue.take();
                session.sendNotification();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    
}
