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
package com.scivicslab.pojoactor.workflow.examples.terminal;

import com.scivicslab.pojoactor.ActionResult;
import com.scivicslab.pojoactor.workflow.IIActorRef;
import com.scivicslab.pojoactor.workflow.IIActorSystem;

/**
 * Actor reference wrapper for TmuxMonitor that enables workflow-based control.
 * This class extends IIActorRef to provide action-based interaction with a TmuxMonitor
 * instance through named actions.
 *
 * <p>Supported actions include:</p>
 * <ul>
 *   <li>startMonitoring - starts the monitoring process</li>
 *   <li>stopMonitoring - stops the monitoring process</li>
 *   <li>printLatestOutput - prints the most recent captured output</li>
 *   <li>checkMonitoring - checks the current monitoring status</li>
 * </ul>
 */
public class TmuxMonitorIIAR extends IIActorRef<TmuxMonitor> {

    /**
     * Creates a new TmuxMonitorIIAR with the specified actor name and monitor instance.
     *
     * @param actorName the name to assign to this actor
     * @param monitor the TmuxMonitor instance to wrap
     */
    public TmuxMonitorIIAR(String actorName, TmuxMonitor monitor) {
        super(actorName, monitor);
    }

    /**
     * Executes an action on the TmuxMonitor by action name.
     * Provides workflow integration by mapping action names to TmuxMonitor methods.
     *
     * @param actionName the name of the action to execute (startMonitoring, stopMonitoring,
     *                   printLatestOutput, checkMonitoring)
     * @param args additional arguments for the action (not used by most actions)
     * @return an ActionResult indicating success or failure with a descriptive message
     */
    @Override
    public ActionResult callByActionName(String actionName, String args) {
        try {
            switch (actionName) {
                case "startMonitoring":
                    this.tell(m -> m.startMonitoring()).get();
                    return new ActionResult(true, "Monitoring started");

                case "stopMonitoring":
                    this.tell(m -> m.stopMonitoring()).get();
                    return new ActionResult(true, "Monitoring stopped");

                case "printLatestOutput":
                    this.tell(m -> m.printLatestOutput()).get();
                    return new ActionResult(true, "Latest output printed");

                case "checkMonitoring":
                    boolean isMonitoring = this.ask(m -> m.isMonitoring()).get();
                    return new ActionResult(true, "Monitoring: " + isMonitoring);

                default:
                    return new ActionResult(false, "Unknown action: " + actionName);
            }
        } catch (Exception e) {
            return new ActionResult(false, "Error: " + e.getMessage());
        }
    }
}