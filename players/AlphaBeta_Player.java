package players;

import game.Board;
import game.Move;

import java.util.Random;
import java.util.Vector;

/**
 * A player that uses AlphaBeta for the game of Quoridor.
 */
public class AlphaBeta_Player extends MinMax_Player {
	
	public AlphaBeta_Player(){
		rng = new Random();
	}

	/**
	 * returns a random move choice
	 */
	public Move make_move(Board b) {
		long start = System.currentTimeMillis();

		Vector<Move> moves = b.get_possible_moves(self_id);
		Vector<Move> good_moves = new Vector<Move>();
		// Increment to zero at start of loop
		int depth = -1;
		evaluated = 0;
		int[] alphas = { LOSS, LOSS, LOSS, LOSS };
		// Iterative deepening
		while ( System.currentTimeMillis() - start < 1000 )
		{
			depth += 1;
			// new list of good moves
			good_moves.removeAllElements();
			int best = LOSS;
			for ( int i=0 ; i < moves.size(); i++) {
				// eval
				int[] eval = eval_move( b, moves.get(i), depth, alphas);
				int myScore = eval[self_id.ordinal()];
				if ( myScore > best) {
					best = myScore;
					// reset alphas
					alphas[ self_id.ordinal() ] = best;
					good_moves.removeAllElements();
					good_moves.add(moves.get(i) );
				} else if( myScore == best ) {
					good_moves.add(moves.get(i));
				}
			}
		}
		// Print moves
		if ( debug ) {
			for ( int i= 0 ; i < good_moves.size() ; i++) {
				System.out.println( good_moves.get(i) );
			}
			System.out.println("Depth: " + depth );
			System.out.println("Evaluated: " + evaluated );
			System.out.println("=====================================" );
		}
		return good_moves.get(rng.nextInt(good_moves.size()));
	}

	/**
	 * new move
	 */
	public int[] eval_move(Board board, Move m, int depth, int[] alphas) {
		evaluated += 1;
		// use this one so the orignal isn't modified
		//System.out.println( m );
		// Generate target node
		Board b = new Board( board );
		b.apply_move( m );
		// also copy alphas
		int[] alpha = (int[]) alphas.clone() ;
		// Should check for game over here

		// Check for depth limit
		if (depth == 0) {
			return eval_board( b);
		} //else

		// Do minMax of kids
		int[] eval = { LOSS, LOSS, LOSS, LOSS };
		// get moves by next player
		Player_ID p = next_player(m.getPlayer_making_move() );
		Vector<Move> moves = b.get_possible_moves( p );
		for ( int i=0; i < moves.size(); i++ ) {
			int[] e = eval_move( b , moves.get(i), depth -1, alphas );
			// Check if this move gives next player better pos
			for ( int j=0; j < 4; j++) {
				if ( j == p.ordinal() ) {
					if( e[j] > eval[j] )
						eval = e;
				// other player
				} else {
					if ( e[j] > eval[j])
					return eval;
				}

			}
		}
		return eval;
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
