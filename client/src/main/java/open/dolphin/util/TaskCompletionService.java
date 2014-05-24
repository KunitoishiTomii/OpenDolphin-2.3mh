package open.dolphin.util;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;

/**
 * Utility class for ExecutorCompletionService
 *
 * @author masuda, Masuda Naika
 */
public class TaskCompletionService<V> extends ExecutorCompletionService<V> {

    private int taskCount;

    public TaskCompletionService(Executor executor) {
        super(executor);
    }

    public void submitTask(Runnable task) {
        try {
            submit(task, null);
            taskCount++;
        } catch (Exception ex) {
            throw ex;
        }
    }

    public void waitCompletion() throws InterruptedException {
        for (int i = 0; i < taskCount; ++i) {
            take();
        }
    }

    public int getTaskCount() {
        return taskCount;
    }

    public void clearTaskCount() {
        taskCount = 0;
    }

}
