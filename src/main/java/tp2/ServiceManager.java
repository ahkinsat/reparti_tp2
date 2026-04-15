package tp2;

import tp2.bo.OutboxProcessor;
import tp2.ho.EventConsumer;

import java.util.concurrent.ConcurrentHashMap;

public class ServiceManager {
    private static final ConcurrentHashMap<Integer, OutboxProcessor> boProcessors = new ConcurrentHashMap<>();
    private static EventConsumer hoConsumer = null;

    public static void startBoService(int boNumber) {
        boProcessors.computeIfAbsent(boNumber, n -> {
            OutboxProcessor proc = new OutboxProcessor(n);
            proc.start();
            return proc;
        });
    }

    public static void stopBoService(int boNumber) {
        OutboxProcessor proc = boProcessors.remove(boNumber);
        if (proc != null) proc.stop();
    }

    public static boolean isBoServiceRunning(int boNumber) {
        OutboxProcessor proc = boProcessors.get(boNumber);
        return proc != null && proc.isRunning();
    }

    public static void startHoService() {
        if (hoConsumer == null) {
            hoConsumer = new EventConsumer();
            hoConsumer.start();
        }
    }

    public static void stopHoService() {
        if (hoConsumer != null) {
            hoConsumer.stop();
            hoConsumer = null;
        }
    }

    public static boolean isHoServiceRunning() {
        return hoConsumer != null && hoConsumer.isRunning();
    }
}
