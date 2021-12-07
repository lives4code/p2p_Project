#!/bin/bash

#sleepTime=30s
make
echo "---starting peers---"
java PeerProcess 1001 &
#PID1=$!
java PeerProcess 1002 &
#PID2=$!
java PeerProcess 1003 &
java PeerProcess 1004 &
java PeerProcess 1005 &
java PeerProcess 1006 &
echo "---peers started---"
#sleep $sleepTime
#echo "killing processes after sleeping" $sleepTime
#kill $PID1
#kill $PID2
rm run.sh; mv Test.sh run.sh
