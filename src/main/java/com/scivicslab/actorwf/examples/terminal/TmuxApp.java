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
package com.scivicslab.actorwf.examples.terminal;

import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.pojoactor.ActorSystem;

/**
 * Demonstration application that shows how to use TmuxSession and TmuxMonitor
 * with the POJO Actor system.
 *
 * <p>This application creates a tmux session, starts monitoring it, sends several
 * commands (ls, echo, pwd), and displays the captured output at each step.
 * Finally, it cleans up by stopping the monitor and killing the session.</p>
 *
 * <p>Example usage demonstrates:</p>
 * <ul>
 *   <li>Creating and managing a tmux session through actors</li>
 *   <li>Continuous monitoring of tmux output</li>
 *   <li>Sending commands and capturing results</li>
 *   <li>Detecting shell prompts</li>
 * </ul>
 */
public class TmuxApp {

    /**
     * Main entry point for the TmuxApp demonstration.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        ActorSystem system = new ActorSystem.Builder("tmux-system").build();

        String sessionName = "demo-session-" + System.currentTimeMillis();
        TmuxSession session = new TmuxSession(sessionName);

        ActorRef<TmuxSession> sessionRef = system.actorOf("session", session);
        ActorRef<TmuxMonitor> monitorRef = system.actorOf("monitor", new TmuxMonitor(session, 1000));

        try {
            System.out.println("Creating tmux session: " + sessionName);
            sessionRef.tell(s -> {
                try {
                    s.createSession();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            System.out.println("Starting monitor...");
            monitorRef.tell(m -> m.startMonitoring()).get();

            Thread.sleep(2000);

            System.out.println("\nSending command: ls -la");
            sessionRef.tell(s -> {
                try {
                    s.sendCommand("ls -la");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            Thread.sleep(2000);
            monitorRef.tell(m -> m.printLatestOutput()).get();

            System.out.println("\nSending command: echo 'Hello from tmux'");
            sessionRef.tell(s -> {
                try {
                    s.sendCommand("echo 'Hello from tmux'");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            Thread.sleep(2000);
            monitorRef.tell(m -> m.printLatestOutput()).get();

            System.out.println("\nSending command: pwd");
            sessionRef.tell(s -> {
                try {
                    s.sendCommand("pwd");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

            Thread.sleep(2000);
            monitorRef.tell(m -> m.printLatestOutput()).get();

            boolean hasPrompt = sessionRef.ask(s -> {
                try {
                    return s.hasPrompt();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();
            System.out.println("\nPrompt detected: " + hasPrompt);

            System.out.println("\nStopping monitor...");
            monitorRef.tell(m -> m.stopMonitoring()).get();

            System.out.println("Killing tmux session...");
            sessionRef.tell(s -> {
                try {
                    s.killSession();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }).get();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            system.terminate();
            System.out.println("\nActor system terminated");
        }
    }
}