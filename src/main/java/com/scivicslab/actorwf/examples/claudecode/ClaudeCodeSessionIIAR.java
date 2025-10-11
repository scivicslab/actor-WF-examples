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
package com.scivicslab.actorwf.examples.claudecode;

import com.scivicslab.actorwf.ActionResult;
import com.scivicslab.actorwf.IIActorRef;
import com.scivicslab.actorwf.IIActorSystem;

/**
 * Interpreter-interfaced actor reference for Claude Code session instances.
 *
 * <p>This class wraps a ClaudeCodeSession object and allows it to be invoked
 * by the workflow interpreter using string-based action names. It provides
 * a bridge between the workflow system and the Claude Code session implementation,
 * enabling workflow-driven Claude Code interaction.</p>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public class ClaudeCodeSessionIIAR extends IIActorRef<ClaudeCodeSession> {

    private ClaudeCodeOutput lastOutput;

    /**
     * Constructs a new ClaudeCodeSessionIIAR with the specified actor name and session instance.
     *
     * @param actorName the name of the actor
     * @param session the Claude Code session instance
     * @param system the actor system managing this actor
     */
    public ClaudeCodeSessionIIAR(String actorName, ClaudeCodeSession session, IIActorSystem system) {
        super(actorName, session, system);
    }

    /**
     * Invokes a Claude Code session action by name.
     *
     * <p>Supported actions:</p>
     * <ul>
     * <li>sendPrompt - Send a prompt (requires prompt text as argument)</li>
     * <li>requestChoices - Request numbered choices (requires prompt as argument)</li>
     * <li>selectChoice - Select a choice by number (requires choice number as argument)</li>
     * <li>sendFollowUp - Send a follow-up prompt (requires prompt as argument)</li>
     * <li>interruptAndSendNew - Interrupt and send new prompt (requires prompt as argument)</li>
     * <li>resetSession - Reset the session (no argument)</li>
     * <li>printLastOutput - Print the last output (no argument)</li>
     * </ul>
     *
     * @param actionName the name of the action to execute
     * @param args arguments for the action (can be empty string)
     * @return an ActionResult indicating success or failure
     */
    @Override
    public ActionResult callByActionName(String actionName, String args) {
        try {
            switch (actionName) {
                case "sendPrompt":
                    lastOutput = this.ask(s -> {
                        try {
                            return s.sendPrompt(args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Prompt sent: " + args);

                case "requestChoices":
                    lastOutput = this.ask(s -> {
                        try {
                            return s.requestChoices(args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Choices requested");

                case "selectChoice":
                    int choiceNum = Integer.parseInt(args.trim());
                    lastOutput = this.ask(s -> {
                        try {
                            return s.selectChoice(choiceNum);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Selected choice: " + choiceNum);

                case "sendFollowUp":
                    lastOutput = this.ask(s -> {
                        try {
                            return s.sendFollowUp(args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Follow-up sent: " + args);

                case "interruptAndSendNew":
                    lastOutput = this.ask(s -> {
                        try {
                            return s.interruptAndSendNew(args);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                    return new ActionResult(true, "Interrupted and sent new: " + args);

                case "resetSession":
                    this.tell(s -> s.resetSession()).get();
                    return new ActionResult(true, "Session reset");

                case "printLastOutput":
                    if (lastOutput != null) {
                        System.out.println("\n=== Claude Code Output ===");
                        System.out.println("Type: " + lastOutput.getPromptType());
                        System.out.println("Lines: " + lastOutput.getLines().size());
                        for (String line : lastOutput.getLines()) {
                            System.out.println(line);
                        }
                        System.out.println("=========================\n");
                    } else {
                        System.out.println("No output available yet");
                    }
                    return new ActionResult(true, "Output printed");

                default:
                    return new ActionResult(false, "Unknown action: " + actionName);
            }
        } catch (Exception e) {
            return new ActionResult(false, "Error: " + e.getMessage());
        }
    }

    /**
     * Gets the last output from Claude Code.
     *
     * @return the last ClaudeCodeOutput, or null if no output yet
     */
    public ClaudeCodeOutput getLastOutput() {
        return lastOutput;
    }
}