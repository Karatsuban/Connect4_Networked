import java.net.*;
import java.io.*;
import java.util.*;
import java.util.Random;

public class NWC4
{

	public static int L = 7;
	public static int H = 6;

	public static String ADDRESS_DEF = "127.0.0.1";
	public static int PORT_NB = 8080;

	public static int SERVER_TURN = 0;
	public static int CLIENT_TURN = 1;

	public static char[][] damier = new char[L][H];
	public static char[] icons = {'X','O'};

	public static Socket clientSocket = null;
	public static ServerSocket serverSocket = null;

	public static int MAGIC = 48; // offset between the value 0 and its ascii value

	public static BufferedReader in = null;
	public static PrintWriter out = null;

	public static Scanner scan = null;


	public static void display()
	{
		System.out.println("-".repeat(2*L+3));
		for (int y=0; y<H; y++)
		{
			System.out.print("| ");
			for (int x=0; x<L; x++)
			{
				System.out.print(damier[x][y]+" ");
			}
			System.out.println("|");
		}
		System.out.println("-".repeat(2*L+3));
		System.out.println("/\\"+" ".repeat(2*L-1)+"/\\");
		System.out.println();
	}


	public static int c(int x, int y, int dx, int dy)
	{
		int x2 = x+dx;
		int y2 = y+dy;
		int c = 0;
		while (true)
		{
			if ( !(x2<L && x2>=0 && y2<H && y2>=0) ) break;
			if (damier[x][y] != damier[x2][y2]) break;
			c += 1;
			x2 += dx;
			y2 += dy;
		}
		return c;
	}

	public static boolean has_won(int x, int y)
	{
		return(
		(c(x,y,1,0) + c(x,y,-1,0) + 1 >= 4)
	        || (c(x,y,0,1) + 1 >= 4)
	        || (c(x,y,-1,-1) + c(x,y,1,1) + 1 >= 4)
	        || (c(x,y,-1,1) + c(x,y,1,-1) + 1 >= 4)
		);
	}

	public static int can_play(int x)
	{
		for (int y=H-1; y>0; y-=1)
		{
			if (damier[x][y] == ' ') return y; // can play column x at height y
		}
		return -1; // cannot play
	}

	public static int updateTurnCount(int val)
	{
		if (val == CLIENT_TURN)
			return SERVER_TURN;
		else
			return CLIENT_TURN;
	}



