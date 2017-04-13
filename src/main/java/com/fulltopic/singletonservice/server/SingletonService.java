package com.fulltopic.singletonservice.server;
/**
 * Created by zf on 17-4-1.
 */


import com.google.common.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SingletonService
{
    private static SingletonService Instance = new SingletonService();
    private static final Logger LOG = LoggerFactory.getLogger(SingletonService.class);
    private static final int NewExecutorServiceThreadNum = 8;

    private ConcurrentLinkedQueue<SingletonServiceJob> pendingRequests = new ConcurrentLinkedQueue<SingletonServiceJob>();
    private AtomicBoolean isWorking = new AtomicBoolean(false);
    private ListeningExecutorService executor = null;

    private SingletonService() {
        this.executor = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(NewExecutorServiceThreadNum));
    }

    public static SingletonService GetInstance()
    {
        return Instance;
    }

    public void sendRequest(int counter) {
        SingletonServiceJob singleJob = new SingletonServiceJob(counter);
        pendingRequests.add(singleJob);

        if(!isWorking.get()) {
            if(isWorking.compareAndSet(false, true)) {
                triggerNewRequest();
            }
        }else {
//            LOG.info("Someone else would process the request");
        }
    }


    private void triggerNewRequest() {
        if(pendingRequests.isEmpty()) {
            isWorking.set(false);
            return;
        }

        SingletonServiceJob job = pendingRequests.poll();
//        LOG.info("----------> TriggerNewRequest " + job.getSeq());

        ListenableFuture singleTask = executor.submit(job);
        Futures.addCallback(singleTask, new SingleJobCallback(), executor);
    }


    private class SingleJobCallback implements FutureCallback<Integer> {
        private void triggerRequest() {
            if(!pendingRequests.isEmpty()) {
                triggerNewRequest();
            }else {
                isWorking.set(false);

                if(!pendingRequests.isEmpty()) {
                    if(isWorking.compareAndSet(false, true)) {
                        triggerNewRequest();
                    }
                }
            }
        }

        public void onSuccess(Integer seq) {
//            LOG.info(Thread.currentThread().getName() + " Finished a job " + seq);
            triggerRequest();
        }

        public void onFailure(Throwable e) {
            LOG.error(Thread.currentThread().getName() + " Failed to run task " + e.getMessage());
            triggerRequest();
        }
    }

    //TODO: Meaning of this check?
    public void check() {
        LOG.error("pendingRequest " + pendingRequests.isEmpty() + " : " + pendingRequests.size());
    }
}
