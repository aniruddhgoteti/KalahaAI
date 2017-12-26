package ai;

import ai.Global;

import java.io.*;
import java.net.*;

import javax.swing.*;
import java.awt.*;
import kalaha.*;

/**
 * This is the main class for your Kalaha AI bot. Currently it only makes a
 * random, valid move each turn.
 * 
 * @author Johan Hagelbäck
 */

// Author: Aniruddh Goteti
// Node class for defining the tree
class Node {
	private final int player;
	private final GameState board;
	private int utilityValue = 0;
	private int bestMove = -1;

	public Node(GameState currentBoard, int player) {
		board = currentBoard;
		this.player = player;
		resetUtiVal();
	}

	public int getUtilityValue() {
		return utilityValue;
	}

	public int getBestMove() {
		return bestMove;
	}

	// Depth First Search Method
	public int depthFirstSearch(int depthLevel, int a, int b) {

		int maxDepthLevel = 10;
		// calculate utility value if depth is reached.
		if (depthLevel == maxDepthLevel)
			return calculateBoardUtilityValue();

		boolean isUtilityValueAssigned = false;

		// iterating through moves

		for (int moveIndex = 6; moveIndex >= 1; moveIndex--) {

			if (board.moveIsPossible(moveIndex)) {

				GameState nextBoard = board.clone();
				nextBoard.makeMove(moveIndex);

				Node nextNeighbourNode = new Node(nextBoard, player);
				int nodeScore = nextNeighbourNode.depthFirstSearch(depthLevel + 1, a, b);
				updateUtiVal(nodeScore, moveIndex);

				// Alpha Beta Pruning
				// a==alpha
				// b==beta
				if (board.getNextPlayer() == player) {
					if (nodeScore > b)
						break;
					a = Math.max(nodeScore, a);
				} else {
					if (nodeScore < a)
						break;
					b = Math.min(nodeScore, b);
				}
				isUtilityValueAssigned = true;
			}
		}

		if (!isUtilityValueAssigned) {
			return calculateBoardUtilityValue();
		} else {
			return utilityValue;
		}
	}

	// resetting the utility value
	private void resetUtiVal() {
		if (board.getNextPlayer() == player)
			utilityValue = Integer.MIN_VALUE;
		else
			utilityValue = Integer.MAX_VALUE;
	}

	// updating the utility value
	private void updateUtiVal(int nextNodeUtilityValue, int move) {
		if (board.getNextPlayer() == player) {
			if (nextNodeUtilityValue > utilityValue) {
				utilityValue = nextNodeUtilityValue;
				bestMove = move;
			}
		} else {
			if (nextNodeUtilityValue < utilityValue) {
				utilityValue = nextNodeUtilityValue;
				bestMove = move;
			}
		}
	}

	// calculating the utility values according to the rules of the game
	private int calculateBoardUtilityValue() {

		int player1;

		int player2;

		if (board.getNextPlayer() == player) {
			player1 = board.getNextPlayer();
			player2 = board.getOppositePlayer();
		} else {
			player1 = board.getOppositePlayer();
			player2 = board.getNextPlayer();
		}

		int gameEnd = board.getWinner();
		int tempUtility = 0;

		if (gameEnd == -1) {

			int ScorePlayer1 = board.getScore(player1);
			int ScorePlayer2 = board.getScore(player2);

			if (ScorePlayer1 > ScorePlayer2)
				tempUtility += 7;
			else if (ScorePlayer1 < ScorePlayer2)
				tempUtility -= 7;

			for (int i = 1; i < 7; i++) {

				if (board.getSeeds(i, player1) != 0) {
					if ((7 - i) == board.getSeeds(i, player1))
						tempUtility += 5;
				}
				if (board.getSeeds(i, player2) != 0) {
					if ((7 - i) == board.getSeeds(i, player2)) {
						tempUtility -= 5;
					}
				}
			}
		} else if (gameEnd == player1)
			tempUtility = 50;
		else if (gameEnd == player2)
			tempUtility = -50;

		utilityValue = tempUtility;
		return utilityValue;

	}

}

public class AIClient implements Runnable {
	private int player;
	private JTextArea text;

	private PrintWriter out;
	private BufferedReader in;
	private Thread thr;
	private Socket socket;
	private boolean running;
	private boolean connected;

