import java.util.LinkedList;

public class SimpleThreadExecutor {
    private final WorkerThread[] threads;
    private final LinkedList<Runnable> taskQueue;

    public SimpleThreadExecutor(int poolSize) {
        this.threads = new WorkerThread[poolSize];
        this.taskQueue = new LinkedList<>();

        for (int i = 0; i < poolSize; i++) {
            threads[i] = new WorkerThread();
            threads[i].start();
        }
    }

    public void submitTask(Runnable task) {
        synchronized (taskQueue) {
            taskQueue.addLast(task);
            taskQueue.notify();
        }
    }

    private class WorkerThread extends Thread {
        @Override
        public void run() {
            while (true) {
                Runnable task;

                synchronized (taskQueue) {
                    while (taskQueue.isEmpty()) {
                        try {
                            taskQueue.wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }

                    task = taskQueue.removeFirst();
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
