class ServiceStatus {
    public int availableMemory;
    public int pendingTasks;

    public ServiceStatus(int availableMemory, int pendingTasks) {
        this.availableMemory = availableMemory;
        this.pendingTasks = pendingTasks;
    }
}