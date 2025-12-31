package ch.adjudicator.agent.clock;

import java.util.concurrent.atomic.AtomicLong;

public class FastClockSource {
    
    public static final int TIME_INCREMENT_MS = 100;
    public AtomicLong timeMs = new AtomicLong(System.currentTimeMillis());
    
    public FastClockSource() {
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(TIME_INCREMENT_MS);
                    timeMs.set(System.currentTimeMillis());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    public long getTimeMs() {
        return timeMs.get();
    }
}
