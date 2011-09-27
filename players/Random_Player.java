package players;

import game.Board;
import game.Move;

import java.util.Random;
import java.util.Vector;

/**
 * A random player for the game of Quoridor.
 */
public class Random_Player implements Player {
	private Random rng;
	private Player_ID self_id;
	
	public Random_Player(){
		rng = new Random();
	}

	/**
	 * returns a random move choice
	 */
	public Move make_move(Board b) {
		Vector<Move> possible_moves = b.get_possible_moves(self_id);
		return possible_moves.get(rng.nextInt(possible_moves.size()));
	}

	/**
	 * ignores move notifications
	 */
	public void notify_of_move(Player_ID playerThatMadeMove, Move moveMade,
			Board resultingBoard) {
		// no code
	}

	/**
	 * sets this players id for use with the game engine
	 */
	public void set_id(Player_ID id){
		self_id = id;
	}
}
