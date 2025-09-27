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
 * A demonstration application that implements a specific Turing machine computation.
 *
 * <p>This application creates a Turing machine actor and executes a defined algorithm
 * that writes a pattern of 0s and 1s to the tape. The program implements a state machine
 * with four states (0-3) that control the tape operations.</p>
 *
 * <p>The algorithm performs the following pattern:</p>
 * <ul>
 * <li>State 0: Write '0', move right, transition to state 1</li>
 * <li>State 1: Move right, transition to state 2</li>
 * <li>State 2: Write '1', move right, transition to state 3</li>
 * <li>State 3: Move right, transition to state 0</li>
 * </ul>
 *
 * <p>This creates an alternating pattern with spacing on the tape.</p>
 */
public class Turing123App {


    /**
     * Main entry point for the Turing123 application.
     *
     * @param args command line arguments (not used)
     */
    static public void main(String[] args) {
        Turing123App obj = new Turing123App();
        obj.calc();
    }

    /**
     * Executes the Turing machine calculation using actor-based messaging.
     *
     * <p>This method implements a specific Turing machine algorithm through asynchronous
     * message passing. The algorithm runs for up to 50 iterations, cycling through four
     * states to write a pattern on the tape. After completion, the final tape content
     * is printed.</p>
     *
     * <p>The method uses the ActorRef pattern for thread-safe, asynchronous communication
     * with the Turing machine instance. All operations complete before proceeding to ensure
     * correct execution order.</p>
     */
    public void calc() {

        ActorRef<Turing> turingActor = new ActorRef<Turing>("p123", new Turing());

        int state = 0;
        try {
            turingActor.tell((a)->{a.initMachine();}).get();

            for (int i = 0; i<50; i++) {
                
                if (state == 0) {
                    turingActor.tell((a)->a.put("0")).get();
                    turingActor.tell((a)->a.move("R")).get();
                    state = 1;
                    continue;
                }
                
                if (state == 1) {
                    turingActor.tell((a)->a.move("R")).get();
                    state = 2;
                    continue;
                }

                if (state == 2) {
                    turingActor.tell((a)->a.put("1")).get();
                    turingActor.tell((a)->a.move("R")).get();
                    state = 3;
                    continue;                    
                }

                if (state == 3) {
                    turingActor.tell((a)->a.move("R")).get();
                    state = 0;
                    continue;                    
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
