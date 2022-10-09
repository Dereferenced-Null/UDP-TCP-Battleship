------------------Submission by Luke Haigh-----------------
This code is a submission for course: SENG 4500
Student number: c3303309

------------------Setup Instructions-----------------------
Program uses Java 17.
Program is multithreaded and as such requires a computer that
is capable of supporting at least 3 threads.
The file must be compiled using "javac BattleshipPlayer.java".
Following compilation, the program can be executed using the
command "java BattleshipPlayer <Broadcast IP> <Port>" i.e.
"java BattleshipPlayer 192.168.0.255 5000" for local Wifi
connections. The port number and IP must be the same for
players who wish to play together.

IMPORTANT NOTE:
Due to the UPD port requirements, this program cannot be run
twice on the same machine without the use of either docker
containers or multiple machines on the same network as attempting
to run the program twice on a single computer will result in a
address bind error as the port is already in use.
