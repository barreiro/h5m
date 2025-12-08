package exp.queue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class WorkQueueExecutor extends ThreadPoolExecutor {


    public WorkQueueExecutor(){
        super(1,50,1,TimeUnit.SECONDS,new WorkQueue(null,null,null));
    }
    public WorkQueueExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, WorkQueue workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public WorkQueue getWorkQueue(){
        return (WorkQueue) getQueue();
    }
}
