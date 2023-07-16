# Connect4_Networked
One-file server and client to play Connect 4 over the network

## Description

Each source file in this repository contains the code for a server and a client to play the game of Connect4 over the network. Each player can use a different version of the server/client as they are compatible.

## Setup

For the time being, games can only be hosted at the address 127.0.0.1, at the port 8080.
You should make sure this port is not already in use before playing.

### Setup for the C file

This file is to be compiled under Linux with the command :   
> $ gcc NWC4.c

### Setup for the Python file

No particular setup is needed. Just make sure you have Python3.X installed on your system.


## Use

One player is the host, the other is the client. The host must execute their file with the parameter '-h' (for host).   
The other just has to execute their file with no argument at all.


## Updates to come

+ Being able to chose an address and a port number via command line parameters
+ Foolproof every function that needs it
+ Add a server/client in Java
+ Prettify the output
