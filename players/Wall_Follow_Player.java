package players;

import game.Board;
import game.Move;
import game.Coordinate_Pair;

import java.util.Random;
import java.util.Vector;

enum Direction{
		S, W, N, E
};


/**
 * A wall following player for the game of Quoridor.  It follows the right
 * hand wall and dosen't place walls.  This is a horriable 
 * stategy in case you are wondering.
 */
public class Wall_Follow_Player implements Player {
	private Random rng;
	private Player_ID self_id;
	private Direction last_dir;
	//                       S,W,N,E
	private int x_delta[] = {0,1,0,-1};
	private int y_delta[] = {1,0,-1,0};
	private int idir;

	public Wall_Follow_Player(){
		rng = new Random();
	}

	/**
	 * returns a move choice
	 */
	public Move make_move(Board b) {
		Coordinate_Pair<Integer, Integer> loc = b.get_player_location(self_id);
		int x = loc.get_x_coordinate();
		int y = loc.get_y_coordinate();
		System.out.println("Player " + (self_id.ordinal() +1) + ":  " +
			loc.get_x_coordinate() + "," + loc.get_y_coordinate());
		// + 3 is the same as -1 mod 4
		// we do this because java % dosen't behave for negative numbers
		for ( int i = (idir + 3) % 4 ; i <  ((idir + 3) % 4) + 3; i++)
		{
			System.out.println("Player " + (self_id.ordinal() +1) + ":  " + i );
			int target_x = x + x_delta[i%4];
			int target_y = y + y_delta[i%4];
			if ( b.can_move_to(x, y, target_x, target_y) )
			{
				idir = i % 4;
				System.out.println("Player " + (self_id.ordinal() +1) + ":  " +
									target_x + "," + target_y );
				return new Move( Move.MOVE_TYPE.MOVE_PAWN,
								 self_id,
								 // Why? are x and y reversed? it's because that is the
								 // the order they is in the in constructor!!! Crazy
								 new Coordinate_Pair<Integer, Integer>(target_y, target_x));
			}
		}
		// Only get here if we are somehow trapped/force to jump
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
		// because I can
		switch ( self_id.ordinal() + 1 )
		{
			case 1:
			last_dir = Direction.W;
			break;
			case 2:
			last_dir = Direction.E;
			break;
			case 3:
			last_dir = Direction.S;
			break;
			case 4:
			last_dir = Direction.N;
			break;
		}
		idir = last_dir.ordinal();
		System.out.println("Player " + last_dir + ":  " + idir );
	}
}
