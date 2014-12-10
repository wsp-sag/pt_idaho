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
 *   Created on Feb 22, 2006 by Andrew Stryker <stryker@pbworld.com>
 */
package com.pb.models.pt;

/**
 * Tour Scheduling Model parameters.
 * 
 * @author Stryker
 * 
 */
public class TourSchedulingParameters {

    // Be sure these are updated!
    public static final int ALTERNATIVES = 19;

    public static final int DURATIONS = 19;

    // PARAMETERS includes ASCs - used in testing
    public static final int PARAMETERS = ALTERNATIVES + 15;
    
    public static final int DEPARTURE_PARAMETERS=93;

    public static final int PURPOSE = 0;

    public static final int CDEPART5 = 1;

    public static final int CDEPART6 = 2;

    public static final int CDEPART7 = 3;

    public static final int CDEPART8 = 4;

    public static final int CDEPART9 = 5;

    public static final int CDEPART10 = 6;

    public static final int CDEPART11 = 7;

    public static final int CDEPART12 = 8;

    public static final int CDEPART13 = 9;

    public static final int CDEPART14 = 10;

    public static final int CDEPART15 = 11;

    public static final int CDEPART16 = 12;

    public static final int CDEPART17 = 13;

    public static final int CDEPART18 = 14;

    public static final int CDEPART19 = 15;

    public static final int CDEPART20 = 16;

    public static final int CDEPART21 = 17;

    public static final int CDEPART22 = 18;

    public static final int CDEPART23 = 19;

    public static final int CDURAT0 = 20;

    public static final int CDURAT1 = 21;

    public static final int CDURAT2 = 22;

    public static final int CDURAT3 = 23;

    public static final int CDURAT4 = 24;

    public static final int CDURAT5 = 25;

    public static final int CDURAT6 = 26;

    public static final int CDURAT7 = 27;

    public static final int CDURAT8 = 28;

    public static final int CDURAT9 = 29;

    public static final int CDURAT10 = 30;

    public static final int CDURAT11 = 31;

    public static final int CDURAT12 = 32;

    public static final int CDURAT13 = 33;

    public static final int CDURAT14 = 34;

    public static final int CDURAT15 = 35;

    public static final int CDURAT16 = 36;

    public static final int CDURAT17 = 37;

    public static final int CDURAT18 = 38;

    public static final int CSCH2PDEP = 39;

    public static final int CWRK2PDEP = 40;

    public static final int CSHP2PDEP = 41;

    public static final int CREC2PDEP = 42;

    public static final int CSCHTRSDEP = 43;

    public static final int CWRKTRDEP = 44;

    public static final int CWRKBDEP = 45;

    public static final int CSHPTRDEP = 46;

    public static final int CRECTRDEP = 47;

    public static final int COTHTRDDEP = 48;

    public static final int CSTPSIDEP = 49;

    public static final int CSTPSODEP = 50;

    public static final int CSTPSBDEP = 51;

    public static final int CFOF2DEP = 52;

    public static final int CSOF2DEP = 53;

    public static final int CFOF3DEP = 54;

    public static final int CSOF3DEP = 55;

    public static final int CTOF3DEP = 56;

    public static final int CFOF4DEP = 57;

    public static final int CSOF4DEP = 58;

    public static final int CTOF4DEP = 59;

    public static final int CVOF4DEP = 60;

    public static final int CPSPTDEP = 61;

    public static final int CGHSSTDDEP = 62;

    public static final int CWRKADDEP = 63;

    public static final int CCOLSTDDEP = 64;

    public static final int CFEMDEP = 65;

    public static final int CAGE25DEP = 66;

    public static final int CAGE21DEP = 67;

    public static final int CAGE22DEP = 68;

    public static final int CAGE55DEP = 69;

    public static final int CNW25DEP = 70;

    public static final int CNW55DEP = 71;

    public static final int CWRK25DEP = 72;

    public static final int CWRKMDDEP = 73;

    public static final int CWRK55DEP = 74;

    public static final int CRETLDEP = 75;

    public static final int CGOVDEP = 76;

    public static final int CEDCDEP = 77;

    public static final int CENTDEP = 78;

    public static final int CHOTDEP = 79;

    public static final int CESTDEP = 80;

    public static final int CGRP46DEP = 81;

    public static final int CUNK4DEP = 82;

    public static final int CUNK3DEP = 83;

    public static final int CGRP11DEP = 84;

    public static final int CHIINCDEP = 85;

    public static final int CAUTO0DEP = 86;

    public static final int CHHCLASS2DEP = 87;

    public static final int CHHCLASS3DEP = 88;

    public static final int CHHCLASS4DEP = 89;

    public static final int CHHCLASS5DEP = 90;

    public static final int CHHCLASS6DEP = 91;

    public static final int CHHCLASS7DEP = 92;

    public static final int CHHCLASS8DEP = 93;

    public static final int CHHCLASS9DEP = 94;

    public static final int CHHCLASS10DEP = 95;

    public static final int CHHCLASS11DEP = 96;

    public static final int CHHCLASS12DEP = 97;

    public static final int CHHCLASS13DEP = 98;

    public static final int CHHCLASS14DEP = 99;

    public static final int CHHCLASS15DEP = 100;

    public static final int CADLT2DEP = 101;

    public static final int CNWADLTDEP = 102;

    public static final int CCH5DEP = 103;

    public static final int CCH615DEP = 104;

    public static final int CHIPSDEP = 105;

    public static final int CHIGHSDEP = 106;

    public static final int CHICOLDEP = 107;

    public static final int CA0COLDEP = 108;

