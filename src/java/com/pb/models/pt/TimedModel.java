/*
 * Copyright 2006 PB Consult Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * Created on Aug 9, 2006 by Andrew Stryker <stryker@pbworld.com>
 */
package com.pb.models.pt;

public abstract class TimedModel {

    private long elapsedTime = 0;

    private long startTime = 0;
    
    private int nestingLevel = 0;
    
    /**
     * Return the elapsed time in milliseconds.
     * @return long elapsed time
     */
    public long getElapsedTime() {
        return elapsedTime;
    }
    
    /**
     * Start timing.
     */
    protected void startTiming() {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        
        nestingLevel += 1;
    }

    /**
     * End timing nest and update elapsed time out of nests..
     *
     */
    protected void endTiming() {
        nestingLevel -= 1;
        
        if (nestingLevel == 0) {
            elapsedTime += System.currentTimeMillis() - startTime;
            startTime = 0;
        }
    }
}
