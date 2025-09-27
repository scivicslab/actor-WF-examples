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

package com.scivicslab.actorwf.examples.turing;

import java.util.concurrent.ExecutionException;

import com.scivicslab.pojoactor.ActorRef;

/**
 * A demonstration application that implements a more complex Turing machine computation.
 *
 * <p>This application creates a Turing machine actor and executes a sophisticated algorithm
 * that manipulates the tape using multiple states and conditional logic. Unlike Turing123App,
 * this implementation includes pattern matching and conditional state transitions.</p>
 *
 * <p>The algorithm performs operations including:</p>
 * <ul>
 * <li>Initializing the tape with markers ('e') and initial values ('0')</li>
 * <li>Pattern matching on tape values ('0', '1', 'x', 'e', space)</li>
 * <li>Conditional state transitions based on current tape values</li>
 * <li>Complex tape manipulation including marking and unmarking positions</li>
 * <li>Periodic tape output display for debugging and visualization</li>
 * </ul>
 *
 * <p>This application demonstrates advanced Turing machine capabilities including
 * bi-directional tape movement and multi-symbol alphabets.</p>
 */
public class Turing134App {

    /**
     * Main entry point for the Turing134 application.
     *
     * @param args command line arguments (not used)
     */
    static public void main(String[] args) {
        Turing134App obj = new Turing134App();
        obj.calc();
    }



    /**
     * Executes the Turing machine calculation using actor-based messaging.
     *
     * <p>This method implements a complex Turing machine algorithm through asynchronous
     * message passing. The algorithm runs for up to 500 iterations and prints the tape
     * state at each iteration for visualization. The computation uses five states (0-4)
     * with conditional logic based on the current tape symbol.</p>
     *
     * <p>The method demonstrates:</p>
     * <ul>
     * <li>State-based control flow with multiple conditional branches</li>
     * <li>Pattern matching using ask/tell operations</li>
     * <li>Complex tape manipulation patterns</li>
     * <li>Iterative tape output for process visualization</li>
     * </ul>
     *
     * <p>The method uses the ActorRef pattern for thread-safe, asynchronous communication
     * with the Turing machine instance.</p>
     */
    public void calc() {

        ActorRef<Turing> turingActor = new ActorRef<Turing>("p134", new Turing());

        int state = 0;
        try {
            turingActor.tell((a)->{a.initMachine();}).get();

            for (int i = 0; i<500; i++) {

                turingActor.tell((a)->a.printTape()).get();
                
                if (state == 0) {
                    turingActor.tell((a)->a.put("e")).get();
                    turingActor.tell((a)->a.move("R")).get();
                    turingActor.tell((a)->a.put("e")).get();
                    turingActor.tell((a)->a.move("R")).get();
                    turingActor.tell((a)->a.put("0")).get();
                    turingActor.tell((a)->a.move("R")).get();
                    turingActor.tell((a)->a.move("R")).get();
                    turingActor.tell((a)->a.put("0")).get();
                    turingActor.tell((a)->a.move("L")).get();
                    turingActor.tell((a)->a.move("L")).get();
                    state = 1;
                    continue;
                }
                
                if (state == 1) {

                    if (turingActor.ask((a)->a.matchCurrentValue("1")).get()) {
                        turingActor.tell((a)->a.move("R")).get();
                        turingActor.tell((a)->a.put("x")).get();
                        turingActor.tell((a)->a.move("L")).get();
                        turingActor.tell((a)->a.move("L")).get();
                        turingActor.tell((a)->a.move("L")).get();
                        state = 1;
                        continue;
                    }
                    else if (turingActor.ask((a)->a.matchCurrentValue("0")).get()) {
                        state = 2;
                        continue;
                    }
                }

                if (state == 2) {

                    if (turingActor.ask((a)->a.isAny()).get()) { // isAny method matches "0" or "1"
                        turingActor.tell((a)->a.move("R")).get();
                        turingActor.tell((a)->a.move("R")).get();
                        state = 2;
                        continue;
                    }
                    
                    else if (turingActor.ask((a)->a.isNone()).get()) { // isNone method matches " "
                        turingActor.tell((a)->a.put("1")).get();
                        turingActor.tell((a)->a.move("L")).get();
                        state = 3;
                        continue;                    
                    }
                    
                }

                if (state == 3) {
                    if (turingActor.ask((a)->a.matchCurrentValue("x")).get()) {
                        turingActor.tell((a)->a.put(" ")).get();
                        turingActor.tell((a)->a.move("R")).get();
                        state = 2;
                        continue;
                    }
                    else if (turingActor.ask((a)->a.matchCurrentValue("e")).get()) {
                        turingActor.tell((a)->a.move("R")).get();
                        state = 4;
                        continue;
                    }
                    else if (turingActor.ask((a)->a.isNone()).get()) {
                        turingActor.tell((a)->a.move("L")).get();
                        turingActor.tell((a)->a.move("L")).get();
                        state = 3;
                        continue;
                    }

                }

                if (state == 4) {

                    if (turingActor.ask((a)->a.isNone()).get()) {
                        turingActor.tell((a)->a.put("0")).get();
                        turingActor.tell((a)->a.move("L")).get();
                        turingActor.tell((a)->a.move("L")).get();
                        state = 1;
                        continue;                    
                    }
                    else {
                        turingActor.tell((a)->a.move("R")).get();
                        turingActor.tell((a)->a.move("R")).get();
                        state = 4;
                        continue;
                    }
                                        
                }

            }

            turingActor.tell((a)->a.printTape()).get();                
                
        } catch (InterruptedException | ExecutionException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally {
            turingActor.close();
        }
        
    }
    
}
