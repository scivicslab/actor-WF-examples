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

/**
 * Simple test application demonstrating basic Claude Code session interaction.
 * <p>
 * This test sends three simple prompts to Claude Code and captures the responses,
 * including a context retention check. It demonstrates the basic request-response
 * pattern using the actor-based API.
 * </p>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public class ClaudeCodeSimpleTest {

    /**
     * Main entry point for the simple test.
     * <p>
     * Executes three test scenarios:
     * <ol>
     *   <li>Simple math question (2 + 2)</li>
     *   <li>Another math question (5 * 3)</li>
     *   <li>Context retention check (recalls first question)</li>
     * </ol>
     * </p>
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        ActorSystem system = new ActorSystem.Builder("claude-simple-system").build();

        String sessionName = "claude-simple-" + System.currentTimeMillis();
        ClaudeCodeSession session = new ClaudeCodeSession(sessionName);

        ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);

        try {
            // Test 1
            System.out.println("\n=== Test 1: Simple math question ===");
            System.out.println("Sending: What is 2 + 2?");

            ClaudeCodeOutput output1 = sessionRef.ask(s -> {
                try {
                    return s.sendPrompt("What is 2 + 2?");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output ---");
            printOutput(output1);

            // Test 2
            System.out.println("\n=== Test 2: Another math question ===");
            System.out.println("Sending: What is 5 * 3?");

            ClaudeCodeOutput output2 = sessionRef.ask(s -> {
                try {
                    return s.sendPrompt("What is 5 * 3?");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output ---");
            printOutput(output2);

            // Test 3
            System.out.println("\n=== Test 3: Context check ===");
            System.out.println("Sending: What was my first question?");

            ClaudeCodeOutput output3 = sessionRef.ask(s -> {
                try {
                    return s.sendPrompt("What was my first question?");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output ---");
            printOutput(output3);

            System.out.println("\n=== Test completed ===");
            System.out.println("Successfully sent 3 prompts and captured responses");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            system.terminate();
            System.out.println("Done.");
        }
    }

    /**
     * Prints a formatted summary of a ClaudeCodeOutput.
     *
     * @param output the output to print
     */
    private static void printOutput(ClaudeCodeOutput output) {
        System.out.println("Type: " + output.getPromptType());
        System.out.println("Lines: " + output.getLines().size());
        for (String line : output.getLines()) {
            System.out.println(line);
        }
    }
}