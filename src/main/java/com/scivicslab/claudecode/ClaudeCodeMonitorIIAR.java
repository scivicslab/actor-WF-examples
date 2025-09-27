package com.scivicslab.claudecode;

import com.scivicslab.actorwf.ActionResult;
import com.scivicslab.actorwf.IIActorRef;

public class ClaudeCodeMonitorIIAR extends IIActorRef<ClaudeCodeMonitor> {

    public ClaudeCodeMonitorIIAR(String actorName, ClaudeCodeMonitor monitor) {
        super(actorName, monitor);
    }

    @Override
    public ActionResult callByActionName(String actionName, String args) {
        try {
            switch (actionName) {
                case "startMonitoring":
                    this.tell(m -> m.startMonitoring()).get();
                    return new ActionResult(true, "Monitoring started");

                case "stopMonitoring":
                    this.tell(m -> m.stopMonitoring()).get();
                    return new ActionResult(true, "Monitoring stopped");

                case "printLatestOutput":
                    this.tell(m -> m.printLatestOutput()).get();
                    return new ActionResult(true, "Latest output printed");

                case "checkMonitoring":
                    boolean isMonitoring = this.ask(m -> m.isMonitoring()).get();
                    return new ActionResult(true, "Monitoring: " + isMonitoring);

                case "getPromptType":
                    var output = this.ask(m -> m.getLatestOutput()).get();
                    if (output != null) {
                        return new ActionResult(true, "Prompt type: " + output.getPromptType());
                    } else {
                        return new ActionResult(true, "No output yet");
                    }

                default:
                    return new ActionResult(false, "Unknown action: " + actionName);
            }
        } catch (Exception e) {
            return new ActionResult(false, "Error: " + e.getMessage());
        }
    }
}