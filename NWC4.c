#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <time.h>

#include <sys/types.h>
#include <sys/socket.h>
#include <netdb.h>

#include <errno.h>

#define L 7
#define H 6

#define ADDRESS "192.168.56.1"
#define PORT_NB "8080"

#define TRUE 1
#define FALSE 0

#define SERVER_TURN 0
#define CLIENT_TURN 1

#define MAGIC 48 // offset between the value 0 and its ascii value

char damier[H][L];
char icons[2] = {'X', 'O'};

void display(){
	for (int y=0; y<=2*(L+1); y++) printf("-");
	printf("\n");
	for (int y = 0; y<H; y++){
		printf("| ");
		for(int x = 0; x<L; x++){
			printf("%c ", damier[x][y]);
		}
		printf("|\n");
	}
	for (int y=0; y<=2*(L+1); y++) printf("-");
	printf("\n");
	for (int y=0; y<=2*(L+1); y++){
		if (y==0 || y==2*L+1)  printf("/");
		else if (y==1 || y==2*L+2)  printf("\\");
		else printf(" ");
	}
	printf("\n");
}

int c(int x, int y, int dx, int dy){
	int x2 = x+dx;
	int y2 = y+dy;
	int c = 0;
	while(x2<L && x2>=0 && y2<H && y2>=0 && damier[x][y] == damier[x2][y2]){
		c++;
		x2 += dx;
		y2 += dy;
	}
	return c;
}

int has_won(int x, int y){
	return (
	(c(x,y,1,0) + c(x,y,-1,0) + 1 >= 4)
	||(c(x,y,0,1) + 1 >= 4)
	||(c(x,y,-1,-1) + c(x,y,1,1) + 1 >= 4)
	||(c(x,y,-1,1) + c(x,y,1,-1) + 1 >= 4)
	);
}

int can_play(int x){
	// FIXME : stange beahvior for L=H=3
	for(int y=H; y>=0; y--){
		if (damier[x][y] == ' ') return y; // can play at height y
	}
	return -1; // cannot play
}



void updateTurnCount(int *val)
{
	if (*val == CLIENT_TURN)
		*val = SERVER_TURN;
	else
		*val = CLIENT_TURN;
}



