package open.dolphin.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;

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

    public TaskCompletionService(Executor executor, BlockingQueue<Future<V>> completionQueue) {
        super(executor, completionQueue);
    }

    @Override
    public Future<V> submit(Callable<V> task) {
        try {
            Future<V> f = super.submit(task);
            taskCount++;
            return f;
        } catch (Exception ex) {
            throw ex;
        }
    }

    @Override
    public Future<V> submit(Runnable task, V result) {
        try {
            Future<V> f = super.submit(task, result);
            taskCount++;
            return f;
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
