package com.scivicslab.terminal;

import com.scivicslab.actorwf.ActionResult;
import com.scivicslab.actorwf.IIActorRef;
import com.scivicslab.actorwf.IIActorSystem;

public class TmuxSessionIIAR extends IIActorRef<TmuxSession> {

    public TmuxSessionIIAR(String actorName, TmuxSession session) {
        super(actorName, session);
    }

    @Override
    public ActionResult callByActionName(String actionName, String args) {
        try {
            switch (actionName) {
                case "createSession":
                    this.tell(s -> {
                        try {
                            s.createSession();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Session created");

                case "sendCommand":
                    this.tell(s -> {
                        try {
                            s.sendCommand(args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Command sent: " + args);

                case "sendKeys":
                    this.tell(s -> {
                        try {
                            s.sendKeys(args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Keys sent: " + args);

                case "capturePane":
                    var lines = this.ask(s -> {
                        try {
                            return s.capturePane();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Captured " + lines.size() + " lines");

                case "printPane":
                    var output = this.ask(s -> {
                        try {
                            return s.capturePane();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    System.out.println("=== Tmux Pane Output ===");
                    for (String line : output) {
                        System.out.println(line);
                    }
                    System.out.println("========================");
                    return new ActionResult(true, "Printed " + output.size() + " lines");

                case "checkPrompt":
                    boolean hasPrompt = this.ask(s -> {
                        try {
                            return s.hasPrompt();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Has prompt: " + hasPrompt);

                case "killSession":
                    this.tell(s -> {
                        try {
                            s.killSession();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Session killed");

                default:
                    return new ActionResult(false, "Unknown action: " + actionName);
            }
        } catch (Exception e) {
            return new ActionResult(false, "Error: " + e.getMessage());
        }
    }
}