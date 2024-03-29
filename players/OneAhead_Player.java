package players;

import game.Board;
import game.Move;

import java.util.Random;
import java.util.Vector;

/**
 * A player that looks one move ahead for the game of Quoridor.
 */
public class OneAhead_Player implements Player {
	private Random rng;
	private Player_ID self_id;
	private static Player_ID[] players_ids = new Player_ID[]{Player_ID.PLAYER_1, Player_ID.PLAYER_2, Player_ID.PLAYER_3, Player_ID.PLAYER_4};
	private boolean debug;
	
	int LOSS = -10000;
	int TIE =       0;
	int WIN =   10000;

	public OneAhead_Player(){
		rng = new Random();
		debug = false;
	}

	/**
	 * sets debuging
	 */
	public void set_debug(boolean debug_val){
		debug = debug_val;
	}

	/**
	 * returns a random move choice
	 */
	public Move make_move(Board b) {
		Vector<Move> moves = b.get_possible_moves(self_id);
		Vector<Move> good_moves = new Vector<Move>();
		int best = LOSS;
		for ( int i=0 ; i < moves.size(); i++) {
			int[] eval = eval_move( b, moves.get(i));
			int myScore = eval[self_id.ordinal()];
			if ( myScore > best) {
                if (debug) {
                    System.out.format("Better---old: %d\n#[%d,%d,%d,%d]  ",
                        best, eval[0], eval[1], eval[2], eval[3]);
                        System.out.println( moves.get( i ) );
                }
				best = myScore;
				good_moves.removeAllElements();
				good_moves.add(moves.get(i) );
			} else if( myScore == best ) {
                if (debug ) {
                    System.out.format("#[%d,%d,%d,%d]  ",
                        eval[0], eval[1], eval[2], eval[3]);
                        System.out.println( moves.get( i ) );
                }
				good_moves.add(moves.get(i));
			} else { // no a good move
                if (debug ) {
                    System.out.format(" [%d,%d,%d,%d]  ",
                        eval[0], eval[1], eval[2], eval[3]);
                        System.out.println( moves.get( i ) );
                }
            }
		}
		
		if( debug ) {
			for ( int i= 0 ; i < good_moves.size() ; i++) {
				System.out.println( good_moves.get(i) );
			}
			//System.out.println( good_moves.size() );
			System.out.println("=====================================" );
		}
		return good_moves.get(rng.nextInt(good_moves.size()));
	}

	/**
	 * new move
	 */
	public int[] eval_move(Board board, Move m) {
		// use this one so the orignal isn't modified
		//System.out.println( m );
		Board b = new Board( board );
		b.apply_move( m );

		return eval_board( b);
	}

	public int[] eval_board(Board b){

		// if game is over
		if ( b.is_game_over() ) {
			Player_ID p = b.compute_winner();
			if (p == null) {
				int[] eval = { TIE, TIE, TIE, TIE};
				return  eval;
			} else {
				int[] eval = { LOSS, LOSS, LOSS, LOSS};
				eval[p.ordinal()] = WIN;
				return eval;
			}
		}

		// other wise
		int shortp[] = new int[4];

		for ( int i = 0; i < 4; i++) {
			// negate shortest path so shorter paths score higher
			shortp[i]= - b.shortest_path(players_ids[i]);
		}

		// eval is mine - best other player
		int[] eval = { WIN, WIN, WIN, WIN };
		for ( int i = 0; i < 4; i++){
			for (int j = 0 ;j < 4; j++) {
				if( i != j && shortp[i] - shortp[j] < eval[i] )
					eval[i] = shortp[i] - shortp[j];
			}
		}
		return eval;
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

	/**
	 * sets the random seed
	 */
	public void set_seed(long seed){
		rng.setSeed(seed);
	}
}
