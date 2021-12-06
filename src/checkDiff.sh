#!/bin/bash
echo "---begin checking---"

for id in {1002..1006}
do
        echo "---diff $id---"
        diff ../peers/1001/thefile ../peers/$id/thefile
        echo
done

echo "---done checking---"
