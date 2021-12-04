#!/bin/bash

for id in {1002..1008}
do
	echo "---diff $id---"
	diff -c ../Files_From_Prof/project_config_file_small/project_config_file_small/1001/theFile ../Files_From_Prof/project_config_file_small/project_config_file_small/$id/theFile
	echo
done

grep -Ev "have" out.txt | grep "PEER CHECK\|SERVER END\|download\|not done"; grep ERROR out.txt
echo "---done checking---"
