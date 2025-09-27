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

import java.util.List;

/**
 * Interactive test application demonstrating Claude Code choice handling.
 * <p>
 * This test demonstrates more complex interaction patterns where Claude Code
 * may present numbered choices or yes/no questions. It shows how to detect
 * and respond to these interactive prompts programmatically.
 * </p>
 * <p>
 * Test scenarios include:
 * <ol>
 *   <li>Asking Claude to present multiple options</li>
 *   <li>Detecting numbered choice prompts</li>
 *   <li>Selecting a specific option by number</li>
 *   <li>Verifying Claude recalls the selection</li>
 * </ol>
 * </p>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public class ClaudeCodeInteractiveTest {

    /**
     * Main entry point for the interactive test.
     * <p>
     * Executes a multi-step interaction that includes:
     * <ul>
     *   <li>Requesting Claude to present options for a programming language</li>
     *   <li>Analyzing the response to detect choice prompts</li>
     *   <li>Making a selection if choices are presented</li>
     *   <li>Verifying context retention of the selection</li>
     * </ul>
     * </p>
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        ActorSystem system = new ActorSystem.Builder("claude-interactive-system").build();

        String sessionName = "claude-interactive-" + System.currentTimeMillis();
        ClaudeCodeSession session = new ClaudeCodeSession(sessionName);

        ActorRef<ClaudeCodeSession> sessionRef = system.actorOf("session", session);

        try {
            // Test 1: Ask for a choice scenario
            System.out.println("\n=== Test 1: Request programming language recommendation ===");
            String prompt1 = "I'm starting a new web application project. Can you suggest 3 popular " +
                           "programming languages for web development and briefly explain each? " +
                           "Please present them as numbered options 1, 2, 3.";
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

            // Check if we got numbered choices
            if (output1.getPromptType() == PromptType.NUMBERED_CHOICE) {
                System.out.println("\n*** Detected numbered choices! ***");
                List<String> choices = output1.getChoices();
                System.out.println("Available choices:");
                for (int i = 0; i < choices.size(); i++) {
                    System.out.println("  " + (i + 1) + ". " + choices.get(i));
                }
            }

            // Test 2: Make a selection
            System.out.println("\n=== Test 2: Make a selection ===");
            String prompt2 = "I'll go with option 2. Can you tell me more about why it's a good choice?";
            System.out.println("Sending: " + prompt2);

            ClaudeCodeOutput output2 = sessionRef.ask(s -> {
                try {
                    return s.sendPrompt(prompt2);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output ---");
            printOutput(output2);

            // Test 3: Context retention check
            System.out.println("\n=== Test 3: Verify context retention ===");
            String prompt3 = "What programming language did I choose?";
            System.out.println("Sending: " + prompt3);

            ClaudeCodeOutput output3 = sessionRef.ask(s -> {
                try {
                    return s.sendPrompt(prompt3);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Output ---");
            printOutput(output3);

            // Test 4: Ask a yes/no question scenario
            System.out.println("\n=== Test 4: Ask about best practices ===");
            String prompt4 = "Should I use TypeScript instead of JavaScript for this project? " +
                           "Please answer with a yes or no and brief explanation.";
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

            if (output4.getPromptType() == PromptType.YES_NO) {
                System.out.println("\n*** Detected yes/no question! ***");
                System.out.println("Question: " + output4.getQuestion());
            }

            System.out.println("\n=== Test completed ===");
            System.out.println("Successfully demonstrated interactive conversation with choices");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            system.terminate();
            System.out.println("Done.");
        }
    }

    /**
     * Prints a formatted summary of a ClaudeCodeOutput.
     * <p>
     * Displays the prompt type, number of lines, actual content, and if applicable,
     * any detected choices or questions.
     * </p>
     *
     * @param output the output to print
     */
    private static void printOutput(ClaudeCodeOutput output) {
        System.out.println("Type: " + output.getPromptType());
        System.out.println("Lines: " + output.getLines().size());

        if (output.getQuestion() != null) {
            System.out.println("Question: " + output.getQuestion());
        }

        if (output.getChoices() != null && !output.getChoices().isEmpty()) {
            System.out.println("Choices detected: " + output.getChoices().size());
        }

        System.out.println("\nContent:");
        for (String line : output.getLines()) {
            System.out.println(line);
        }
    }
}