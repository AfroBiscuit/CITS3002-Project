CITS3002 Networks & Security Project - Dominic Cockman (20927611) & Alex Guglielmino (20933584)

#############
#Server use:#
#############

The server only had the option to exit/close early after starting up.
The server broadcasts on port# 4444.

To close the server, type '-q', 'quit', or 'exit' anytime.

#############
#Client use:#
#############

The client is executed without any commands.
The client then waits for single commands during execution.
The client will continue taking command until the user inputs a command to quit or exit, closing the client.

These commands are...

	-u certificate
		upload a certificate from the client to the server
	-a filename
		add or replace a file to the trustcloud
	-h hostname:port
		connect to the specified host on the specified port
	-l
		list all of the files stored on the server in the clients stdout
	-f filename
		list the contents of a specified file on the clients stdout
	-c number
		provide the required circumference (length) of a ring of trust
	-v filename certificate
		vouch for the authenticity of an existing file in the trustcloud server using the indicated certificate
	-q
		quit the current client/server
	exit
		same as -q
	quit
		same as -q

NOTE: All filenames (the arguments to -a, -u, -v) must be with quotation marks.
	Some example command calls for clarification:
		-a "filename.txt"
		-f "filename"
		-u "certificate"
		-v "filename.txt" "certificate"
		-h localhost:4444
		-c 3
		-l
		-q
		exit
		quit

Upon connecting to a server, the default circumference is set to 1. 
The client may change this at any time (using <-c> <number>). 
This circumference will then be remembered by the server and used in calculating the ring of trust in future calls to <-f>.

########################
#Files already present:#
########################

There are currently only 3 X509 Certificates present on the server, however there are 6 in the ClientCerts folder. On top of that, there are currently two small test files in the ServerFiles folder.
currently, the private keys are stored in a folder within the project folder, however, in an ideal situation these keys would have a unique place on each clients computer.

In this project, we have chosen to allow vouching only from Certificates on the client's side, rather than from a server held certificate, however the ring of trust uses the certificates
held on the server, as do several other methods. 

On top of this, we have chosen to implement a command reader to accept the flags, which are as seen above. We've elected to view the '-c' command as a way to set the circumference for the rest of the session
and then compare the ring of trust for each file against that.

