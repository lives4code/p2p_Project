#!/bin/bash

echo "---starting peers---"
java PeerProcess.java 1001 &
java PeerProcess.java 1002 &
echo "---peers started---"
