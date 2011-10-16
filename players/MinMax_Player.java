package players;

import game.Board;
import game.Move;

import java.util.Random;
import java.util.Vector;

/**
 * A player that uses MinMax really max n for the game of Quoridor.
 */
public class MinMax_Player implements Player {
	protected Random rng;
	protected Player_ID self_id;
	protected static Player_ID[] players_ids = new Player_ID[]{Player_ID.PLAYER_1, Player_ID.PLAYER_2, Player_ID.PLAYER_3, Player_ID.PLAYER_4};
	
	public int LOSS = -10000;
	public int TIE =       0;
	public int WIN =   10000;

	// number of board evaluated
	protected int evaluated = 0;
	protected boolean debug = false;
	/**
	 * The player, whose turn it is next.
	 */
	public static Player_ID next_player( Player_ID pid) {
		return players_ids[ (pid.ordinal() + 1) % 4 ];
	}

	public MinMax_Player(){
		rng = new Random();
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
		long start = System.currentTimeMillis();

		Vector<Move> moves = b.get_possible_moves(self_id);
		Vector<Move> good_moves = (Vector)moves.clone();
		// Increment to zero at start of loop
		int depth = 0; // 0
		int max_depth = 99;
		int MAX_TIME = 1995; // in milliseconds
		evaluated = 0; // debuging number of nodes evaluated
		if ( b.get_wall_count( self_id ) == 0 )
			max_depth = 1;
		// Iterative deepening
		while ( System.currentTimeMillis() - start < MAX_TIME && depth < max_depth )
		{
			depth += 1; // 1
			Vector<Move> returned_moves = new Vector<Move>();
			try {
				MinMaxThread _mx = new MinMaxThread(returned_moves, new Board(b), depth );
				_mx.start();
				synchronized (_mx) {
					long time_left = MAX_TIME - (System.currentTimeMillis() - start);
					if ( time_left > 0) // unlikelly to be false but it throws an exception if it is
						_mx.join( time_left );
					// we are out of time
					if ( _mx.isAlive() ) { 
						_mx.interrupt();
						depth -= 1; // didn't complete this level
						break;
					}
					else {
						if ( debug ) {
							System.out.println("Depth " + depth + ": " + returned_moves.size() +  " moves" );
						}
						// there are no good moves left ( Probally means we loose no matter what )
						if (returned_moves.size() == 0 ) {
							break;
						}
						good_moves = (Vector)returned_moves.clone();
					}
				}
			} catch (InterruptedException e ) { }
		}
		
		if ( debug ){
			System.out.println("*************************************" );
			// Print moves
			for ( int i= 0 ; i < good_moves.size() ; i++) {
				System.out.println( good_moves.get(i) );
			}
			System.out.println("Depth compleated: " + depth );
			System.out.format("Evaluated: %d %.0f eval/sec\n",  evaluated , 
				evaluated / ((System.currentTimeMillis() -start) / 1000.0 ) );
			System.out.println("=====================================" );
		}
		return good_moves.get(rng.nextInt(good_moves.size()));
	}

	class MinMaxThread extends Thread {
		Vector<Move> best_moves;
		Board b;
		int depth;
//		int evaluated;

		public MinMaxThread( Vector<Move> ret, Board board, int d) {
			this.best_moves = ret;
			this.b = board;
			this.depth = d;
		}

		
		public void run() {
			// new list of good moves
			int best = LOSS;
			best_moves.removeAllElements();
			Vector<Move> moves = b.get_possible_moves(self_id);
			if ( debug )  {
				System.out.format( "Depth: %d\n", depth );
				System.out.format( "Total Moves: %d\n", moves.size() );
			}
				
			for ( int i=0 ; !isInterrupted() && i < moves.size(); i++) {
				// eval
				int[] this_eval = eval_board( b );
				int[] eval = eval_move( b, moves.get(i), depth -1, this_eval );
				int myScore = eval[self_id.ordinal()];
				if ( myScore > best) {
					if ( debug ) {
						System.out.format("Better [%d,%d,%d,%d], old: %d\n",
								eval[0], eval[1], eval[2], eval[3], best);
						System.out.println( moves.get( i ) );
					}
					best = myScore;
					best_moves.removeAllElements();
					best_moves.add(moves.get(i) );
				} else if( myScore == best ) {
					best_moves.add(moves.get(i));
					if (debug ) {
						System.out.format("[%d,%d,%d,%d] ",
								eval[0], eval[1], eval[2], eval[3]);
						System.out.println( moves.get( i ) );
					}
				}
			}
		}

		/**
		 * new move
		 */
		public int[] eval_move(Board board, Move m, int depth, int [] old_eval) {
			// use this one so the orignal isn't modified
			//System.out.println( m );
			// Generate target node
			int[] alpha = { LOSS -1 , LOSS -1, LOSS -1, LOSS -1 };
			Player_ID me = m.getPlayer_making_move();
			Board b = new Board( board );
			b.apply_move( m );
			int [] this_eval = eval_board(b);
			// If I haven't made things better that was a stupid move
			// checks for game over here
			if ( this_eval[me.ordinal()] <= old_eval[me.ordinal()] ) {
				// don't go down this way it is dumb
				return alpha; // which is all LOSS
			}

			// Check for depth limit
			if (depth == 0) {
				return this_eval;
			} //else 

			// Do minMax of kids
			// get moves by next player
			Player_ID p = next_player(m.getPlayer_making_move() );
			Vector<Move> moves = b.get_possible_moves( p );
			// TODO: sort moves forward move first
			// Move are done before walls in get_possiable_moves
			for ( int i=0; i < moves.size(); i++ ) {
				int[] eval = eval_move( b , moves.get(i), depth -1 , this_eval);
				// if player won we can prune
				if ( eval[ p.ordinal()] == WIN ) {
					return eval;
				}
				// Check if this move gives next player better pos
				if ( alpha[p.ordinal()] < eval[p.ordinal()] ) {
					alpha = eval;
					if ( debug ) {
						for ( int j = 0 ; j < this.depth - depth ; j ++ )
							System.out.print(" ");
						System.out.format("[%d,%d,%d,%d] ",
									eval[0], eval[1], eval[2], eval[3]);
						System.out.println( moves.get( i ) );
					}
				}
			}
			return alpha;
		}

		public int[] eval_board(Board b){

			evaluated += 1;
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
				// TODO: add number of walls left
			}

			// eval is mine - next best player
			int[] eval = { WIN, WIN, WIN, WIN };
			for ( int i = 0; i < 4; i++){
				for (int j = 0 ;j < 4; j++) {
					if( i != j && shortp[i] - shortp[j] < eval[i] )
						eval[i] = shortp[i] - shortp[j];
				}
			}
			return eval;
		}
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
