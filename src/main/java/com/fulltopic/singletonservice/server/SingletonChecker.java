package com.fulltopic.singletonservice.server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by zf on 17-4-5.
 */
public class SingletonChecker {
    private ScheduledExecutorService service;
    private static final int DELAY = 100;
    private static final int INITIALDELAY = 100;
    private static final int THREADNUM = 1;
    private Runnable task;

    public SingletonChecker() {
        service = Executors.newScheduledThreadPool(THREADNUM);
//        task = new CheckTask();
    }

    public void check() {
        service.scheduleAtFixedRate(new CheckTask(), INITIALDELAY, DELAY, TimeUnit.MICROSECONDS);
        service.scheduleAtFixedRate(new CheckEmptyTask(), INITIALDELAY, DELAY * 10000, TimeUnit.MICROSECONDS);
    }

    private class CheckTask implements Runnable {
        public void run() {
            SingletonServiceJob.IsLegal();
            SingletonService.GetInstance().check();
        }
    }

    private class CheckEmptyTask implements Runnable {
        public void run() {SingletonService.GetInstance().check();}
    }
}
