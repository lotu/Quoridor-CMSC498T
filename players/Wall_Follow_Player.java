package players;

import game.Board;
import game.Move;
import game.Coordinate_Pair;

import java.util.Random;
import java.util.Vector;

/**
 * A wall following player for the game of Quoridor.  It follows the left
 * hand wall and doesn't place walls.  Also, it doesn't try to jump so 
 * another player can sometimes get it stuck going in circles.  This is 
 * a horrible strategy in case you are wondering.
 */
public class Wall_Follow_Player implements Player {
	private Random rng;
	private Player_ID self_id;
	// the direction the player last traveled
	// S = 0 , W = 1 , N = 2 , E = 3
	private int idir;
	// how much to change to go in a direction
	// indexed in the same fashion as idir
	//                       S, W, N,E
	private int x_delta[] = {0,-1, 0,1};
	private int y_delta[] = {1, 0,-1,0};

	public Wall_Follow_Player(){
		rng = new Random();
	}

	/**
	 * returns a move choice
	 */
	public Move make_move(Board b) {
		// get the x and y location of the player
		Coordinate_Pair<Integer, Integer> loc = b.get_player_location(self_id);
		int x = loc.get_x_coordinate();
		int y = loc.get_y_coordinate();

		/* try to go left then straight, then right, the back*/
		// + 3 is the same as -1 mod 4 (i.e. one less than current direction)
		// + 2 is the same as -2 mod 4
		// we do this because Java % doesn't behave for negative numbers
		for ( int i = (idir + 3) % 4; i != (idir + 2) % 4; i = (i + 1) % 4)
		{
			// Print direction we are trying
			//System.out.println("Player " + (self_id.ordinal() +1) + ":  " + i );
			int target_x = x + x_delta[i];
			int target_y = y + y_delta[i];
			// can_move_to does row column
			if ( b.can_move_to(y, x, target_y, target_x) )
			{
				idir = i;
				return new Move( Move.MOVE_TYPE.MOVE_PAWN,
								 self_id,
								 // Why are x and y reversed? It's because that is
								 // the order they is in the in constructor!!! Crazy
								 new Coordinate_Pair<Integer, Integer>(target_y, target_x));
			}
		}
		// Only get here if we are somehow trapped/force to jump
		Vector<Move> possible_moves = b.get_possible_moves(self_id);
		return possible_moves.get(rng.nextInt(possible_moves.size()));
	}

	/**
	 * sets this players id for use with the game engine
	 */
	public void set_id(Player_ID id){
		self_id = id;
		// Set the last direction traveled based on player id
		// this way they appear to travel along the left wall
		switch ( self_id.ordinal() + 1 )
		{
			// S = 0 , W = 1 , N = 2 , E = 3
			case 1:
			idir = 3;
			break;
			case 2:
			idir = 0;
			break;
			case 3:
			idir = 1;
			break;
			case 4:
			idir = 2;
			break;
		}
	}

	/**
	* sets the random seed
	*/
	public void set_seed(long seed){
		rng.setSeed(seed);
	}

	/**
	 * ignores move notifications
	 */
	public void notify_of_move(Player_ID playerThatMadeMove, Move moveMade,
			Board resultingBoard) {
		// no code
	}
}