	/**
	 * Creates a new client.
	 */
	public AIClient() {
		player = -1;
		connected = false;

		// This is some necessary client stuff. You don't need
		// to change anything here.
		initGUI();

		try {
			addText("Connecting to localhost:" + KalahaMain.port);
			socket = new Socket("localhost", KalahaMain.port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			addText("Done");
			connected = true;
		} catch (Exception ex) {
			addText("Unable to connect to server");
			return;
		}
	}

	/**
	 * Starts the client thread.
	 */
	public void start() {
		// Don't change this
		if (connected) {
			thr = new Thread(this);
			thr.start();
		}
	}

	/**
	 * Creates the GUI.
	 */
	private void initGUI() {
		// Client GUI stuff. You don't need to change this.
		JFrame frame = new JFrame("My AI Client");
		frame.setLocation(Global.getClientXpos(), 445);
		frame.setSize(new Dimension(420, 250));
		frame.getContentPane().setLayout(new FlowLayout());

		text = new JTextArea();
		JScrollPane pane = new JScrollPane(text);
		pane.setPreferredSize(new Dimension(400, 210));

		frame.getContentPane().add(pane);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setVisible(true);
	}

	/**
	 * Adds a text string to the GUI textarea.
	 * 
	 * @param txt
	 *            The text to add
	 */
	public void addText(String txt) {
		// Don't change this
		text.append(txt + "\n");
		text.setCaretPosition(text.getDocument().getLength());
	}

	/**
	 * Thread for server communication. Checks when it is this client's turn to make
	 * a move.
	 */
	public void run() {
		String reply;
		running = true;

		try {
			while (running) {
				// Checks which player you are. No need to change this.
				if (player == -1) {
					out.println(Commands.HELLO);
					reply = in.readLine();

					String tokens[] = reply.split(" ");
					player = Integer.parseInt(tokens[1]);

					addText("I am player " + player);
				}

				// Check if game has ended. No need to change this.
				out.println(Commands.WINNER);
				reply = in.readLine();
				if (reply.equals("1") || reply.equals("2")) {
					int w = Integer.parseInt(reply);
					if (w == player) {
						addText("I won!");
					} else {
						addText("I lost...");
					}
					running = false;
				}
				if (reply.equals("0")) {
					addText("Even game!");
					running = false;
				}

				// Check if it is my turn. If so, do a move
				out.println(Commands.NEXT_PLAYER);
				reply = in.readLine();
				if (!reply.equals(Errors.GAME_NOT_FULL) && running) {
					int nextPlayer = Integer.parseInt(reply);

					if (nextPlayer == player) {
						out.println(Commands.BOARD);
						String currentBoardStr = in.readLine();
						boolean validMove = false;
						while (!validMove) {
							long startT = System.currentTimeMillis();
							// This is the call to the function for making a move.
							// You only need to change the contents in the getMove()
							// function.
							GameState currentBoard = new GameState(currentBoardStr);
							int cMove = getMove(currentBoard);

							// Timer stuff
							long tot = System.currentTimeMillis() - startT;
							double e = (double) tot / (double) 1000;

							out.println(Commands.MOVE + " " + cMove + " " + player);
							reply = in.readLine();
							if (!reply.startsWith("ERROR")) {
								validMove = true;
								addText("Made move " + cMove + " in " + e + " secs");
							}
						}
					}
				}

				// Wait
				Thread.sleep(100);
			}
		} catch (Exception ex) {
			running = false;
		}

		try {
			socket.close();
			addText("Disconnected from server");
		} catch (Exception ex) {
			addText("Error closing connection: " + ex.getMessage());
		}
	}

	/**
	 * This is the method that makes a move each time it is your turn. Here you need
	 * to change the call to the random method to your Minimax search.
	 * 
	 * @param currentBoard
	 *            The current board state
	 * @return Move to make (1-6)
	 */
	public int getMove(GameState currentBoard) {

		int bestOptimalNodeScore = -Integer.MIN_VALUE; // Assume -infinity
		int bestOptimalMove = 0;

		Node Algorithm = new Node(currentBoard, player);

		int nodeScore = Algorithm.depthFirstSearch(0, bestOptimalNodeScore, Integer.MAX_VALUE);

		if (nodeScore > bestOptimalNodeScore) {
			bestOptimalNodeScore = nodeScore;
			bestOptimalMove = Algorithm.getBestMove();

		}
		return bestOptimalMove;
	}

	/**
	 * Returns a random ambo number (1-6) used when making a random move.
	 * 
	 * @return Random ambo number
	 */
	public int getRandom() {
		return 1 + (int) (Math.random() * 6);
	}
}