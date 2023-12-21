/**
 * Represents the status of a service.
 */
class ServiceStatus {
    public int availableMemory;
    public int pendingTasks;

    /**
     * Constructs a ServiceStatus object with the specified available memory and pending tasks.
     * 
     * @param availableMemory the amount of available memory
     * @param pendingTasks the number of pending tasks
     */
    public ServiceStatus(int availableMemory, int pendingTasks) {
        this.availableMemory = availableMemory;
        this.pendingTasks = pendingTasks;
    }
}