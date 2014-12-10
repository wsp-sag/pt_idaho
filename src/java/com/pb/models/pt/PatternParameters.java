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
 *   Created on Feb 17, 2006 by Joel Freedman <freedman@pbworld.com>
 */
package com.pb.models.pt;

/**
 * A class that provides an index into a table of Pattern Model parameters.
 * @author Freedman
 *
 */
public class PatternParameters {

    public static final int CTRS1             = 0  ; // Number of tours is 1                                                                               
    public static final int CTRS2             = 1  ; // Number of tours is 2                                                                               
    public static final int CTRS3             = 2  ; // Number of tours is 3                                                                               
    public static final int CTRS4             = 3  ; // Number of tours is 4                                                                               
    public static final int CTRS5             = 4  ; // Number of tours is 5 or more                                                                       
    public static final int CWRKNSTPS         = 5  ; // One work/work-based tour only, no stops                                                            
    public static final int CWRKNSTP2         = 6  ; // One work/work-based tour, no stops                                                                 
    public static final int CWRKOSTPS         = 7  ; // One work/work-based tour only, with outbound stop only                                             
    public static final int CWRKISTPS         = 8  ; // One work/work-based tour only, with inbound stop only                                              
    public static final int CWRKOSTP2         = 9  ; // One work/work-based tour, with outbound stop only                                                  
    public static final int CWRKISTP2         = 10 ; // One work/work-based tour, with inbound stop only                                                   
    public static final int CSCH              = 11 ; // School tour only, with no work stops                                                               
    public static final int CWRK              = 12 ; // Work/work-based tour only                                                                          
    public static final int CSCHWRK           = 13 ; // School tour, then work/work-based tour, with no work stops                                         
    public static final int CWRKSCH           = 14 ; // Work/work-based tour, then school tour, with no work stops                                         
    public static final int CCOMBO            = 15 ; // School and work on same tour                                                                       
    public static final int CWRKPNSTP2        = 16 ; // Two or more work/work-based tours, no stops                                                        
    public static final int CWRKPWSTP2        = 17 ; // Two or more work/work-based tours, with stops                                                      
    public static final int CACTCG1D          = 18 ; // More than 1 school tour dummy                                                                      
    public static final int CBACT             = 19 ; // Number of work-based tours                                                                         
    public static final int CBACT1            = 20 ; // Presence of work based tours                                                                       
    public static final int CHSCHH            = 21 ; // Pattern is home-school-home                                                                        
    public static final int CT1OTH            = 22 ; // if only 1 tour--other                                                                              
    public static final int CT2SHP            = 23 ; // if multiple tours--shop only                                                                       
    public static final int CT2OTH            = 24 ; // if multiple tours--other only                                                                      
    public static final int CP0SEC3B          = 25 ; // if no primary tour, there are 3+ secondary tours                                                   
    public static final int CACTSD            = 26 ; // Presence of shop tours dummy                                                                       
    public static final int CACTRD            = 27 ; // Presence of recreation tours dummy                                                                 
    public static final int CACTOD            = 28 ; // Presence of other tours dummy                                                                      
    public static final int CSHPWRK           = 29 ; // Shop before work dummy                                                                             
    public static final int CRECWRK           = 30 ; // Recreation before work dummy                                                                       
    public static final int COTHWRK           = 31 ; // Other before work dummy                                                                            
    public static final int CSHPSCH           = 32 ; // Shop tours present before school                                                                   
    public static final int CRECSCH           = 33 ; // Recreatio tours present before school                                                              
    public static final int COTHSCH           = 34 ; // Other tours present, before school                                                                 
    public static final int CRECSHP           = 35 ; // Recreation tours occur prior to shop tours                                                         
    public static final int CSHPACT1          = 36 ; // One shop activity                                                                                  
    public static final int CSHPACT2          = 37 ; // Two shop activities                                                                                
    public static final int CSHPACT3          = 38 ; // Three shop activities                                                                              
    public static final int CSHPACT4          = 39 ; // Four or more shop activities                                                                       
    public static final int CRECACT1          = 40 ; // One recreation activity                                                                            
    public static final int CRECACT2          = 41 ; // Two recreation activities                                                                          
    public static final int CRECACT3          = 42 ; // Three recreation activities                                                                        
    public static final int CRECACT4          = 43 ; // Four or more recreation activities                                                                 
    public static final int COTHACT1          = 44 ; // One other activity                                                                                 
    public static final int COTHACT2          = 45 ; // Two other activities                                                                               
    public static final int COTHACT3          = 46 ; // Three other activities                                                                             
    public static final int COTHACT4          = 47 ; // Four or more other activities                                                                      
    public static final int CWNWINT1          = 48 ; // Number of stops on work tours * Number of non-work tours                                           
    public static final int CWNWINT2          = 49 ; // Presence of stops on work tours * Number of stops on non-work tours                                
    public static final int CTSINT1           = 50 ; // Number of intermediate stops * number of tours                                                     
    public static final int CSCOUT            = 51 ; // dummy if there is outbound without inboud in school tour                                           
    public static final int CSCIN             = 52 ; // dummy if there is inbound without outbound  in school tour                                         
    public static final int CSCINOUT          = 53 ; // dummy if there is both inbound-outbound in school tour                                             
    public static final int CWRKOUT           = 54 ; // dummy if there is outbound without inboud in work tour                                             
    public static final int CWRKIN            = 55 ; // dummy if there is inbound without outbound  in work tour                                           
    public static final int CWRKINOUT         = 56 ; // dummy if there is both inbound-outbound in work tour                                               
    public static final int CP1_O             = 57 ; // if work or school, there are outbound stops on it                                                  
    public static final int CP1_I             = 58 ; // if work or school, there are inbound stops on it                                                   
    public static final int CP1_IO            = 59 ; // if work or school, there are inbound & oubound stops on it                                         
    public static final int CP2_O1            = 60 ; // if school & work tours, there are outbound stops on the first                                      
    public static final int CP2_I1            = 61 ; // if school & work tours, there are inbound stops on the first                                       
    public static final int CP2_IO1           = 62 ; // if school & work tours, there are oubound & inbounds stops on the first                            
    public static final int CP2_O2            = 63 ; // if school & work tours, there are outbound stops on the second                                     
    public static final int CP2_I2            = 64 ; // if school & work tours, there are inbound stops on the second                                      
    public static final int CP2_IO2           = 65 ; // if school & work tours, there are oubound & inbounds stops on the second                           
    public static final int CCOMBO_IO         = 66 ; // if school and work on same tour, there are additional stops on it                                  
    public static final int COUTGTINNS        = 67 ; // Outbound stops > inbound stops                                                                     
    public static final int COUTLTINNS        = 68 ; // Outbound stops < inbound stops                                                                     
    public static final int COUTEQINNS        = 69 ; // Outbound stops = inbound stops & > 0                                                               
    public static final int CSTPBD            = 70 ; // Presence of stops on work-based tours                                                              
    public static final int CSTOPSSD          = 71 ; // Presence of stops on shop tours                                                                    
    public static final int CSTOPSRD          = 72 ; // Presence of stops on recreation tours                                                              
    public static final int CSTOPSO1          = 73 ; // One stop on other tours                                                                            
    public static final int CSTOPSO2          = 74 ; // Two stops on other tours                                                                           
    public static final int CSTOPSO3          = 75 ; // Three or more stops on other tours                                                                 
    public static final int C2TA0             = 76 ; // Two or more tours dummy if autos = 0                                                               
    public static final int CTWOALTA          = 77 ; // Two or more tours dummy if 0 < autos < adults                                                      
    public static final int CSTPNWA0          = 78 ; // Presence of stops on tours if autos = 0                                                            
    public static final int CSTOPALTA         = 79 ; // Presence of stops on tours if 0 < autos < adults                                                   
    public static final int CSTPWA0           = 80 ; // Presence of stops on work tours if autos = 0                                                       
    public static final int CSTPBA0           = 81 ; // Presence of stops on work based tours if autos = 0                                                 
    public static final int CSTPWBA0          = 82 ; // Presence of stops on work / work based tours if autos = 0                                          
    public static final int CSTPWBAI          = 83 ; // Presence of stops on work / work based tours if 0 < autos < workers                                
    public static final int CSTOPNWA0         = 84 ; // Presence of stops on shop, rec & other tours if autos = 0                                          
    public static final int CSRPTOURZA        = 85 ; // Presence of shop, rec & other tours if autos = 0                                                   
    public static final int CSROSTPC          = 86 ; // Presence of stops for shop, rec & other if cars < workers (cars > 0)                               
    public static final int CSROSTNWA         = 87 ; // Presence of stops for shop, rec or other if non-working adult in HH                                
    public static final int CSROSTPAD         = 88 ; // Presence of stops for shop, rec & other if only 1 adult in HH                                      
    public static final int CSTPPRIMHH7       = 89 ; // Presence of stops on work or school  if two+ adults, workers=adults, no children                   
    public static final int CSTPPRIMHH8       = 90 ; // Presence of stops on work or school  if two+ adults, workers=adults, 1+ children, no preschooler   
    public static final int CSTPPRIMHH9       = 91 ; // Presence of stops on work or school  if two+ adults, workers=adults, 1+ children, 1+ preschooler   
    public static final int CSTPPRIMHH10      = 92 ; // Presence of stops on work or school  if two+ adults, workers<adults, no children                   
    public static final int CSTPPRIMHH11      = 93 ; // Presence of stops on work or school  if two+ adults, workers<adults, 1+ children, no preschooler   
    public static final int CSTPPRIMHH12      = 94 ; // Presence of stops on work or school  if two+ adults, workers<adults, 1+ children, 1+ preschooler   
    public static final int CSTPPRIMHH13      = 95 ; // Presence of stops on work or school  if two+ adults, no workers, no children                       
    public static final int CSTPPRIMHH14      = 96 ; // Presence of stops on work or school  if two+ adults, no workers, 1+ children, no preschooler       
    public static final int CSTPPRIMHH15      = 97 ; // Presence of stops on work or school  if two+ adults, no workers, 1+ children, 1+ preschooler       
    public static final int CSTPNWHH1         = 98 ; // Presence of stops on shop, rec or other if one adult, non worker, no children                      
    public static final int CSTPNWHH2         = 99 ; // Presence of stops on shop, rec or other if one adult, non worker, 1+ children, no preschooler      
    public static final int CSTPNWHH3         = 100; // Presence of stops on shop, rec or other if one adult, non worker, 1+ children, 1+ preschooler      
    public static final int CSTPNWHH4         = 101; // Presence of stops on shop, rec or other if one adult, worker, no children                          
    public static final int CSTPNWHH5         = 102; // Presence of stops on shop, rec or other if one adult, worker, 1+ children, no preschooler          
    public static final int CSTPNWHH6         = 103; // Presence of stops on shop, rec or other if one adult, worker, 1+ children, 1+ preschooler          
    public static final int CSTPNWHH7         = 104; // Presence of stops on shop, rec or other if two+ adults, workers=adults, no children                
    public static final int CSTPNWHH8         = 105; // Presence of stops on shop, rec or other if two+ adults, workers=adults, 1+ children, no preschooler
    public static final int CSTPNWHH9         = 106; // Presence of stops on shop, rec or other if two+ adults, workers=adults, 1+ children, 1+ preschooler
    public static final int CSTPNWHH10        = 107; // Presence of stops on shop, rec or other if two+ adults, workers<adults, no children                
    public static final int CSTPNWHH11        = 108; // Presence of stops on shop, rec or other if two+ adults, workers<adults, 1+ children, no preschooler
    public static final int CSTPNWHH12        = 109; // Presence of stops on shop, rec or other if two+ adults, workers<adults, 1+ children, 1+ preschooler
    public static final int CSTPNWHH13        = 110; // Presence of stops on shop, rec or other if two+ adults, no workers, no children                    
    public static final int CSTPNWHH14        = 111; // Presence of stops on shop, rec or other if two+ adults, no workers, 1+ children, no preschooler    
    public static final int CSTPNWHH15        = 112; // Presence of stops on shop, rec or other if two+ adults, no workers, 1+ children, 1+ preschooler    
    public static final int CSCH_W            = 113; // School tour only, with no work stops, if worker                                                    
    public static final int CSCHAGE1          = 114; // School tour present if age <=1                                                                     
    public static final int CSCHAGE2          = 115; // School tour present if age = 2                                                                     
    public static final int CSCHAGE3          = 116; // School tour present if age = 3                                                                     
    public static final int CSCHAGE4          = 117; // School tour present if age = 4                                                                     
    public static final int CSCHAGE5          = 118; // School tour present if age = 5                                                                     
    public static final int CWRKAGE3          = 119; // Work activity for 15 years old                                                                     
    public static final int CWRKAGE4          = 120; // Work activity for 16 years old                                                                     
    public static final int CWRKAGE5          = 121; // Work activity for 17 years old                                                                     
    public static final int CSCHHINC          = 122; // School tour present if high income                                                                 
    public static final int CSCHNWA           = 123; // School tour present if non working adult in HH                                                     
    public static final int CSTPSWFM          = 124; // Shop stops on work tours if female                                                                 
    public static final int CSHPAFEM          = 125; // Presence of shop tours or stops if female                                                          
    public static final int CSHPTT4C          = 126; // Shop if age 16-17 & cars > adults                                                                  
    public static final int CSHPAAGE1         = 127; // Shop if age less than 25 yrs old                                                                   
    public static final int CSHOPAGE2         = 128; // Shop if age 25-35                                                                                  
    public static final int CSHOPAGE3         = 129; // Shop if age 35-45                                                                                  
    public static final int CSHOPAGE4         = 130; // Shop if age 45-55 yrs                                                                              
    public static final int CSHOPAGE5         = 131; // Shop if age 55-65 yrs                                                                              
    public static final int CSHOPAGE6         = 132; // Shop if age 65+ yrs                                                                                
    public static final int CSHOPLINC         = 133; // Shop if low income                                                                                 
    public static final int CSHPAHINC         = 134; // Presence of shop tours or stops if high income                                                     
    public static final int CSHPNWA           = 135; // Shop if non-working adult in HH                                                                    
    public static final int CSHP1AD           = 136; // Shop if there is only 1 adult in the HH                                                            
    public static final int CSHPHH1           = 137; // Shop if one adult, non worker, no children                                                         
    public static final int CSHPHH2           = 138; // Shop if one adult, non worker, 1+ children, no preschooler                                         
    public static final int CSHPHH3           = 139; // Shop if one adult, non worker, 1+ children, 1+ preschooler                                         
    public static final int CSHPHH4           = 140; // Shop if one adult, worker, no children                                                             
    public static final int CSHPHH5           = 141; // Shop if one adult, worker, 1+ children, no preschooler                                             
    public static final int CSHPHH6           = 142; // Shop if one adult, worker, 1+ children, 1+ preschooler                                             
    public static final int CSHPHH7           = 143; // Shop if two+ adults, workers=adults, no children                                                   
    public static final int CSHPHH8           = 144; // Shop if two+ adults, workers=adults, 1+ children, no preschooler                                   
    public static final int CSHPHH9           = 145; // Shop if two+ adults, workers=adults, 1+ children, 1+ preschooler                                   
    public static final int CSHPHH10          = 146; // Shop if two+ adults, workers<adults, no children                                                   
    public static final int CSHPHH11          = 147; // Shop if two+ adults, workers<adults, 1+ children, no preschooler                                   
    public static final int CSHPHH12          = 148; // Shop if two+ adults, workers<adults, 1+ children, 1+ preschooler                                   
    public static final int CSHPHH13          = 149; // Shop if two+ adults, no workers, no children                                                       
    public static final int CSHPHH14          = 150; // Shop if two+ adults, no workers, 1+ children, no preschooler                                       
    public static final int CSHPHH15          = 151; // Shop if two+ adults, no workers, 1+ children, 1+ preschooler                                       
    public static final int CRECAGE4          = 152; // Recreation if age = 4                                                                              
    public static final int CRECAGE5          = 153; // Recreation if age = 5                                                                              
    public static final int CRECAAGE25        = 154; // Presence of recreation tours or stops if age less than 25 yrs                                      
    public static final int CRECTOTAG1        = 155; // Presence of recreation tours or stops if age 25-35 yrs                                             
    public static final int CRECTOTAG2        = 156; // Presence of recreation tours or stops if age 35-45 yrs                                             
    public static final int CRECTOTAG3        = 157; // Presence of recreation tours or stops if age 45-55 yrs                                             
    public static final int CRECTOTAG4        = 158; // Presence of recreation tours of stops if age 55-65 yrs                                             
    public static final int CRECAGE6          = 159; // Recreation if age 65+ yrs                                                                          
    public static final int CRECALINC         = 160; // Presense of recreation tours or stops if low income                                                
    public static final int CRECAHINC         = 161; // Presence of recreation tours or stops if high income                                               
    public static final int CREC2AD           = 162; // Recreation if 2+ adults in HH   
    public static final int CRECHH5           = 163; // Recreation if one adult, worker, 1+ children, no preschooler                                       
    public static final int CRECHH6           = 164; // Recreation if one adult, worker, 1+ children, 1+ preschooler                                       
    public static final int CRECHH7           = 165; // Recreation if two+ adults, workers=adults, no children                                             
    public static final int CRECHH8           = 166; // Recreation if two+ adults, workers=adults, 1+ children, no preschooler                             
    public static final int CRECHH9           = 167; // Recreation if two+ adults, workers=adults, 1+ children, 1+ preschooler                             
    public static final int CRECHH10          = 168; // Recreation if two+ adults, workers<adults, no children                                             
    public static final int CRECHH11          = 169; // Recreation if two+ adults, workers<adults, 1+ children, no preschooler                             
    public static final int CRECHH12          = 170; // Recreation if two+ adults, workers<adults, 1+ children, 1+ preschooler                             
    public static final int CRECHH13          = 171; // Recreation if two+ adults, no workers, no children                                                 
    public static final int CRECHH14          = 172; // Recreation if two+ adults, no workers, 1+ children, no preschooler                                 
    public static final int CRECHH15          = 173; // Recreation if two+ adults, no workers, 1+ children, 1+ preschooler                                 
    public static final int COTHAFEM          = 174; // Presence of other tours or stops if female                                                         
    public static final int COTHTTA2          = 175; // Presence of other tours or stops if age 11-13                                                      
    public static final int COTHTTA3          = 176; // Presence of other tours or stops if age 14-15                                                      
    public static final int COTHTT4C          = 177; // Presence of other tours or stops if age 16-17 & cars > adults                                      
    public static final int COTHAAGE25        = 178; // Presence of other tours or stops if age less than 25 yrs old                                       
    public static final int COTHAGE5          = 179; // Presence of other tours or stops if age 55-65 yrs                                                  
    public static final int COTHAGE6          = 180; // Presence of other tours or stops if age 65+ yrs                                                    
    public static final int COTHTTLI          = 181; // Presence of other tours or stops if low income                                                     
    public static final int COTHAHINC         = 182; // Presence of other tours or stops if high income                                                    
    public static final int COTHNWA           = 183; // Other if non-working adult in HH                                                                   
    public static final int COTH1AD           = 184; // Other if there is only 1 adult in the HH                                                           
    public static final int COTHHH1           = 185; // Other if one adult, non worker, no children                                                        
    public static final int COTHHH2           = 186; // Other if one adult, non worker, 1+ children, no preschooler                                        
    public static final int COTHHH3           = 187; // Other if one adult, non worker, 1+ children, 1+ preschooler                                        
    public static final int COTHHH4           = 188; // Other if one adult, worker, no children                                                            
    public static final int COTHHH5           = 189; // Other if one adult, worker, 1+ children, no preschooler                                            
    public static final int COTHHH6           = 190; // Other if one adult, worker, 1+ children, 1+ preschooler                                            
    public static final int COTHHH7           = 191; // Other if two+ adults, workers=adults, no children                                                  
    public static final int COTHHH8           = 192; // Other if two+ adults, workers=adults, 1+ children, no preschooler                                  
    public static final int COTHHH9           = 193; // Other if two+ adults, workers=adults, 1+ children, 1+ preschooler                                  
    public static final int COTHHH10          = 194; // Other if two+ adults, workers<adults, no children                                                  
    public static final int COTHHH11          = 195; // Other if two+ adults, workers<adults, 1+ children, no preschooler                                  
    public static final int COTHHH12          = 196; // Other if two+ adults, workers<adults, 1+ children, 1+ preschooler                                  
    public static final int COTHHH13          = 197; // Other if two+ adults, no workers, no children                                                      
    public static final int COTHHH14          = 198; // Other if two+ adults, no workers, 1+ children, no preschooler                                      
    public static final int COTHHH15          = 199; // Other if two+ adults, no workers, 1+ children, 1+ preschooler                                      
    public static final int CHFEM             = 200; // Stay at home if female                                                                             
    public static final int CHOMEAGE1         = 201; // Stay at home if age <=1                                                                            
    public static final int CHAGE1            = 202; // Stay at home if age 25-35 yrs                                                                      
    public static final int CHAGE2            = 203; // Stay at home if age 35-45 yrs                                                                      
    public static final int CHAGE3            = 204; // Stay at home if age 45-55 yrs                                                                      
    public static final int CHAGE4            = 205; // Stay at home if age 55-65 yrs                                                                      
    public static final int CHAGE5            = 206; // Stay at home if age 65+ yrs                                                                        
    public static final int CHOMELINC         = 207; // Stay at home if low income household                                                               
    public static final int CHOMEA0           = 208; // Stay at home if autos = 0                                                                          
    public static final int CHOMEALTA         = 209; // Stay at home if 0 < autos < adults                                                                 
    public static final int CHOMENWA          = 210; // Stay at home if non-working adult in HH                                                            
    public static final int CHOMEHH1          = 211; // Stay at Home if one adult, non worker, no children                                                 
    public static final int CHOMEHH2          = 212; // Stay at Home if one adult, non worker, 1+ children, no preschooler                                 
    public static final int CHOMEHH3          = 213; // Stay at Home if one adult, non worker, 1+ children, 1+ preschooler                                 
    public static final int CHOMEHH4          = 214; // Stay at Home if one adult, worker, no children                                                     
    public static final int CHOMEHH5          = 215; // Stay at Home if one adult, worker, 1+ children, no preschooler                                     
    public static final int CHOMEHH6          = 216; // Stay at Home if one adult, worker, 1+ children, 1+ preschooler                                     
    public static final int CHOMEHH7          = 217; // Stay at Home if two+ adults, workers=adults, no children                                           
    public static final int CHOMEHH8          = 218; // Stay at Home if two+ adults, workers=adults, 1+ children, no preschooler                           
    public static final int CHOMEHH9          = 219; // Stay at Home if two+ adults, workers=adults, 1+ children, 1+ preschooler                           
    public static final int CHOMEHH10         = 220; // Stay at Home if two+ adults, workers<adults, no children                                           
    public static final int CHOMEHH11         = 221; // Stay at Home if two+ adults, workers<adults, 1+ children, no preschooler                           
    public static final int CHOMEHH12         = 222; // Stay at Home if two+ adults, workers<adults, 1+ children, 1+ preschooler                           
    public static final int CHOMEHH13         = 223; // Stay at Home if two+ adults, no workers, no children                                               
    public static final int CHOMEHH14         = 224; // Stay at Home if two+ adults, no workers, 1+ children, no preschooler                               
    public static final int CHOMEHH15         = 225; // Stay at Home if two+ adults, no workers, 1+ children, 1+ preschooler                               
    public static final int CDISTTR2          = 226; // Number of tours if home to work distance is 1.0 - 2.5 miles                                        
    public static final int CDISTTR3          = 227; // Number of tours if home to work distance is 2.5 - 5.0 miles                                        
    public static final int CDISTTR4          = 228; // Number of tours if home to work distance is 5.0 - 10.0 miles                                       
    public static final int CDISTTR5          = 229; // Number of tours if home to work distance is 10.0 - 25.0 miles                                      
    public static final int CDISTTR6          = 230; // Number of tours if home to work distance is 25.0 - 50.0 miles                                      
    public static final int CDISTTR7          = 231; // Number of tours if home to work distance is 50.0+ miles                                            
    public static final int CDISTST2          = 232; // Number of stops on work tours if home to work distance is 1 - 2.5 miles                            
    public static final int CDISTST3          = 233; // Number of stops on work tours if home to work distance is 2.5 - 5.0 miles                          
    public static final int CDISTST4          = 234; // Number of stops on work tours if home to work distance is 5.0 - 10.0 miles                         
    public static final int CDISTST5          = 235; // Number of stops on work tours if home to work distance is 10.0 - 25.0 miles                        
    public static final int CDISTST6          = 236; // Number of stops on work tours if home to work distance is 25.0 - 50.0 miles                        
    public static final int CDISTST7          = 237; // Number of stops on work tours if home to work distance is 50.0+ miles                              
    public static final int CDISTSTA2         = 238; // Number of stops on all tours if home to work distance is 1.0 - 2.5 miles                           
    public static final int CDISTSTA3         = 239; // Number of stops on all tours if home to work distance is 2.5 - 5.0 miles                           
    public static final int CDISTSTA4         = 240; // Number of stops on all tours if home to work distance is 5.0 - 10.0 miles                          
    public static final int CDISTSTA5         = 241; // Number of stops on all tours if home to work distance is 10.0 - 25.0 miles                         
    public static final int CDISTSTA6         = 242; // Number of stops on all tours if home to work distance is 25.0 - 50.0 miles                         
    public static final int CDISTSTA7         = 243; // Number of stops on all tours if home to work distance is 50.0+ miles
    public static final int CNWRKTRS          = 244; // number of work tours
    public static final int CNSCHTRS          = 245; // number of school tours
    public static final int CNSHPTRS          = 246; // number of shop tours
    public static final int CNRECTRS          = 247; // number of recreation tours
    public static final int CNOTHTRS          = 248; // number of other tours
    public static final int CLSUMTOURSHP      = 249; // Destination choice logsum * number of shop tours
    public static final int CLSUMTOURRECOTH   = 250; // Destination choice logsum * number of rectation or other tours
    public static final int CLSUMSTOPPERTOUR  = 251; // Destination choice logsum * number of other stops per tour
    public static final int CLSUMCTOUR        = 252; // Destination choice logsum (college) * total number of tours
    public static final int CLSUMCSPT         = 253; // Destination choice logsum (college) * total stops per tour
    
    // new variables for ohio
    public static final int CSCHSTOPSCHTOUR   = 254; // Presence of school stops on school tours
    public static final int CWRKSTOPSCHTOUR   = 255; // Presence of work stops only on school tours
    public static final int CHOMEWORKHOME     = 256; // Pattern is Home-Work-Home
    
}