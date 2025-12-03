package exp.provided;

import exp.queue.WorkQueue;
import exp.queue.WorkQueueExecutor;
import io.hyperfoil.tools.yaup.AsciiArt;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadPoolExecutor;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
public class ExecutorConfigurationTest {

    @Inject
    @Named("workExecutor")
    WorkQueueExecutor executor;

    @Test
    public void initialize_with_inject() {
        WorkQueue workQueue = executor.getWorkQueue();
        assertNotNull(workQueue,"queue should not be null");
        assertTrue(workQueue.hasWorkService(),"queue should have a work service");
        assertTrue(workQueue.hasNodeGroupService(),"queue should have a node group service");
        assertTrue(workQueue.hasNodeService(),"queue should have a node service");
        assertTrue(workQueue.hasValueService(),"queue should have a value service");
    }
}
