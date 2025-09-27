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

/**
 * A Turing machine implementation that simulates the basic operations of a Turing machine.
 *
 * <p>This class provides functionality for reading, writing, and moving on an infinite tape,
 * along with position tracking and iteration counting. The Turing machine operates on a
 * tape that can expand dynamically as needed.</p>
 *
 * <p>Key features:</p>
 * <ul>
 * <li>Infinite tape support through dynamic resizing</li>
 * <li>Read and write operations at the current tape position</li>
 * <li>Left and right movement along the tape</li>
 * <li>Iteration counter with configurable maximum</li>
 * <li>Pattern matching capabilities for tape values</li>
 * </ul>
 */
public class Turing {

    /** The current position of the Turing machine head on the tape. */
    int currentPos = 0;

    /** The iteration counter for tracking machine steps. */
    int counter = 0;

    /** The infinite tape for the Turing machine. */
    Tape tape = new Tape();

    /** The maximum number of iterations allowed before stopping. */
    int maxIterations = 100;


    /**
     * Returns the current position of the machine head on the tape.
     * 
     * @return the current position
     */
    public int getCurrentPos() {
        return currentPos;
    }

    /**
     * Returns the value at the current position on the tape.
     *
     * @return the string value at the current tape position
     */
    public String getCurrentValue() {
        return this.tape.getWithResizing(currentPos);
    }

    /**
     * Returns the entire content of the tape as a string.
     * 
     * @return the tape content as a string representation
     */
    public String getTapeContent() {
        return this.tape.toString();
    }



    /**
     * Increments the iteration counter.
     *
     * @return the value of the iteration counter after incrementing
     */
    public int increment() {
        return ++this.counter;
    }


    /**
     * Initializes the Turing machine to its starting state.
     *
     * <p>This method resets the machine by:</p>
     * <ul>
     * <li>Setting the current position to 0</li>
     * <li>Resetting the iteration counter to 0</li>
     * <li>Creating a new empty tape</li>
     * </ul>
     */
    public void initMachine() {
        this.currentPos = 0;
        this.counter = 0;
        this.tape = new Tape();
    }


    /**
     * Checks if the current position contains either "0" or "1".
     * 
     * @return true if the current value is "0" or "1", false otherwise
     */
    public boolean isAny() {
        return this.getCurrentValue().equals("0") || this.getCurrentValue().equals("1");
    }



    /**
     * Checks if the value at the current position is NONE (space).
     *
     * @return true if the current position contains a space, false otherwise
     */
    public boolean isNone() {
        return this.getCurrentValue().equals(" ");
    }

    /**
     * Checks if the value of the current position on the tape matches a given value.
     *
     * @param value the value to compare against the current tape position
     * @return true if the current value equals the given value, false otherwise
     */
    public boolean matchCurrentValue(String value) {
        return this.getCurrentValue().equals(value);
    }


    /**
     * Checks if the value of the current position on the tape matches any value in a given set.
     *
     * @param values an array of values to test against
     * @return true if the current value equals one of the given values, false otherwise
     */
    public boolean matchCurrentValue(String[] values) {
        boolean result = false;
        String c = this.getCurrentValue();
        for (int i=0; i<values.length; i++) {
            if (values[i].equals(c)) {
                result = true;
                break;
            }
        }
        return result;
    }


    /**
     * Moves the current position on the tape one step left or right.
     *
     * @param direction a string representing the move direction: "L" for left or "R" for right (case-insensitive)
     */
    public void move(String direction) {
        if (direction.equalsIgnoreCase("L")) {
            this.currentPos --;
        }
        else if (direction.equalsIgnoreCase("R")) {
            this.currentPos ++;
        }
    }


    /**
     * Writes a value at the current position of the tape.
     *
     * @param value the string value to write on the tape
     */
    public void put(String value) {
        this.tape.setWithResizing(this.currentPos, value);
    }


    /**
     * Prints the current state of the tape to standard output.
     *
     * <p>The output format is: TAPE [counter] value [tape-content]</p>
     */
    public void printTape() {
        System.out.println(String.format("%s\t%d\t%s\t%s",
                                         "TAPE",
                                         this.counter,
                                         "value",
                                         this.tape.toString()
                                         ));

    }



    /**
     * Checks if the maximum number of iterations has been reached.
     *
     * @return true if the counter has reached or exceeded maxIterations, false otherwise
     */
    public boolean reachMaxIterations() {
        return this.counter >= this.maxIterations;
    }
    


    /**
     * Sets the current position of the Turing machine head on the tape.
     *
     * @param pos the new current position
     */
    public void setCurrentPos(int pos) {
        this.currentPos = pos;
    }
    
    
}