	public static void main(String[] args)
	{
		for (int x=0; x<L; x++)
		{
			for (int y=0; y<H; y++)
			{
				damier[x][y] = ' ';
			}
		}

		boolean isHost = false;

		for (int i=0; i<args.length; i++)
		{
			if (args[i].equals("--host"))
			{
				isHost = true;
			}
			else
			{
				ADDRESS_DEF = args[i];
			}
		}



		System.out.println("You are" + (isHost ? ' ' : " NOT ") + "the host");
		InetAddress addr = null;

		// trying to convert from String address to InetAddress
		try {
			addr = InetAddress.getByName(ADDRESS_DEF); //ADDRESS_DEF);
		}
		catch(UnknownHostException e)
		{
			System.out.println("Host unknown : "+e.getMessage());
			System.exit(1);
		}



		if (isHost)
		{
			// try opening a server socket
			try {
				serverSocket = new ServerSocket(PORT_NB, 0, addr);
			}
			catch (IOException e)
			{
				System.out.println("Error when opening socket: make sure there is not already a host!");
				System.exit(1);
			}

			System.out.println("Waiting for the other player to connect to "+ADDRESS_DEF+":"+PORT_NB);

			// try to listen in to new connection
			try
			{
				clientSocket = serverSocket.accept();
			}
			catch (IOException e)
			{
				System.out.println("Error listening for connection : "+e.getMessage());
				System.exit(1);
			}

			System.out.println("You are connected!");

			// try to set up the output stream
			try
			{
				out = new PrintWriter(clientSocket.getOutputStream(), true);
			}
			catch (IOException e)
			{
				System.out.println("Error when setting up the output stream : "+e.getMessage());
				System.exit(1);
			}


			// try to set up the input stream
			try
			{
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(), "UTF-8"));
			}
			catch (IOException e)
			{
				System.out.println("Error when setting up the input stream : "+e.getMessage());
				System.exit(1);
			}


		}
		else // if we are the client
		{

			System.out.println("Trying to connect to "+ADDRESS_DEF+":"+PORT_NB);

			// try opening a client socket
			try
			{
				clientSocket = new Socket(ADDRESS_DEF, PORT_NB);
			}
			catch(UnknownHostException e1)
			{
				System.out.println("Host unknown : "+e1.getMessage());
				System.exit(1);
			}
			catch(IOException e2)
			{
				System.out.println("Error on connect: make sure the host is up!");
				System.exit(1);
			}

			System.out.println("You are connected!");

			// trying to set up the input stream
			try
			{
				in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
			}
			catch (IOException e)
			{
				System.out.println("Error when setting up the input stream : "+e.getMessage());
				System.exit(1);
			}

			// trying to set up the output stream
			try
			{
				out = new PrintWriter(clientSocket.getOutputStream(), true);
			}
			catch (IOException e)
			{
				System.out.println("Error when setting up the output stream : "+e.getMessage());
				System.exit(1);
			}


		}


		int cells_left = L*H;
		int x_choice = -1;
		int y_choice = -1;

		boolean isGameOver = false;
		int playTurn = -1;
		int myTurn = -1;
		int otherTurn = -1;


		// agreeing on who begins
		if (isHost)
		{
			Random rand = new Random();
			playTurn = rand.nextInt(2); // number between 0 and 1

			out.write(playTurn+MAGIC); // sending the id of the beginner
			out.flush();

			myTurn = SERVER_TURN;
			otherTurn = CLIENT_TURN;
		}
		else
		{

			try
			{
				playTurn = in.read()-MAGIC;
			}
			catch(IOException e)
			{
				System.out.println("Error with the in-side of the pipe!");
				System.out.println(e.getMessage());
			}

			myTurn = CLIENT_TURN;
			otherTurn = SERVER_TURN;
		}

		// display who begins
		if (playTurn == myTurn)
			System.out.println("You begin!");
		else
			System.out.println("The opponent begins!");

		System.out.println("You play as the "+ icons[myTurn]);



		// main loop
		while (!isGameOver)
		{

			if (playTurn == myTurn)
			{
				y_choice = -1;
				while (y_choice == -1)
				{
					System.out.print("What column to play? ");
					scan = new Scanner(System.in);
					if (!scan.hasNextInt(10))
					{
						scan.next();
					}
					else
					{
						x_choice = scan.nextInt();
						if (x_choice >=0 && x_choice < L)
						{
							y_choice = can_play(x_choice);
						}
					}
				}
				damier[x_choice][y_choice] = icons[myTurn]; // update the grid
				cells_left -= 1; // decrease left cell count
				display();


				// check for victory
				if (has_won(x_choice, y_choice))
				{
					System.out.println("Congratulations! You win!");
					isGameOver = true;
				}

				// check for draw
				if (cells_left == 0)
				{
					System.out.println("Too bad, it's a draw!");
					isGameOver = true;
				}

				out.write(x_choice+MAGIC);
				out.flush();

				playTurn = updateTurnCount(playTurn);

			}
			else
			{

				x_choice = -1;

				try
				{
					x_choice = in.read()-MAGIC; // wait for a column nb
				}
				catch(IOException e)
				{
					System.out.println("Error with the in-side of the pipe!");
					System.out.println(e.getMessage());
				}

				y_choice = can_play(x_choice);

				damier[x_choice][y_choice] = icons[otherTurn]; // update the grid
				cells_left -= 1;
				display();


				// check for victory
				if (has_won(x_choice, y_choice))
				{
					System.out.println("Oh no! You lost!");
					isGameOver = true;
				}

				// check for draw
				if (cells_left == 0)
				{
					System.out.println("Too bad, it's a draw!");
					isGameOver = true;
				}

				playTurn = updateTurnCount(playTurn);

			}

		}

		if (isHost)
		{
			try
			{
				serverSocket.close();
			}
			catch(IOException e)
			{
				System.out.println("Error when closing server socket :"+e.getMessage());
			}
		}


		try
		{
			clientSocket.close();
		}
		catch(IOException e)
		{
			System.out.println("Error when closing client socket :"+e.getMessage());
		}


	}

}
