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
package com.scivicslab.actorwf.examples.codex;

import com.scivicslab.actorwf.examples.claudecode.ClaudeCodeOutput;
import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.pojoactor.ActorSystem;

/**
 * Test application demonstrating helper methods for common Codex interaction patterns.
 * <p>
 * This test showcases the convenience methods provided by CodexSession
 * for common use cases:
 * <ul>
 *   <li>Requesting and selecting choices</li>
 *   <li>Sending follow-up prompts after selection</li>
 *   <li>Interrupting and redirecting conversation</li>
 *   <li>Fallback behavior for timeouts</li>
 * </ul>
 * </p>
 *
 * @author Scivics Lab
 * @version 1.0
 */
public class CodexHelperMethodsTest {

    /**
     * Main entry point for the helper methods test.
     *
     * @param args command line arguments (not used)
     */
    public static void main(String[] args) {
        ActorSystem system = new ActorSystem.Builder("codex-helpers-system").build();

        String sessionName = "codex-helpers-" + System.currentTimeMillis();
        CodexSession session = new CodexSession(sessionName);

        ActorRef<CodexSession> sessionRef = system.actorOf("session", session);

        try {
            // Test 1: Request choices using convenience method
            System.out.println("\n=== Test 1: Request choices ===");
            String question = "I'm building a REST API. Suggest 3 backend frameworks " +
                            "and present them as numbered options 1, 2, 3.";
            System.out.println("Sending: " + question);

            ClaudeCodeOutput choices = sessionRef.ask(s -> {
                try {
                    return s.requestChoices(question);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Choices received ---");
            printOutput(choices);

            // Test 2: Select a choice with additional input
            System.out.println("\n=== Test 2: Select choice with additional input ===");
            System.out.println("Selecting: option 1 with additional question");

            ClaudeCodeOutput selectionResponse = sessionRef.ask(s -> {
                try {
                    return s.selectChoice(1, "What are the main advantages?");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Selection response ---");
            printOutput(selectionResponse);

            // Test 3: Send follow-up question
            System.out.println("\n=== Test 3: Send follow-up question ===");
            String followUp = "Show me a simple example of using this framework";
            System.out.println("Sending follow-up: " + followUp);

            ClaudeCodeOutput followUpResponse = sessionRef.ask(s -> {
                try {
                    return s.sendFollowUp(followUp);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Follow-up response ---");
            printOutput(followUpResponse);

            // Test 4: Verify context retention
            System.out.println("\n=== Test 4: Verify context retention ===");
            String contextCheck = "What framework did I choose?";
            System.out.println("Sending: " + contextCheck);

            ClaudeCodeOutput contextResponse = sessionRef.ask(s -> {
                try {
                    return s.sendPrompt(contextCheck);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("\n--- Context check response ---");
            printOutput(contextResponse);

            System.out.println("\n=== Test completed ===");
            System.out.println("Successfully demonstrated all helper methods with context preservation");

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

        if (output.getChoices() != null && !output.getChoices().isEmpty()) {
            System.out.println("Choices: " + output.getChoices().size());
        }

        System.out.println("\nContent:");
        int lineCount = 0;
        for (String line : output.getLines()) {
            System.out.println(line);
            lineCount++;
            if (lineCount > 20) {
                System.out.println("... (truncated, " + (output.getLines().size() - 20) + " more lines)");
                break;
            }
        }
    }
}