    public static final int CAIPSDEP = 109;

    public static final int CAIGHSDEP = 110;

    public static final int CPSHH6DEP = 111;

    public static final int CPSHH9DEP = 112;

    public static final int CGHSHH7DEP = 113;

    public static final int CGHSHH8DEP = 114;

    public static final int CGHSHH9DEP = 115;

    public static final int CGHSHH10DEP = 116;

    public static final int CGHSHH11DEP = 117;

    public static final int CGHSHH12DEP = 118;

    public static final int CGHSHH13DEP = 119;

    public static final int CGHSHH14DEP = 120;

    public static final int CGHSHH15DEP = 121;

    public static final int CCOLHH4DEP = 122;

    public static final int CCOLHH5DEP = 123;

    public static final int CCOLHH6DEP = 124;

    public static final int CCOLHH7DEP = 125;

    public static final int CCOLHH8DEP = 126;

    public static final int CCOLHH9DEP = 127;

    public static final int CCOLHH10DEP = 128;

    public static final int CCOLHH11DEP = 129;

    public static final int CCOLHH12DEP = 130;

    public static final int CXYDSTDEP = 131;

    public static final int CSCH2PDUR = 132;

    public static final int CWRK2PDUR = 133;

    public static final int CSHP2PDUR = 134;

    public static final int CREC2PDUR = 135;

    public static final int CSCHTRSDUR = 136;

    public static final int CWRKTRDUR = 137;

    public static final int CWRKBDUR = 138;

    public static final int CSHPTRDUR = 139;

    public static final int CRECTRDUR = 140;

    public static final int COTHTRDDUR = 141;

    public static final int CSTPSIDUR = 142;

    public static final int CSTPSODUR = 143;

    public static final int CSTPSBDUR = 144;

    public static final int CFOF2DUR = 145;

    public static final int CSOF2DUR = 146;

    public static final int CFOF3DUR = 147;

    public static final int CSOF3DUR = 148;

    public static final int CTOF3DUR = 149;

    public static final int CFOF4DUR = 150;

    public static final int CSOF4DUR = 151;

    public static final int CTOF4DUR = 152;

    public static final int CVOF4DUR = 153;

    public static final int CPSPTDUR = 154;

    public static final int CGHSSTDDUR = 155;

    public static final int CWRKADDUR = 156;

    public static final int CCOLSTDDUR = 157;

    public static final int CFEMDUR = 158;

    public static final int CAGE25DUR = 159;

    public static final int CAGE21DUR = 160;

    public static final int CAGE22DUR = 161;

    public static final int CAGE55DUR = 162;

    public static final int CNW25DUR = 163;

    public static final int CNW55DUR = 164;

    public static final int CWRK25DUR = 165;

    public static final int CWRKMDDUR = 166;

    public static final int CWRK55DUR = 167;

    public static final int CRETLDUR = 168;

    public static final int CGOVDUR = 169;

    public static final int CEDCDUR = 170;

    public static final int CENTDUR = 171;

    public static final int CHOTDUR = 172;

    public static final int CESTDUR = 173;

    public static final int CGRP46DUR = 174;

    public static final int CUNK4DUR = 175;

    public static final int CUNK3DUR = 176;

    public static final int CGRP11DUR = 177;

    public static final int CHIINCDUR = 178;

    public static final int CAUTO0DUR = 179;

    public static final int CHHCLASS2DUR = 180;

    public static final int CHHCLASS3DUR = 181;

    public static final int CHHCLASS4DUR = 182;

    public static final int CHHCLASS5DUR = 183;

    public static final int CHHCLASS6DUR = 184;

    public static final int CHHCLASS7DUR = 185;

    public static final int CHHCLASS8DUR = 186;

    public static final int CHHCLASS9DUR = 187;

    public static final int CHHCLASS10DUR = 188;

    public static final int CHHCLASS11DUR = 189;

    public static final int CHHCLASS12DUR = 190;

    public static final int CHHCLASS13DUR = 191;

    public static final int CHHCLASS14DUR = 192;

    public static final int CHHCLASS15DUR = 193;

    public static final int CADLT2DUR = 194;

    public static final int CNWADLTDUR = 195;

    public static final int CCH5DUR = 196;

    public static final int CCH615DUR = 197;

    public static final int CHIPSDUR = 198;

    public static final int CHIGHSDUR = 199;

    public static final int CHICOLDUR = 200;

    public static final int CA0COLDUR = 201;

    public static final int CAIPSDUR = 202;

    public static final int CAIGHSDUR = 203;

    public static final int CPSHH6DUR = 204;

    public static final int CPSHH9DUR = 205;

    public static final int CGHSHH7DUR = 206;

    public static final int CGHSHH8DUR = 207;

    public static final int CGHSHH9DUR = 208;

    public static final int CGHSHH10DUR = 209;

    public static final int CGHSHH11DUR = 210;

    public static final int CGHSHH12DUR = 211;

    public static final int CGHSHH13DUR = 212;

    public static final int CGHSHH14DUR = 213;

    public static final int CGHSHH15DUR = 214;

    public static final int CCOLHH4DUR = 215;

    public static final int CCOLHH5DUR = 216;

    public static final int CCOLHH6DUR = 217;

    public static final int CCOLHH7DUR = 218;

    public static final int CCOLHH8DUR = 219;

    public static final int CCOLHH9DUR = 220;

    public static final int CCOLHH10DUR = 221;

    public static final int CCOLHH11DUR = 222;

    public static final int CCOLHH12DUR = 223;

    public static final int CXYDSTDUR = 224;

}
