package target;

public class TargetForWorker {
    private String name;
    private Target.Result result;
    private Target.Status status;
    private String runLog;
    private String extraData;
    private String taskName;


    public TargetForWorker(String name, String extraData, String taskName) {
        this.name = name;
        this.extraData = extraData;
        this.runLog = "";
        this.taskName = taskName;
    }

    public String getName() {
        return name;
    }

    public Target.Result getResult() {
        return result;
    }

    public void setResult(Target.Result result) {
        this.result = result;
    }

    public String getRunLog() {
        return runLog;
    }

    public void setRunLog(String runLog) {
        this.runLog = runLog;
    }

    public Target.Status getStatus() {
        return status;
    }

    public void setStatus(Target.Status status) {
        this.status = status;
    }

    public String getExtraData() {
        return extraData;
    }

    public String getTaskName() {
        return taskName;
    }
}
