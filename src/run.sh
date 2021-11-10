#!/bin/bash

echo "---starting peers---"
java PeerProcess 1001 &
java PeerProcess 1002 &
echo "---peers started---"
