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

import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.pojoactor.ActorSystem;

import java.io.IOException;

/**
 * Interrupt test application demonstrating timeout and manual interrupt handling.
 * <p>
 * This test demonstrates how to interrupt a running Claude Code prompt and
 * continue with a new prompt while preserving session context. This is useful
 * when dealing with long-running operations that need to be cancelled.
 * </p>
 * <p>
 * Test scenarios include:
 * <ol>
 *   <li>Sending a potentially long-running prompt with timeout</li>
 *   <li>Interrupting the prompt if it exceeds timeout</li>
 *   <li>Sending a new prompt after interruption</li>
 *   <li>Verifying session context is preserved</li>
 * </ol>
 * </p>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public class ClaudeCodeInterruptTest {

    /**
     * Main entry point for the interrupt test.
     * <p>
     * Executes test scenarios that demonstrate:
     * <ul>
     *   <li>Setting up a session with initial context</li>
     *   <li>Sending a prompt with timeout constraint</li>
     *   <li>Manually interrupting a running prompt</li>
     *   <li>Resuming conversation after interrupt</li>
     *   <li>Verifying context preservation across interrupts</li>
     * </ul>
     * </p>
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        ActorSystem system = new ActorSystem.Builder("claude-interrupt-system").build();

        String sessionName = "claude-interrupt-" + System.currentTimeMillis();
        ClaudeCodeSession session = new ClaudeCodeSession(sessionName);

        ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);

        try {
            // Test 1: Establish initial context
            System.out.println("\n=== Test 1: Establish initial context ===");
            String prompt1 = "My name is Alice and I'm working on a machine learning project.";
            System.out.println("Sending: " + prompt1);

            ClaudeCodeOutput output1 = sessionRef.ask(s -> {
                try {
                    return s.sendPrompt(prompt1);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output ---");
            printOutput(output1);

            // Test 2: Send a prompt with timeout (simulating potential long operation)
            System.out.println("\n=== Test 2: Send prompt with timeout ===");
            String prompt2 = "Write a detailed explanation of neural networks.";
            System.out.println("Sending with 30-second timeout: " + prompt2);

            try {
                ClaudeCodeOutput output2 = sessionRef.ask(s -> {
                    try {
                        return s.sendPromptWithTimeout(prompt2, 30000);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }).get();

                System.out.println("\n--- Output (completed within timeout) ---");
                printOutput(output2);
            } catch (Exception e) {
                System.out.println("\n*** Timeout occurred! ***");
                System.out.println("Error: " + e.getMessage());
            }

            // Test 3: Manual interrupt scenario
            System.out.println("\n=== Test 3: Manual interrupt demonstration ===");
            System.out.println("Starting a potentially long task...");

            // Create a thread that will send a prompt
            Thread promptThread = new Thread(() -> {
                try {
                    sessionRef.ask(s -> {
                        try {
                            String longPrompt = "Generate a comprehensive tutorial on quantum computing " +
                                              "including all mathematical foundations.";
                            System.out.println("Sending long prompt: " + longPrompt);
                            return s.sendPrompt(longPrompt);
                        } catch (IOException e) {
                            System.out.println("Prompt was interrupted: " + e.getMessage());
                            return null;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }).get();
                } catch (Exception e) {
                    System.out.println("Thread interrupted: " + e.getMessage());
                }
            });

            promptThread.start();

            // Simulate user pressing Escape after 3 seconds
            System.out.println("Waiting 3 seconds then interrupting...");
            Thread.sleep(3000);

            System.out.println("*** Sending interrupt (simulating Escape key) ***");
            boolean interrupted = sessionRef.ask(s -> s.interrupt()).get();
            System.out.println("Interrupt result: " + (interrupted ? "Process interrupted" : "No process running"));

            promptThread.join(5000);

            // Test 4: Continue after interrupt - verify session is preserved
            System.out.println("\n=== Test 4: Continue after interrupt ===");
            String prompt4 = "Instead, just give me a brief one-sentence summary of quantum computing.";
            System.out.println("Sending: " + prompt4);

            ClaudeCodeOutput output4 = sessionRef.ask(s -> {
                try {
                    return s.sendPrompt(prompt4);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output ---");
            printOutput(output4);

            // Test 5: Verify context retention (from Test 1)
            System.out.println("\n=== Test 5: Verify initial context is preserved ===");
            String prompt5 = "What is my name and what am I working on?";
            System.out.println("Sending: " + prompt5);

            ClaudeCodeOutput output5 = sessionRef.ask(s -> {
                try {
                    return s.sendPrompt(prompt5);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output ---");
            printOutput(output5);

            System.out.println("\n=== Test completed ===");
            System.out.println("Successfully demonstrated interrupt and resume with context preservation");

        } catch (Exception e) {
            System.err.println("Test error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            system.terminate();
            System.out.println("Done.");
        }
    }

    /**
     * Prints a formatted summary of a ClaudeCodeOutput.
     *
     * @param output the output to print (null if interrupted)
     */
    private static void printOutput(ClaudeCodeOutput output) {
        if (output == null) {
            System.out.println("(No output - operation was interrupted)");
            return;
        }

        System.out.println("Type: " + output.getPromptType());
        System.out.println("Lines: " + output.getLines().size());

        if (output.getQuestion() != null) {
            System.out.println("Question: " + output.getQuestion());
        }

        System.out.println("\nContent:");
        for (String line : output.getLines()) {
            System.out.println(line);
        }
    }
}