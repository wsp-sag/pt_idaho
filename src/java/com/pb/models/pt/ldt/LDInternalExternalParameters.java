/*
 * Copyright  2006 PB Consult Inc.
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
 */
package com.pb.models.pt.ldt;

/**
 * A class that contains parameters for the Internal-External model
 * for long distance tours.  
 * 
 * @author Erhardt
 * @version 1.0 Apr 4, 2006
 *
 */
public class LDInternalExternalParameters {

    // all parameters are applied to the external alternative
    public static final int CONSTANT      = 0;
    public static final int COMPLETETOUR  = 1;
    public static final int INCOME60P     = 2; 
    public static final int WORKER        = 3;
    public static final int AGELT25       = 4;
    public static final int AGE5564       = 5;
    public static final int AGE65P        = 6;
    public static final int OCCCONSTRUCT  = 7; 
    public static final int OCCFININSREAL = 8;
    public static final int OCCPUBADMIN   = 9; 
    public static final int OCCEDUCATION  = 10; 
    public static final int OCCMEDICAL    = 11;
    public static final int TIMETOEXTSTA  = 12; 
}
