package exp.queue;

import exp.entity.Node;
import exp.entity.Work;
import exp.entity.node.JqNode;
import exp.svc.WorkService;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class WorkQueueTest {

    @Inject
    TransactionManager tm;

    @Inject
    WorkService workService;

    @Test
    public void poll_null_until_source_completes() throws InterruptedException, SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        WorkQueue q = new WorkQueue(workService,null,null,null);

        tm.begin();
        Node aNode = new JqNode("a");
        aNode.persist();
        Node bNode = new JqNode("b");
        bNode.sources= List.of(aNode);
        bNode.persist();

        Work aWork = new Work(aNode,null,null);
        aWork.persist();
        Work bWork = new Work(bNode,bNode.sources,null);
        bWork.persist();
        tm.commit();

        q.addWork(bWork);
        q.addWork(aWork);

        Runnable firstRunnable = q.take();
        assertNotNull(firstRunnable);

        assertFalse(q.hasWork(aWork),"a should be removed from the q");
        assertTrue(q.hasWork(bWork),"b should remain in the queue");

        Runnable polled = q.poll();

        assertNull(polled,"b should remain in q until a completes");

        firstRunnable.run();

        polled = q.poll();
        assertNotNull(polled,"b work should return now that a is complete");
    }

    @Test
    public void poll_return_first_non_blocked() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
        WorkQueue q = new WorkQueue(workService,null,null,null);

        tm.begin();
        Node aNode = new JqNode("a");
        aNode.persist();
        Node bNode = new JqNode("b");
        bNode.sources= List.of(aNode);
        bNode.persist();
        Node cNode = new JqNode("c");
        cNode.persist();

        Work aWork = new Work(aNode,null,null);
        aWork.persist();
        Work bWork = new Work(bNode,bNode.sources,null);
        bWork.persist();
        Work cWork = new Work(cNode,cNode.sources,null);
        cWork.persist();
        tm.commit();

        q.addWork(aWork);
        q.addWork(bWork);
        q.addWork(cWork);

        Runnable firstRunnable = q.poll();
        assertFalse(q.hasWork(aWork),"a should be removed from the q");

        Runnable polled = q.poll();

        assertNotNull(polled,"poll should pull next runnable work");
        assertTrue(q.hasWork(bWork),"b should remain in the queue");
        assertFalse(q.hasWork(cWork),"c should not be in the queue");
    }


}
