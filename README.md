# Connect4_Networked
One-file server and client to play Connect 4 over the network

## Description

Each source file in this repository contains the code for a server and a client to play the game of Connect4 over the network. Each player can use a different version of the server/client as they are compatible.

## Use

One player is the host, the other is the client.  
Games can be hosted at any ip adress available (only tested on private IPV4 addresses), but the port number used is 8080  
You should make sure this port is not already in use before playing.  
You can launch you file with the following (optionnal) arguments (in any order) :    
- --host : specify you will be the host
- X.Y.Z.A : any IPV4 address  

Make sure the two players use the same adress and that only one of them is the host!


## Updates to come

+ Use any size of 'board' to play on
