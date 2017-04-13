package com.fulltopic.singletonservice.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by zf on 17-4-1.
 */
public class SingletonServiceJob implements Callable<Integer>{
    //To ensure the counter counts
    private AtomicInteger counter = new AtomicInteger(0);
    private final int count;
    private final static Logger LOG = LoggerFactory.getLogger(SingletonServiceJob.class);
    private static AtomicInteger seqer = new AtomicInteger(0);
    private int seq = 0;

    private static int requestCount = 0;
    private static int responseCount = 0;

    public SingletonServiceJob(int count) {
        this.count = count;
        this.seq = seqer.getAndAdd(1);
    }

    public Integer call() {
        requestCount ++;

//        LOG.info("running job " + this + " for counter " + counter + " by " + Thread.currentThread().getName());
        for (int i = 0; i < count; i ++) {
            counter.incrementAndGet();
        }

//        LOG.info("End of running job " + this + " by " + Thread.currentThread().getName());
        responseCount ++;
        return this.seq;
    }

    public int getSeq() {
        return this.seq;
    }

    public static boolean IsLegal() {
        if((requestCount - responseCount) <= 1) {
//            LOG.info("----------> OK");
            return true;
        }else {
            LOG.error("Mismatching request/response " + requestCount + " : " + responseCount);
            return false;
        }
    }
}
