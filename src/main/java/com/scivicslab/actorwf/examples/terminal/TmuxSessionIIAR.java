/*
 * Copyright 2025 Scivics Lab
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.scivicslab.pojoactor.workflow.examples.terminal;

import com.scivicslab.pojoactor.ActionResult;
import com.scivicslab.pojoactor.workflow.IIActorRef;
import com.scivicslab.pojoactor.workflow.IIActorSystem;

/**
 * Actor reference wrapper for TmuxSession that enables workflow-based control.
 * This class extends IIActorRef to provide action-based interaction with a TmuxSession
 * instance through named actions.
 *
 * <p>Supported actions include:</p>
 * <ul>
 *   <li>createSession - creates a new tmux session</li>
 *   <li>sendCommand - sends a command with Enter to the session</li>
 *   <li>sendKeys - sends raw keystrokes to the session</li>
 *   <li>capturePane - captures the current pane contents</li>
 *   <li>printPane - captures and prints the pane contents</li>
 *   <li>checkPrompt - checks if a shell prompt is present</li>
 *   <li>killSession - terminates the tmux session</li>
 * </ul>
 */
public class TmuxSessionIIAR extends IIActorRef<TmuxSession> {

    /**
     * Creates a new TmuxSessionIIAR with the specified actor name and session instance.
     *
     * @param actorName the name to assign to this actor
     * @param session the TmuxSession instance to wrap
     */
    public TmuxSessionIIAR(String actorName, TmuxSession session) {
        super(actorName, session);
    }

    /**
     * Executes an action on the TmuxSession by action name.
     * Provides workflow integration by mapping action names to TmuxSession methods.
     *
     * @param actionName the name of the action to execute (createSession, sendCommand, sendKeys,
     *                   capturePane, printPane, checkPrompt, killSession)
     * @param args additional arguments for the action (used by sendCommand and sendKeys)
     * @return an ActionResult indicating success or failure with a descriptive message
     */
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