import socket
import sys
import random

L = 7
H = 6

ADDRESS_DEF = "127.0.0.1"
PORT_NB = 8080

SERVER_TURN = 0
CLIENT_TURN = 1

damier = [[' ' for _ in range(H)] for _ in range(L)]
icons = ["X", "O"]


def display():
	global L, H, damier
	print("-"*(2*L+3))
	for y in range(H):
		print("| ", end='')
		for x in range(L):
			print(damier[x][y], end=' ');
		print("|")
	print("-"*(2*L+3))
	print("/\\"+(2*L-1)*" "+"/\\")
	print()


def c(x, y, dx, dy):
	global L, H, damier
	x2 = x+dx
	y2 = y+dy
	c = 0
	end = None
	while 1:
		if not (x2<L and x2>=0 and y2<H and y2>=0) : break
		if not (damier[x][y] == damier[x2][y2]) : break
		c += 1
		x2 += dx
		y2 += dy
	return c



def has_won(x, y):
	return (
	(c(x,y,1,0) + c(x,y,-1,0) + 1 >= 4)
	or (c(x,y,0,1) + 1 >= 4)
	or (c(x,y,-1,-1) + c(x,y,1,1) + 1 >= 4)
	or (c(x,y,-1,1) + c(x,y,1,-1) + 1 >= 4)
	)


def can_play(x):
	global H, damier
	for y in range(H-1, -1, -1):
		if damier[x][y] == ' ': return y # can play column x at height y
	return -1 # cannot play



def updateTurnCount(val):
	if val == CLIENT_TURN:
		return SERVER_TURN
	else:
		return CLIENT_TURN




def main(argc, argv):
	global L, H, damier, ADDRESS_DEF, PORT_NB

	isHost = False;

	for arg in argv[1:]:
		if arg == "--host":
			isHost = True
		else:
			ADDRESS_DEF = arg

	mySocket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
	mySocket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)

	print("You are {}the host".format("" if isHost else "NOT "))


	if isHost:

		try:
			mySocket.bind((ADDRESS_DEF, PORT_NB))
		except socket.error:
			print("Error on bind: make sure there is not already an host!")
			sys.exit()

		print("Waiting for the other player to connect to {}:{}!".format(ADDRESS_NB, PORT_NB))
		mySocket.listen(1)
		connection, address = mySocket.accept()
		mySocket = connection # rename the new socket for further uses
		# TODO : add a try in case the user ctl-c at this time

	else:

		print("Trying to connect to {}:{}".format(ADDRESS_DEF, PORT_NB))

		try:
			mySocket.connect((ADDRESS_DEF, PORT_NB))
		except socket.error:
			print("Error on connect: make sure the host is up!")
			sys.exit(1)

		print("You are connected!")


	cells_left = L*H
	x_choice = None
	y_choice = None

	isGameOver = False
	playTurn = None
	myTurn = None
	otherTurn = None



	# agreeing on who begins
	if isHost:
		playTurn = random.randint(0,1) # chosing who begins
		mySocket.send(str(playTurn).encode("Utf8")) # sending the id of the beginner
		myTurn = SERVER_TURN # only play when the turn is SERVER_TURN
		otherTurn = CLIENT_TURN
	else:
		playTurn = int(mySocket.recv(1).decode("Utf8")) # receive 1 byte i.e. the beginner's turn nb
		myTurn = CLIENT_TURN # only play when the turn is CLIENT_TURN
		otherTurn = SERVER_TURN


	# display who begins
	if (playTurn == myTurn):
		print("You begin!")
	else:
		print("The opponent begins!")

	print("You play as the '{}'".format(icons[myTurn]))


	# main loop
	while not isGameOver:

		if playTurn == myTurn:
			y_choice = -1
			while y_choice == -1:
				x_choice = input("What column to play? ")
				if x_choice in [str(k) for k in range(L)]:
					x_choice = int(x_choice)
					y_choice = can_play(x_choice)

			damier[x_choice][y_choice] = icons[myTurn] # update the grid
			cells_left -= 1 # decrease left cell count
			display()

			# check for victory
			if has_won(x_choice, y_choice):
				print("Congratulations! You win!")
				isGameOver = True

			# check for draw
			if cells_left == 0:
				print("Too bad, it's a draw!")
				isGameOver = True


			mySocket.send(str(x_choice).encode("Utf8"))

			playTurn = updateTurnCount(playTurn)

		else:
			x_choice = int(mySocket.recv(1).decode("Utf8")) # wait for a column nb
			y_choice = can_play(x_choice)

			damier[x_choice][y_choice] = icons[otherTurn] # update the grid
			cells_left -= 1 # decrease left cell count
			display()

			# check for victory
			if has_won(x_choice, y_choice):
				print("Oh no! You lost!")
				isGameOver = True

			# check for draw
			if cells_left == 0:
				print("Too bad, it's a draw!")
				isGameOver = True


			playTurn = updateTurnCount(playTurn)


	if isHost:
		mySocket.close()


main(len(sys.argv), sys.argv)
