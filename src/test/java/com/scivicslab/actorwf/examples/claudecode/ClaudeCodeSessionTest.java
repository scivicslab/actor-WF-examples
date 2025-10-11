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
package com.scivicslab.pojoactor.workflow.examples.claudecode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.pojoactor.ActorSystem;

import java.util.logging.Logger;

@DisplayName("ClaudeCode session management")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClaudeCodeSessionTest {

    private static final Logger logger = Logger.getLogger(ClaudeCodeSessionTest.class.getName());

    private ActorSystem system;
    private ActorRef<ClaudeCodeSession> sessionRef;
    private ClaudeCodeOutput firstOutput;
    private ClaudeCodeOutput secondOutput;
    private ClaudeCodeOutput contextCheckOutput;

    @BeforeAll
    public void setUp() {
        logger.info("Setting up ClaudeCode session test");
        system = new ActorSystem.Builder("claude-test-system").build();
        String sessionName = "claude-test-" + System.currentTimeMillis();
        ClaudeCodeSession session = new ClaudeCodeSession(sessionName);
        sessionRef = system.actorOf("session", session);
    }

    @AfterAll
    public void tearDown() {
        logger.info("Tearing down ClaudeCode session test");
        if (system != null) {
            system.terminate();
        }
    }

    @Test
    @Order(1)
    @DisplayName("should send first prompt and receive response")
    public void testSendFirstPrompt() throws Exception {
        logger.info("Test: sending first prompt");

        firstOutput = sessionRef.ask(s -> {
            try {
                return s.sendPrompt("What is 2 + 2?");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).get();

        assertNotNull(firstOutput, "Output should not be null");
        assertTrue(firstOutput.getLines().size() > 0, "Output should have lines");
        assertEquals(PromptType.RESPONSE, firstOutput.getPromptType(), "Should be a response type");

        logger.info("First prompt test passed");
    }

    @Test
    @Order(2)
    @DisplayName("should send second prompt and receive response")
    public void testSendSecondPrompt() throws Exception {
        logger.info("Test: sending second prompt");

        secondOutput = sessionRef.ask(s -> {
            try {
                return s.sendPrompt("What is 5 * 3?");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).get();

        assertNotNull(secondOutput, "Output should not be null");
        assertTrue(secondOutput.getLines().size() > 0, "Output should have lines");
        assertEquals(PromptType.RESPONSE, secondOutput.getPromptType(), "Should be a response type");

        logger.info("Second prompt test passed");
    }

    @Test
    @Order(3)
    @DisplayName("should retain context from previous prompts")
    public void testContextRetention() throws Exception {
        logger.info("Test: checking context retention");

        contextCheckOutput = sessionRef.ask(s -> {
            try {
                return s.sendPrompt("What was my first question?");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).get();

        assertNotNull(contextCheckOutput, "Output should not be null");
        assertTrue(contextCheckOutput.getLines().size() > 0, "Output should have lines");
        assertEquals(PromptType.RESPONSE, contextCheckOutput.getPromptType(), "Should be a response type");

        String content = String.join(" ", contextCheckOutput.getLines()).toLowerCase();
        assertTrue(content.contains("2") && content.contains("2"),
                  "Response should reference the first question about 2 + 2");

        logger.info("Context retention test passed");
    }

    @Test
    @Order(4)
    @DisplayName("should handle choice selection")
    public void testChoiceSelection() throws Exception {
        logger.info("Test: choice selection");

        ClaudeCodeOutput choicesOutput = sessionRef.ask(s -> {
            try {
                return s.requestChoices("Suggest 3 programming languages as numbered options 1, 2, 3");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).get();

        assertNotNull(choicesOutput, "Choices output should not be null");
        assertTrue(choicesOutput.getLines().size() > 0, "Should have choices");

        ClaudeCodeOutput selectionOutput = sessionRef.ask(s -> {
            try {
                return s.selectChoice(1);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).get();

        assertNotNull(selectionOutput, "Selection output should not be null");
        assertEquals(PromptType.RESPONSE, selectionOutput.getPromptType(), "Should be a response type");

        logger.info("Choice selection test passed");
    }
}