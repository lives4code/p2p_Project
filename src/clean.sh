#!/bin/bash
rm *.class
rm *.txt
rm ../log/log*

for id in {1..6}
do
	rm ../peers/100$id/thefile
done

