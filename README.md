# p2p_Project
<!-- GETTING STARTED -->
## Getting Started

Use the following instructions to set up and test the project

### Installation

Use the following instructions to clone the repository.
1. Clone the repo
   ```sh
   git clone https://github.com/lives4code/p2p_Project.git
   ```

### Customize your testing configurations

Use the following instructions to edit the common and peer configurations

2. Go to `/p2p_Project/cfg/Common.cfg` to edit the common configurations
3. Go to `/p2p_Project/cfg/PeerInfo.cfg` to edit the peer configurations

### Set up with your UF username 

Use the following instructions to set up the project to use your personal UF username

4. Go to `/p2p_Project/src/StartRemotePeers.java`
5. Change the username variable on line 61 to your UF ID

### Generate an ssh key

Use the following instructions to generate your personal ssh key and add it to the remote machine

6. Enter `ssh-keygen -t ed25519` to generate an ssh key
7. Enter `ssh-copy-id -i ~/.ssh/id_ed25519 YOUR_UFID@lin114-00.cise.ufl.edu` to copy the ssh key to the remote machine

### Upload the code to the remote machines

Use the following instructions to transfer the code to the UF machines

8. In /p2p_Project, enter `sftp YOUR_UFID@lin114-00.cise.ufl.edu` to remote into the machine using sftp
9. Enter `mkdir p2p_Project` to create a directory for the project files
10. Enter `put -r src` to upload the src folder
11. Enter `put -r cfg` to upload the cfg folder
12. Enter `put -r large_cfg` to upload the large_cfg folder
13. Enter `put -r log` to upload the empty log folder
14. Enter `put -r peers` to upload the peers folder
15. Enter `exit` to return to your local machine

### Compile the code

Use the following instructions to compile the program remotely

16. Enter `ssh YOUR_UFID@lin114-00.cise.ufl.edu` to remote into the UF machine
17. Enter `cd p2p_Project/src` to navigate to the project's source folder
18. Enter `make` to compile the code
19. Enter `exit` to return to your local machine

### Test the program on remote machines

Use the following instructions to run the program

20. In /p2p_Project/src, enter `./compileJava` to compile StartRemotePeers.java
21. Enter `java StartRemotePeers` to run the program

### Verify your results

Use the following instructions to check the output of the program

22. Enter `ssh YOUR_UFID@lin114-##.cise.ufl.edu` to remote into machine ##
23. Enter `cd p2p_Project/log` to view the logs
24. Go to the `peers` folder to see the tranferred file
25. In the src folder, run `./checkDiff.sh` to run a complete comparison of the files received by each peer
