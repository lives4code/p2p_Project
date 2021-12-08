#!/bin/bash
rm *.class
rm *.txt
rm ../log/log*

for id in {2..6}
do
	rm ../peers/100$id/thefile
done

