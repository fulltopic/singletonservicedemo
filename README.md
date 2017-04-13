# Prove the correctness of this demo

Let's define

* Request thread = thread that calls SingleService.sendRequest(). It belongs to netty IO thread pool.
* Corker thread = thread that works with SingletonServiceJob.call() and SingletonJobCallback. It belongs to newFixedThreadPool.

And these obvious facts:
* ConcurrentLinkedQueue is wait-free.
* Only worker thread can set the SingletonService.isWorking = F 

### Mutual Exclusive

This is to prove that at anytime, there is only one (or none) SingletonServiceJob.call() running. 

Suppose it is not true, there are 3 cases:

#### Two threads called SingletonService.triggerNewRequest() concurrently

When worker thread is working, the SingletonService.isWorking == True as the any chance this Atomic turned to False is in SingletonJobCallback. 
And the worker thread will hold the SingleService.isWorking till the SingletonJobCallback. 
So, a new request would not be triggered before the end of the previous one.

##### Tow request threads called SingletonService.triggerNewRequest() concurrently

By mutual exclusiveness of SingletonService.isWorking, there is no chance that 2 request threads called SingletonService.triggerNewRequest() concurrently.

##### Request thread and worker thread called SingletonService.triggerNewRequest() concurrently

We have sequence for request thread:
Request.w(pendingRequest) -> Request.r(isWorking) = F -> Request.CAS(isWorking) = T -> Request.call(triggerNewRequest)

And sequence for worker thread:
Worker.r(pendingRequest) = Empty -> Worker.w(isWorking = F) -> Worker.r(pendingRequest) = !Empty -> Worker.CAS(isWorking) = T -> Worker.call(triggerNewRequest)

If they both invoked SingletonService.triggerNewRequest, the sequence should be:
Worker.r(pendingRequest) = Empty -> (Request.w(pendingRequest), Worker.w(isWorking = F)) -> (Request.r(isWorking) = F, Worker.r(pendingRequest) = !Empty)
 -> **Request.CAS(isWorking) = T -> Worker.CAS(isWorking) = T** 
 -> (Request.call(triggerNewRequest), Worker.call(triggerNewRequest))
 
That is conflicted.

##### 2 worker threads called SingletonService.triggerNewRequest() concurrently

The precondition is that there should have been 2 worker threads <- SingletonService.triggerNewRequest() was called by 2 threads concurrently.
In initiation, there are only request threads, and it has been proved no SingletonService.triggerNewRequest() called concurrently by request threads.

So, it is impossible.

#### executor submitted 2 jobs before running one of them 

* For one request in SingletonService.pendingRequests, following sequence would be followed:
T.triggerNewRequest -> T.run(job.call()) -> T.callback -> T.triggerNewRequest
T would be same thread or different threads.
* After triggerNewRequest() called and before SingleJobCallback called, the flag isWorking == T.
* We have proved that SingletonService.triggerNewRequest() would not be called concurrently. 

Then if 2 jobs submitted before any one had been executed, the function triggerNewRequest was called twice sequentially without SingleJobCallback executed in between.
Suppose 
A.triggerNewRequest -> H -> B.triggerNewRequst, T.triggerRequest() does not belong to H

* A.triggerNewRequest < B.w(isWorking = T) -> B.triggerNewRequest
* B.triggerNewRequest < A.w(isWorking = T) -> A.triggerNewRequest

That is impossible.

Therefor, the execution sequence guarantee that one request can only be processed after the process of previous request returned.  

### Liveness

This is to prove that any request in SingletonService.pendingRequests would be served.

If SingleService.pendingRequests is not empty and while all thread died without dealing with the pending request, the threads are in following sequence:

1. R.w(isEmpty = F) -> R.r(isWorking) = T -> give up
2. R.w(isEmpty = F) -> R.r(isWorking) = F -> R.CAS(isWorking) = F -> give up

Precondition: isWorking = T
3. W'.w(isEmpty = T) -> W.r(isEmpty) = T -> W.w(isWorking = F) -> W.r(isEmpty) = T -> giveup
4. W'.w(isEmpty = T) -> W.r(isEmpty) = T -> W.w(isWorking = F) -> W.r(isEmpty) = F -> W.CAS(isWorking) = F -> giveup
5. W'.w(isEmpty = T) -> W.r(isEmpty) = T -> W.w(isWorking = F) -> W.r(isEmpty) = F -> W.CAS(isWorking) = T -> W.r(isEmpty) = T -> giveup

Precondition: isWorking = T
6. W.r(isEmpty) = F -> W.r(isEmpty) = T -> W.w(isWorking = F) -> giveup


According to previous prove, there is only one W living in these sequences.
- Case(6) is impossible as no one turns isEmpty = T. 
- For last W.r(isEmpty) of {3, 4, 5}, if W.r(isEmpty) = T, the request has not been added into SingletonService.pendingRequests, the request thread would deal with the new request.
- If W.r(isEmpty) = F, the winner of CAS would deal with the new request. 


### Lockless

The algorithm is lockless as the request would be appended into SingletonService.pendingRequests if the participant threads running indefinitely long.
In corresponding ODL case, it is lockless as the RPC always returns

### Waitless

It is not waitless as the implementation depends on OS scheduler.