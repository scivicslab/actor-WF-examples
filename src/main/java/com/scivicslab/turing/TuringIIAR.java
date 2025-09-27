/*
 * Copyright 2025 devteam@scivics-lab.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.scivicslab.turing;

import com.scivicslab.actorwf.ActionResult;
import com.scivicslab.actorwf.IIActorRef;
import com.scivicslab.actorwf.IIActorSystem;

import java.util.concurrent.ExecutionException;

/**
 * Interpreter-interfaced actor reference for Turing machine instances.
 *
 * <p>This class wraps a Turing machine object and allows it to be invoked
 * by the workflow interpreter using string-based action names.</p>
 *
 * @author devteam@scivics-lab.com
 */
public class TuringIIAR extends IIActorRef<Turing> {

    /**
     * Constructs a new TuringIIAR with the specified actor name and Turing instance.
     *
     * @param actorName the name of the actor
     * @param turing the Turing machine instance
     * @param system the actor system managing this actor
     */
    public TuringIIAR(String actorName, Turing turing, IIActorSystem system) {
        super(actorName, turing, system);
    }

    /**
     * Invokes a Turing machine action by name.
     *
     * <p>Supported actions:</p>
     * <ul>
     * <li>initMachine - Initialize the Turing machine</li>
     * <li>put - Write a value to the current tape position (requires argument)</li>
     * <li>move - Move the tape head (requires "L" or "R" argument)</li>
     * <li>printTape - Print the current tape contents</li>
     * <li>increment - Increment the iteration counter</li>
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
                case "initMachine":
                    this.tell(t -> t.initMachine()).get();
                    return new ActionResult(true, "Machine initialized");

                case "put":
                    this.tell(t -> t.put(args)).get();
                    return new ActionResult(true, "Put " + args);

                case "move":
                    this.tell(t -> t.move(args)).get();
                    return new ActionResult(true, "Moved " + args);

                case "printTape":
                    this.tell(t -> t.printTape()).get();
                    return new ActionResult(true, "Tape printed");

                case "increment":
                    int count = this.ask(t -> t.increment()).get();
                    return new ActionResult(true, "Counter: " + count);

                default:
                    return new ActionResult(false, "Unknown action: " + actionName);
            }
        } catch (InterruptedException | ExecutionException e) {
            return new ActionResult(false, "Error: " + e.getMessage());
        }
    }
}