package demo.verticle;

public class ServiceResult {
    private String serviceName;
    private String threadId;
    private String message;
    private int activityCount;
    private int subActivityCount;

    public ServiceResult(String serviceName, String threadId, String message, int activityCount, int subActivityCount) {
        this.serviceName = serviceName;
        this.threadId = threadId;
        this.message = message;
        this.activityCount = activityCount;
        this.subActivityCount = subActivityCount;
    }

    public ServiceResult(String serviceName, String threadId, int activityCount) {
        this(serviceName, threadId, "", activityCount, 0);
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getThreadId() {
        return threadId;
    }

    public String getMessage() {
        return message;
    }

    public int getActivityCount() {
        return activityCount;
    }

    public int getSubActivityCount() {
        return subActivityCount;
    }
}
