package com.scivicslab.claudecode;

import com.scivicslab.actorwf.ActionResult;
import com.scivicslab.actorwf.IIActorRef;

public class ClaudeCodeSessionIIAR extends IIActorRef<ClaudeCodeSession> {

    public ClaudeCodeSessionIIAR(String actorName, ClaudeCodeSession session) {
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

                case "startClaudeCode":
                    this.tell(s -> {
                        try {
                            s.startClaudeCode();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Claude Code started");

                case "sendPrompt":
                    this.tell(s -> {
                        try {
                            s.sendPrompt(args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Prompt sent: " + args);

                case "sendChoice":
                    try {
                        int choice = Integer.parseInt(args);
                        this.tell(s -> {
                            try {
                                s.sendChoice(choice);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }).get();
                        return new ActionResult(true, "Choice sent: " + choice);
                    } catch (NumberFormatException e) {
                        return new ActionResult(false, "Invalid choice number: " + args);
                    }

                case "sendYes":
                    this.tell(s -> {
                        try {
                            s.sendYes();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Sent: yes");

                case "sendNo":
                    this.tell(s -> {
                        try {
                            s.sendNo();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Sent: no");

                case "sendEnter":
                    this.tell(s -> {
                        try {
                            s.sendEnter();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Sent: Enter");

                case "sendCtrlC":
                    this.tell(s -> {
                        try {
                            s.sendCtrlC();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Sent: Ctrl-C");

                case "captureAndParse":
                    var output = this.ask(s -> {
                        try {
                            return s.captureAndParse();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Captured: " + output.getPromptType());

                case "printOutput":
                    var lines = this.ask(s -> {
                        try {
                            return s.captureOutput();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    System.out.println("=== Claude Code Output ===");
                    for (String line : lines) {
                        System.out.println(line);
                    }
                    System.out.println("==========================");
                    return new ActionResult(true, "Printed output");

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