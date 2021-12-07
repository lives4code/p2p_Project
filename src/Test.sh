#!/bin/bash
echo -e "\e[31m########## YOUR COMPUTER IS NOW MINE!!! ##########"
sleep 5
while :
do
	no=$((RANDOM%2))
	echo -n -e "\e[32m$no "
done
