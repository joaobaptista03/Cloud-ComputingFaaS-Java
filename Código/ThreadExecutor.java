import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadExecutor {
    private final WorkerThread[] threads;
    private final LinkedList<Runnable> taskQueue;
    private final Lock queueLock;
    private final Condition queueNotEmpty;

    public ThreadExecutor(int poolSize) {
        this.threads = new WorkerThread[poolSize];
        this.taskQueue = new LinkedList<>();
        this.queueLock = new ReentrantLock();
        this.queueNotEmpty = queueLock.newCondition();

        for (int i = 0; i < poolSize; i++) {
            threads[i] = new WorkerThread();
            threads[i].start();
        }
    }

    public void submitTask(Runnable task) {
        queueLock.lock();
        try {
            taskQueue.addLast(task);
            queueNotEmpty.signal(); // Signal that the queue is not empty
        } finally {
            queueLock.unlock();
        }
    }

    private class WorkerThread extends Thread {
        @Override
        public void run() {
            while (true) {
                Runnable task;

                queueLock.lock();
                try {
                    while (taskQueue.isEmpty()) {
                        try {
                            queueNotEmpty.await(); // Wait until the queue is not empty
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    task = taskQueue.removeFirst();
                } finally {
                    queueLock.unlock();
                }

                try {
                    task.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
