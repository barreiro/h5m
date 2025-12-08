package exp.queue;

import exp.entity.Value;
import exp.entity.Work;
import exp.svc.NodeService;
import exp.svc.ValueService;
import exp.svc.WorkService;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;

public class WorkRunner implements Runnable {

    public static final int RETRY_LIMIT = 3;

    NodeService nodeService;

    ValueService valueService;

    WorkService workService;

    Work work;
    private WorkQueue workQueue;

    private Runnable then;

    public WorkRunner(Work work,WorkQueue workQueue,NodeService nodeService,ValueService valueService,WorkService workService){
        this.work = work;
        this.workQueue = workQueue;
        this.nodeService = nodeService;
        this.valueService = valueService;
        this.workService = workService;

    }

    public WorkRunner then(Runnable then){
        this.then = then;
        return this;
    }

    @Override
    @Transactional
    public void run() {
        try {
            if(work.activeNode==null || work.sourceValues == null || work.sourceValues.isEmpty()){
                //error conditions
            }
            //looping over values works for Jq / Js nodes but what about cross test comparison
            //calculateValue should probably accept all sourceValues and leave it to the node function to decide
            List<Value> calculated = nodeService.calculateValues(work.activeNode,work.sourceValues);
            //TODO purge existing values
            for(Value v : work.sourceValues) {
                valueService.deleteDescendantValues(v, work.activeNode);
            }
            //TODO change drop drop and replace to update data?
            //does this break descendant values or do we assume they will recalculate?
            for(Value newValue : calculated){
                valueService.create(newValue);
            }
            if(then!=null){
                then.run();
            }
            //not in the finally so that it only happens if the work succeeds
            //TODO this is throwing TransactionRequiredException
            workService.delete(work);
        }catch( Exception e){
            //TODO how to handle the exception, adding it back to the todo list
            e.printStackTrace();
            work.retryCount++;
            if(work.retryCount > RETRY_LIMIT){
                System.err.println("Work exceeded retry limit");
            } else {
                System.err.println("Adding work to retry in queue");
                workQueue.add(this);
            }
        } finally {
            if(work.activeNode!=null){
                workQueue.decrement(work);
            }
        }
    }


}
