#!/bin/bash

# A bash script to test the compliantNode code by running the 
# Simulation file for 3x3x3x2 = 54 combinations. 
# There are four required command line arguments: 
# p_graph (.1, .2, .3), p_malicious (.15, .30, .45), 
# p_txDistribution (.01, .05, .10), and numRounds (10, 20). 
#
# User can choose number of tests e.g. only run the first 10 tests
#
clear
echo Enter number of tests you want to run [0 to 54]
read testNum
echo
num=0
for i in .1 .2 .3;
    do
       	for j in .15 .30 .45;
     		do
     			for k in 0.01 .04 .10;
     				do
     					for l in 10 20;
     						do
                                if !(($num < $testNum)); then
                                    exit
                                fi
                                    echo ________________________________
                                    echo
                                    echo Test $num:
									echo Simulation number $i $j $k $l:
                                    echo ________________________________
                                    echo
									java Simulation $i $j $k $l
     						    ((num++))
                            done
     				done
     		done
    done 