int main(int argc, char* argv[]){

	srand(time(NULL));

	int isHost;
	char str[15];
	strcpy(str, "-h");

	if (argc == 2)
	{
		if (strcmp(argv[1], str) == 0)
			isHost = TRUE;
		else
			isHost = FALSE;
	}else
	{
		isHost = FALSE; // Default
	}


	printf("You are %sthe host\n", isHost == TRUE ? "" : "NOT ");


	// host & non-host
	int status, sockfd;
	struct addrinfo hints;
	struct addrinfo *res;


	// non-host stuff
	int bytes_rcvd;
	int rcvd_buf[50];
	int rcvd_len = 50;


	// host stuff
	struct sockaddr_storage non_host_addr;
	socklen_t addr_size;
	int accept_sockfd;
	int sent_buf;
	int bytes_sent;
	int yes = 1;


	memset(&hints, 0, sizeof hints); // make sure the struct is empty
	hints.ai_family = AF_UNSPEC;     // don't care IPv4 or IPv6
	hints.ai_socktype = SOCK_STREAM; // TCP stream sockets
	hints.ai_flags = AI_PASSIVE;     // fill in my IP for me


	if ((status = getaddrinfo(ADDRESS, PORT_NB, &hints, &res)) != 0) {
		fprintf(stderr, "getaddrinfo error: %s\n", gai_strerror(status));
		exit(1);
	}


	if ((sockfd = socket(res->ai_family, res->ai_socktype, res->ai_protocol)) == -1)
	{
		printf("Error on socket !\n");
		exit(1);
	}


	if (isHost)
	{
		// reuse socket if needed
		if ( setsockopt(sockfd, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int)) == -1)
		{
			printf("Error on setsockopt: cannot reuse port!\n");
			close(sockfd);
			exit(1);
		}


		// bind
		if ( bind(sockfd, res->ai_addr, res->ai_addrlen) == -1)
		{
			printf("Error on bind: make sure there is not already a host!\n");
			close(sockfd);
			exit(1);
		}
	}else
	{
		// connect
		if ( connect(sockfd, res->ai_addr, res->ai_addrlen) == -1)
		{
			printf("Error on connect: make sure the host is up!\n");
			close(sockfd);
			exit(1);
		}
	}

	if (isHost)
	{
		// listen
		printf("Waiting for the other player to connect !\n");

		if ( listen(sockfd, 1) == -1)
		{
			printf("Error on listen!\n");
			close(sockfd);
			exit(1);
		}

		if (( accept_sockfd = accept(sockfd, (struct sockaddr*)&non_host_addr, &addr_size)) == -1)
		{
			printf("Error on accept!\n");
			close(sockfd);
			exit(1);
		}
		close(sockfd); // close the listening socket
		sockfd = accept_sockfd; // rename the new socket for further uses
	}





	// Create the game
	int cells_left = L*H;
	int x_choice;
	int y_choice;

	int isGameOver = FALSE;
	int playTurn;
	int myTurn;
	int otherTurn;

	// clearing the board
	for(int x=0; x<L; x++){
		for(int y=0; y<H; y++){
			damier[x][y] = ' ';
		}
	}


	// agreeing on who begins
	if (isHost)
	{

		playTurn = rand()%2; // chosing who begins
		sent_buf = playTurn+MAGIC; // sending 1 byte (from int value, 'convert' to ascii value)
		bytes_sent = send(sockfd, &sent_buf, 1, 0); // sending the id of the beginner
		myTurn = SERVER_TURN; // only play when the turn is SERVER_TURN
		otherTurn = CLIENT_TURN;
	}
	else
	{
		bytes_rcvd = recv(sockfd, rcvd_buf, rcvd_len-1, 0);
		playTurn = rcvd_buf[0]-MAGIC; // get the beginner's turn nb (get ascii value, 'convert' to int)
		myTurn = CLIENT_TURN; // only play when the turn is CLIENT_TURN
		otherTurn = SERVER_TURN;
	}


	// displaying who begins
	if (playTurn == myTurn)
		printf("You begin!\n");
	else
		printf("The opponent begins!\n");

	printf("You play as the '%c'\n", icons[myTurn]);

	// main loop
	while (!isGameOver)
	{

		if (playTurn == myTurn)
		{

			do {
				printf("What column to play? ");
				scanf("%i", &x_choice);
			}while((y_choice = can_play(x_choice)) == -1);

			damier[x_choice][y_choice] = icons[myTurn]; // update the grid
			cells_left -= 1; // decrease left cell count
			display();

			// check for victory
			if(has_won(x_choice,y_choice))
			{
				printf("Congratulations! You win!\n");
				isGameOver = TRUE;
			}

			// check for draw
			if (cells_left == 0)
			{
				printf("Too bad, it's a draw!\n");
				isGameOver = TRUE;
			}

			int sent = x_choice+MAGIC;
			bytes_sent = send(sockfd, &sent, 1, 0); // send the value to host

			updateTurnCount(&playTurn); // update the turn count

		}
		else
		{
			bytes_rcvd = recv(sockfd, rcvd_buf, rcvd_len-1, 0); // wait for a column nb

			x_choice = rcvd_buf[0]-MAGIC;
			y_choice = can_play(x_choice);

			damier[x_choice][y_choice] = icons[otherTurn]; // update the grid
			cells_left -= 1; // decrease left cell count
			display(); // display the grid

			// check for victory
			if(has_won(x_choice,y_choice)) {
				printf("Oh no! You lost!\n");
				isGameOver = TRUE;
			}

			// check for draw
			if (cells_left == 0)
			{
				printf("Too bad, it's a draw!\n");
				isGameOver = TRUE;
			}

			updateTurnCount(&playTurn); // update the turn count
		}

	}

	close(sockfd);
	freeaddrinfo(res);

	return 0;
}


