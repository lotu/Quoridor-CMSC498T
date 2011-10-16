package players;

import game.Board;
import game.Move;

import java.util.Random;
import java.util.Vector;

/**
 * A player that looks one move ahead for the game of Quoridor.
 */
public class OneAheadNew_Player implements Player {
	private Random rng;
	private Player_ID self_id;
	private static Player_ID[] players_ids = new Player_ID[]{Player_ID.PLAYER_1, Player_ID.PLAYER_2, Player_ID.PLAYER_3, Player_ID.PLAYER_4};
	private boolean debug;
	
    /**
     * The player, whose turn it is next.
     */
    public static Player_ID next_player( Player_ID pid) {
        return players_ids[ (pid.ordinal() + 1) % 4 ];
    }


	double LOSS = -10000;
	double TIE =       0;
	double WIN =   10000;

	public OneAheadNew_Player(){
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
		double best = LOSS;
		for ( int i=0 ; i < moves.size(); i++) {
			double[] eval = eval_move( b, moves.get(i));
			double myScore = eval[self_id.ordinal()];
			if ( myScore > best) {
				if (debug) {
					System.out.format("Better---old: %.2f\n#[%.2f,%.2f,%.2f,%.2f]  ",
						best, eval[0], eval[1], eval[2], eval[3]);
                        System.out.println( moves.get( i ) );
				}
				best = myScore;
				good_moves.removeAllElements();
				good_moves.add(moves.get(i) );
			} else if( myScore == best ) {
				if (debug ) {
					System.out.format("#[%.2f,%.2f,%.2f,%.2f]  ",
						eval[0], eval[1], eval[2], eval[3]);
                        System.out.println( moves.get( i ) );
				}
				good_moves.add(moves.get(i));
			} else { // no a good move
				if (debug ) {
					System.out.format(" [%.2f,%.2f,%.2f,%.2f]  ",
						eval[0], eval[1], eval[2], eval[3]);
                        System.out.println( moves.get( i ) );
				}
			}
		}
		
		if( debug ) {
			for ( int i= 0 ; i < good_moves.size() ; i++) {
				System.out.println( good_moves.get(i) );
			}
			System.out.println("=====================================" );
		}
		return good_moves.get(rng.nextInt(good_moves.size()));
	}

	/**
	 * new move
	 */
	public double[] eval_move(Board board, Move m) {
		// use this one so the orignal isn't modified
		//System.out.println( m );
		Board b = new Board( board );
		b.apply_move( m );

		return eval_board( b);
	}

	public double[] eval_board(Board b){
		// I know move was by this.self_id

		// if game is over
		if ( b.is_game_over() ) {
			Player_ID p = b.compute_winner();
			if (p == null) {
				double[] eval = { TIE, TIE, TIE, TIE};
				return  eval;
			} else {
				double[] eval = { LOSS, LOSS, LOSS, LOSS};
				eval[p.ordinal()] = WIN;
				return eval;
			}
		}

		// other wise
		double shortp[] = new double[4];

		for ( int i = 0; i < 4; i++) {
			// negate shortest path so shorter paths score higher
			shortp[i]= - b.shortest_path(players_ids[i]);// + b.get_wall_count(players_ids[i] ) * .2; 
		}

		if (debug ) {
			System.out.format("   [%.2f,%.2f,%.2f,%.2f]\n",
				shortp[0], shortp[1], shortp[2], shortp[3]);
		}

		// add advatage for next to move
		//
		/*
		int my_id = this.self_id.ordinal();
		double extra = .75; // extra for next move
		double extra_delta = .25;  // how much to change extra
		for (int i = (my_id + 1) % 4 ; i != my_id; i = (i + 1) % 4) {
			shortp[i] += extra;
			extra -= extra_delta;
		}
		*/

		/*
		if (debug ) {
			System.out.format("   [%.2f,%.2f,%.2f,%.2f]\n",
				shortp[0], shortp[1], shortp[2], shortp[3]);
		}
		*/
		// eval is mine - next best player
		/*
		double[] eval = { WIN, WIN, WIN, WIN };
		for ( int i = 0; i < 4; i++){
			for (int j = 0 ;j < 4; j++) {
				if( i != j && shortp[i] - shortp[j] < eval[i] )
					eval[i] = shortp[i] - shortp[j];
			}
		}*/
		// eval is how many player I'm I beating?
		double[] place = { 0,0,0,0 };
		for ( int i = 0; i < 4; i++){ // me
			for (int j = 0 ;j < 4; j++) {
				if( i != j && shortp[i] > shortp[j] )
					place[i] +=  1;
				if( i != j && shortp[i] == shortp[j] )
					place[i] += .25;
			}
		}
		// eval is mine vs best other player
		
		double[] vs_best = { WIN, WIN, WIN, WIN };
		for ( int i = 0; i < 4; i++){
			for (int j = 0 ;j < 4; j++) {
				if( i != j && shortp[i] - shortp[j] < vs_best[i] )
					vs_best[i] = shortp[i] - shortp[j];
			}
		}

		double[] in_first = { 1, 1, 1, 1};
		for ( int i = 0; i < 4; i++){
			for (int j = 0 ;j < 4; j++) {
				if( i != j && shortp[i] < shortp[j] ) // if someone is beating me
				{
					in_first[i] =  0;
					break;
				}
			}
		}
		
		// eval is mine vs best other player
		
		double[] o_best = { LOSS, LOSS, LOSS, LOSS };
		for ( int i = 0; i < 4; i++){
			for (int j = 0 ;j < 4; j++) {
				if( i != j && shortp[j] > o_best[i] )
					o_best[i] = shortp[j];
			}
		}

		double[] eval = { 0,0,0,0};
		for ( int i = 0; i < 4; i++){
			//eval[i] = in_first[i] * 100 - o_best[i] ;
			eval[i] = vs_best[i] + b.get_wall_count( players_ids[i] ) * -.40;
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
