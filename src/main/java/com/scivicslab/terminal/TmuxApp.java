package com.scivicslab.terminal;

import com.scivicslab.pojoactor.ActorRef;
import com.scivicslab.pojoactor.ActorSystem;

public class TmuxApp {

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