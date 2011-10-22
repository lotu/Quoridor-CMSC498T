package players;

import players.My_Move.MOVE_TYPE;
import players.Player_ID;

import game.Board;
import game.Move;
import game.Coordinate_Pair;

import java.util.Random;
import java.util.Vector;

import game.RectangularGrid;
import game.Coordinate_Pair;
import game.Cell_Status;
import game.Cell;
import game.Board;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;
import java.util.Vector;
import java.util.Comparator;
import java.util.PriorityQueue;


/**
 * A player that uses Max N for the game of Quoridor.  Prunes aggressivelly "bad" moves 
 and uses iterative deepening to stay under time
 */
public class P1_Bullen implements Player {
	protected Random rng;
	protected Player_ID self_id;
	protected static Player_ID[] players_ids = new Player_ID[]{Player_ID.PLAYER_1, Player_ID.PLAYER_2, Player_ID.PLAYER_3, Player_ID.PLAYER_4};
	
	// Large constants for Win and loss
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

	public P1_Bullen(){
		rng = new Random();
	}

	/**
	 * sets debuging
	 */
	public void set_debug(boolean debug_val){
		debug = debug_val;
	}


	/**
	 * returns a move choice
	 */
	public Move make_move(Board orig_board) {
		// When we started
		long start = System.currentTimeMillis();
		// Convert board to my faster board type
		My_Board b = new My_Board( orig_board );

		Vector<My_Move> moves = b.get_possible_moves(self_id);
		Vector<My_Move> good_moves = (Vector)moves.clone();
		// Increment to zero at start of loop
		int depth = 0; // 0
		int max_depth = 99;
		int MAX_TIME = 1970; // in milliseconds I seam to go over sometime when the machine is under load
		evaluated = 0; // debuging number of nodes evaluated
		// if we are out of wall their is no point in going very deep
		if ( b.get_wall_count( self_id ) == 0 )
			max_depth = 1;
		// Iterative deepening
		while ( System.currentTimeMillis() - start < MAX_TIME && depth < max_depth )
		{
			depth += 1; // 1
			Vector<My_Move> returned_moves = new Vector<My_Move>();
			try {
				MinMaxThread _mx = new MinMaxThread(returned_moves, new My_Board(b), depth );
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
		My_Move ret_move = good_moves.get(rng.nextInt(good_moves.size()));
		return  ret_move.to_OldMove();
	}

	/**
	* Thread to run one level of iterative deepening.  It can be interupted on
	* the top level, when the host is out of time.  This is really MaxN
	* but minMax stuck.
	*/
	class MinMaxThread extends Thread {
		Vector<My_Move> best_moves;
		My_Board b;
		int depth;
//		int evaluated;

		public MinMaxThread( Vector<My_Move> ret, My_Board board, int d) {
			this.best_moves = ret;
			this.b = board;
			this.depth = d;
		}

		
		public void run() {
			// new list of good moves
			int best = LOSS;
			best_moves.removeAllElements();
			Vector<My_Move> moves = b.get_possible_moves(self_id);
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
		 * Expand board with move m, and return evaluation of node.
		 * depth is the how much deeper we can go.
	     * old_eval is the result of the eval function on the parent so we don't have to run it 
		 * on each child.
		 */
		public int[] eval_move(My_Board board, My_Move m, int depth, int [] old_eval) {
			// use this one so the orignal isn't modified
			//System.out.println( m );
			// Generate target node
			// Initalize to worse than a LOSS
			int[] alpha = { LOSS -1 , LOSS -1, LOSS -1, LOSS -1 };
			Player_ID me = m.getPlayer_making_move();
			My_Board b = new My_Board( board );
			b.apply_move( m );
			int [] this_eval = eval_board(b);
			// If I haven't made things better that was a stupid move
			// checks for game over here
			if ( this_eval[me.ordinal()] <= old_eval[me.ordinal()] ) {
				// don't go down this way it is dumb (or se we tell ourselves)
				return alpha; // which is all LOSS - 1
			}

			// Check for depth limit
			if (depth == 0) {
				return this_eval;
			} //else 

			// Do Max N of kids
			// get moves by next player
			Player_ID p = next_player(m.getPlayer_making_move() );
			Vector<My_Move> moves = b.get_possible_moves( p );
			// TODO: sort moves forward move first
			// My_Move are done before walls in get_possiable_moves
			// So it sorted enough with the pruning
			for ( int i=0; i < moves.size(); i++ ) {
				int[] eval = eval_move( b , moves.get(i), depth -1 , this_eval);
				// if player won we can prune
				if ( eval[ p.ordinal()] == WIN ) {
					return eval;
				}
				// Check if this move gives next player better eval
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

		/**
		 * Produce the evaluation of a board for each player.  If the 
		 * game is over then use WIN/LOSS numbers, otherwise 
		 * min ( shortest_path ( me ) - max( shortest_path( other_players ) ) )
		 * it dosen't look like this in the code but it equivilent
		 */
		public int[] eval_board(My_Board b){

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

			// other wise we need to estimate the winner
			int shortp[] = new int[4];

			for ( int i = 0; i < 4; i++) {
				// negate shortest path so shorter paths score higher
				shortp[i]= - b.shortest_path(players_ids[i]);
				// TODO: add number of walls left 
				// adding number of walls was never shown to be a good idea
				// could explore further though
			}

			// eval is mine -  best other player
			int[] eval = { WIN, WIN, WIN, WIN };
			for ( int i = 0; i < 4; i++){
				for (int j = 0 ;j < 4; j++) {
					if( i != j && shortp[i] - shortp[j] < eval[i] )
						eval[i] = shortp[i] - shortp[j];
				}
			}
			return eval;
		}
	} // End of MinMaxThread Class


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
} // End of P1_Bullen class

//////////////////////////////////////////////////////////////////////////////
//////////////////////////////////////////////////////////////////////////////
////   _____         _              ____                      _ 
////  |  ___|_ _ ___| |_ ___ _ __  | __ )  ___   __ _ _ __ __| |
////  | |_ / _` / __| __/ _ \ '__| |  _ \ / _ \ / _` | '__/ _` |
////  |  _| (_| \__ \ ||  __/ |    | |_) | (_) | (_| | | | (_| |
////  |_|  \__,_|___/\__\___|_|    |____/ \___/ \__,_|_|  \__,_|
//// 
////
//// Below is a modified version of the game driver.  It should run exactlly 
//// the same as the code that was posted on the due date October 16th.
//// It uses A* and cacheing to speed things up.  This improves the number of
//// nodes evaluate per second from ~1,200 to ~25,000 

/**
 * This class contains all of the state information for a game of Quoridor. The methods for manipulating
 * this state, according to the rules of Quoridor, are also provided. 
 * 
 * @author bswilson
 *
 */
class My_Board {
	private static final int BOARD_SIZE = 9;
	private static final int INITIAL_WALLS = 5;
	private RectangularGrid<Cell_Status> board;
	private My_Coordinate_Pair[] player_location;
	private Path[] cached_path;
	private HashMap<My_Coordinate_Pair, Boolean> horizontal_wall_placement_locations;
	private HashMap<My_Coordinate_Pair, Boolean> vertical_wall_placement_locations;
	private int[] walls;
	
	// Cache
	public int cache_hit;
	public int cache_miss;

	/**
	 * Construct the initial Quoridor board and player locations.
	 */
	public My_Board(){
		//initialize an empty board with all cell open
		Cell_Status[][] cells = new Cell_Status[BOARD_SIZE][BOARD_SIZE];
		for(int i = 0; i < BOARD_SIZE; i++){
			for(int j = 0; j < BOARD_SIZE; j++){
				cells[i][j] = Cell_Status.FREE;
			}
		}
		board = new RectangularGrid<Cell_Status>(BOARD_SIZE, BOARD_SIZE, cells);
		walls = new int[]{INITIAL_WALLS, INITIAL_WALLS, INITIAL_WALLS, INITIAL_WALLS};
		
		player_location = new My_Coordinate_Pair[4];
		//put player pawns on the board in starting locations
		board.get_cell(0, BOARD_SIZE/2).set_data(Cell_Status.P1);
		player_location[0] = new My_Coordinate_Pair(0, BOARD_SIZE/2);
		board.get_cell(BOARD_SIZE/2, BOARD_SIZE-1).set_data(Cell_Status.P2);
		player_location[1] = new My_Coordinate_Pair(BOARD_SIZE/2, BOARD_SIZE - 1);
		board.get_cell(BOARD_SIZE-1, BOARD_SIZE/2).set_data(Cell_Status.P3);
		player_location[2] = new My_Coordinate_Pair(BOARD_SIZE-1, BOARD_SIZE/2);
		board.get_cell(BOARD_SIZE/2, 0).set_data(Cell_Status.P4);
		player_location[3] = new My_Coordinate_Pair(BOARD_SIZE/2, 0);

		// Create cached_path
		cached_path = new Path[4];
		cache_hit = 0;
		cache_miss = 0;
		
		horizontal_wall_placement_locations = new HashMap<My_Coordinate_Pair, Boolean>();
		vertical_wall_placement_locations = new HashMap<My_Coordinate_Pair, Boolean>();
	}
	
	/**
	 * Copy constructor to make a deep copy
	 * @param b
	 */
	public My_Board(My_Board b){
		//copy all cell statuses
		Cell_Status[][] cells = new Cell_Status[BOARD_SIZE][BOARD_SIZE];
		for(int i = 0; i < BOARD_SIZE; i++){
			for(int j = 0; j < BOARD_SIZE; j++){
				cells[i][j] = b.get_board().get_cell(i, j).get_data();
			}
		}
		
		board = new RectangularGrid<Cell_Status>(BOARD_SIZE, BOARD_SIZE, cells);
		walls = new int[]{b.walls[0], b.walls[1], b.walls[2], b.walls[3]};
		
        player_location = new My_Coordinate_Pair[4];
		//put player pawns on the board in starting locations
		board.get_cell(b.player_location[0].get_y_coordinate(), b.player_location[0].get_x_coordinate()).set_data(Cell_Status.P1);
		player_location[0] = new My_Coordinate_Pair(b.player_location[0].get_y_coordinate(), b.player_location[0].get_x_coordinate());
		board.get_cell(b.player_location[1].get_y_coordinate(), b.player_location[1].get_x_coordinate()).set_data(Cell_Status.P2);
		player_location[1] = new My_Coordinate_Pair(b.player_location[1].get_y_coordinate(), b.player_location[1].get_x_coordinate());
		board.get_cell(b.player_location[2].get_y_coordinate(), b.player_location[2].get_x_coordinate()).set_data(Cell_Status.P3);
		player_location[2] = new My_Coordinate_Pair(b.player_location[2].get_y_coordinate(), b.player_location[2].get_x_coordinate());
		board.get_cell(b.player_location[3].get_y_coordinate(), b.player_location[3].get_x_coordinate()).set_data(Cell_Status.P4);
		player_location[3] = new My_Coordinate_Pair(b.player_location[3].get_y_coordinate(), b.player_location[3].get_x_coordinate());
	
		// Create cached_path
		cached_path = new Path[4];
		cache_hit = 0;
		cache_miss = 0;

		horizontal_wall_placement_locations = new HashMap<My_Coordinate_Pair, Boolean>();
		vertical_wall_placement_locations = new HashMap<My_Coordinate_Pair, Boolean>();
		for(My_Coordinate_Pair key : b.horizontal_wall_placement_locations.keySet()){
			int row = key.get_y_coordinate();
			int col = key.get_x_coordinate();
			board.get_cell(row, col).get_neighbors().remove(board.get_cell(row-1, col));
			board.get_cell(row-1, col).get_neighbors().remove(board.get_cell(row, col));
			board.get_cell(row, col+1).get_neighbors().remove(board.get_cell(row-1, col+1));
			board.get_cell(row-1, col+1).get_neighbors().remove(board.get_cell(row, col+1));

			horizontal_wall_placement_locations.put(new My_Coordinate_Pair(row, col), true);
		}
		for(My_Coordinate_Pair key : b.vertical_wall_placement_locations.keySet()){
			int row = key.get_y_coordinate();
			int col = key.get_x_coordinate();
	
			board.get_cell(row, col).get_neighbors().remove(board.get_cell(row, col-1));
			board.get_cell(row, col-1).get_neighbors().remove(board.get_cell(row, col));
			board.get_cell(row+1, col).get_neighbors().remove(board.get_cell(row+1, col-1));
			board.get_cell(row+1, col-1).get_neighbors().remove(board.get_cell(row+1, col));

			vertical_wall_placement_locations.put(new My_Coordinate_Pair(row, col), true);
		}
	}

	/**
	 * Copy constructor to make a deep copy.  Used to copy form the real board.  
	 * Notes are in place to make it obvious you are edit this constructor
	 * @param b
	 */
	public My_Board(Board b){
		//** FROM BOARD XXX
		//copy all cell statuses
		Cell_Status[][] cells = new Cell_Status[BOARD_SIZE][BOARD_SIZE];
		for(int i = 0; i < BOARD_SIZE; i++){
			for(int j = 0; j < BOARD_SIZE; j++){
				cells[i][j] = b.get_board().get_cell(i, j).get_data();
			}
		}
		
		board = new RectangularGrid<Cell_Status>(BOARD_SIZE, BOARD_SIZE, cells);
		walls = new int[]{b.get_wall_count(Player_ID.PLAYER_1), 
						  b.get_wall_count(Player_ID.PLAYER_2), 
						  b.get_wall_count(Player_ID.PLAYER_3), 
						  b.get_wall_count(Player_ID.PLAYER_4) };
		
		//** FROM BOARD XXX
        player_location = new My_Coordinate_Pair[4];
		//put player pawns on the board in starting locations
		board.get_cell(b.get_player_location(Player_ID.PLAYER_1).get_y_coordinate(), 
					   b.get_player_location(Player_ID.PLAYER_1).get_x_coordinate()).set_data(Cell_Status.P1);
		player_location[0] = new My_Coordinate_Pair(b.get_player_location(Player_ID.PLAYER_1).get_y_coordinate(),
												    b.get_player_location(Player_ID.PLAYER_1).get_x_coordinate());
		board.get_cell(b.get_player_location(Player_ID.PLAYER_2).get_y_coordinate(),
					   b.get_player_location(Player_ID.PLAYER_2).get_x_coordinate()).set_data(Cell_Status.P2);
		player_location[1] = new My_Coordinate_Pair(b.get_player_location(Player_ID.PLAYER_2).get_y_coordinate(),
													b.get_player_location(Player_ID.PLAYER_2).get_x_coordinate());
		board.get_cell(b.get_player_location(Player_ID.PLAYER_3).get_y_coordinate(), 
					   b.get_player_location(Player_ID.PLAYER_3).get_x_coordinate()).set_data(Cell_Status.P3);
		player_location[2] = new My_Coordinate_Pair(b.get_player_location(Player_ID.PLAYER_3).get_y_coordinate(), 
													b.get_player_location(Player_ID.PLAYER_3).get_x_coordinate());
		board.get_cell(b.get_player_location(Player_ID.PLAYER_4).get_y_coordinate(), 
					   b.get_player_location(Player_ID.PLAYER_4).get_x_coordinate()).set_data(Cell_Status.P4);
		player_location[3] = new My_Coordinate_Pair(b.get_player_location(Player_ID.PLAYER_4).get_y_coordinate(), 
													b.get_player_location(Player_ID.PLAYER_4).get_x_coordinate());
	
		//** FROM BOARD XXX
		// Create copy the cache
		cached_path = new Path[4];
		cache_hit = 0;
		cache_miss = 0;

		//** FROM BOARD XXX
		horizontal_wall_placement_locations = new HashMap<My_Coordinate_Pair, Boolean>();
		vertical_wall_placement_locations = new HashMap<My_Coordinate_Pair, Boolean>();
		HashMap<Coordinate_Pair<Integer, Integer>, Boolean> borig_horizontal = b.getHorizontal_wall_placement_locations();
		HashMap<Coordinate_Pair<Integer, Integer>, Boolean> borig_vertical = b.getVertical_wall_placement_locations();
		for(Coordinate_Pair<Integer, Integer> key : borig_horizontal.keySet()){
			int row = key.get_y_coordinate();
			int col = key.get_x_coordinate();
			board.get_cell(row, col).get_neighbors().remove(board.get_cell(row-1, col));
			board.get_cell(row-1, col).get_neighbors().remove(board.get_cell(row, col));
			board.get_cell(row, col+1).get_neighbors().remove(board.get_cell(row-1, col+1));
			board.get_cell(row-1, col+1).get_neighbors().remove(board.get_cell(row, col+1));

			horizontal_wall_placement_locations.put(new My_Coordinate_Pair(row, col), true);
		}
		//** FROM BOARD XXX
		for(Coordinate_Pair<Integer, Integer> key : borig_vertical.keySet()){
			int row = key.get_y_coordinate();
			int col = key.get_x_coordinate();
	
			board.get_cell(row, col).get_neighbors().remove(board.get_cell(row, col-1));
			board.get_cell(row, col-1).get_neighbors().remove(board.get_cell(row, col));
			board.get_cell(row+1, col).get_neighbors().remove(board.get_cell(row+1, col-1));
			board.get_cell(row+1, col-1).get_neighbors().remove(board.get_cell(row+1, col));

			vertical_wall_placement_locations.put(new My_Coordinate_Pair(row, col), true);
		}
	}
	
	/**
	 * Assumes there is a pawn on the source location. Can that pawn move from its location 
	 * to the destination location in one move.
	 *  
	 * @param from_row
	 * @param from_col
	 * @param to_row
	 * @param to_col
	 * @return
	 */
	public boolean can_move_to(int from_row, int from_col, int to_row, int to_col){
		//cannot move outside bounds of board
		if(from_row < 0 || to_row < 0 || from_col < 0 || to_col < 0 ||
				from_row >= BOARD_SIZE || to_row >= BOARD_SIZE || 
				from_col >= BOARD_SIZE || to_col >= BOARD_SIZE){
			return false;
		}
		
		//cannot move to an occupied cell, no matter where it is
		if(board.get_cell(to_row, to_col).get_data() != Cell_Status.FREE){
			return false;
		}
		
		//is the move legal? is the destination one of 4 neighbors
		if(board.get_cell(from_row, from_col).get_neighbors().contains(board.get_cell(to_row, to_col))){
			return true;
		}
		
		//a player can jump over a player that is blocking its path
		//jump up
		if(to_row == from_row - 2 && to_col == from_col){
			//player must be directly above
			if(!(board.get_cell(from_row - 1, from_col).get_data() == Cell_Status.FREE)){
				//and a wall must not be blocking the jump
				if(board.are_cells_neighbors(from_row, from_col, from_row-1, from_col) && can_move_to(from_row-1, from_col, from_row-2, from_col)){
					return true;
				}
			}
		}
		//jump down
		else if(to_row == from_row + 2 && to_col == from_col){
			//player must be directly below
			if(!(board.get_cell(from_row + 1, from_col).get_data() == Cell_Status.FREE)){
				//and a wall must not be blocking the jump
				if(board.are_cells_neighbors(from_row, from_col, from_row+1, from_col) && can_move_to(from_row+1, from_col, from_row+2, from_col)){
					return true;
				}
			}
		}
		//jump left
		else if(to_row == from_row && to_col == from_col-2){
			//player must be directly below
			if(!(board.get_cell(from_row, from_col - 1).get_data() == Cell_Status.FREE)){
				//and a wall must not be blocking the jump
				if(board.are_cells_neighbors(from_row, from_col, from_row, from_col-1) && can_move_to(from_row, from_col-1, from_row, from_col-2)){
					return true;
				}
			}
		}
		//jump right
		else if(to_row == from_row && to_col == from_col+2){
			//player must be directly below
			if(!(board.get_cell(from_row, from_col + 1).get_data() == Cell_Status.FREE)){
				//and a wall must not be blocking the jump
				if(board.are_cells_neighbors(from_row, from_col, from_row, from_col+1) && can_move_to(from_row, from_col+1, from_row, from_col+2)){
					return true;
				}
			}
		}
		
		//a player can side-step another player if a wall is blocking a straight jump
		//upper left 
		if(to_row == from_row - 1 && to_col == from_col - 1){
			//player must be directly above or left of from location AND
			//there must not be a wall between jumped pawn and the destination
			if(!(board.get_cell(from_row - 1, from_col).get_data() == Cell_Status.FREE) && 
					can_move_to(from_row-1, from_col, to_row, to_col) && board.are_cells_neighbors(from_row, from_col, from_row-1, from_col)){
				//and straight jump must not be possible
				if(!(can_move_to(from_row-1, from_col, from_row-2, from_col))){
					return true;
				}
			}
			else if(!(board.get_cell(from_row, from_col-1).get_data() == Cell_Status.FREE) &&
					can_move_to(from_row, from_col-1, to_row, to_col) && board.are_cells_neighbors(from_row, from_col, from_row, from_col-1)){
				//and straight jump must not be possible
				if(!(can_move_to(from_row, from_col-1, from_row, from_col-2))){
					return true;
				}
			}
		}
		//upper right
		else if(to_row == from_row - 1 && to_col == from_col + 1){
			//player must be directly above or right of from location
			if(!(board.get_cell(from_row - 1, from_col).get_data() == Cell_Status.FREE) &&
					can_move_to(from_row-1, from_col, to_row, to_col) && board.are_cells_neighbors(from_row, from_col, from_row-1, from_col)){
				//and straight jump must not be possible
				if(!(can_move_to(from_row-1, from_col, from_row-2, from_col))){
					return true;
				}
			}
			else if(!(board.get_cell(from_row, from_col+1).get_data() == Cell_Status.FREE) && 
					can_move_to(from_row, from_col+1, to_row, to_col) && board.are_cells_neighbors(from_row, from_col, from_row, from_col+1)){
				//and straight jump must not be possible
				if(!(can_move_to(from_row, from_col+1, from_row, from_col+2))){
					return true;
				}
			}
		}
		//lower left 
		else if(to_row == from_row + 1 && to_col == from_col - 1){
			//player must be directly below or left of from location
			if(!(board.get_cell(from_row + 1, from_col).get_data() == Cell_Status.FREE) && 
					can_move_to(from_row+1, from_col, to_row, to_col) && board.are_cells_neighbors(from_row, from_col, from_row+1, from_col)){
				//and straight jump must not be possible
				if(!(can_move_to(from_row+1, from_col, from_row+2, from_col))){
					return true;
				}
			}
			else if(!(board.get_cell(from_row, from_col-1).get_data() == Cell_Status.FREE) &&
					can_move_to(from_row, from_col-1, to_row, to_col) && board.are_cells_neighbors(from_row, from_col, from_row, from_col-1)){
				//and straight jump must not be possible
				if(!(can_move_to(from_row, from_col-1, from_row, from_col-2))){
					return true;
				}
			}
		}
		//lower right
		else if(to_row == from_row + 1 && to_col == from_col + 1){
			//player must be directly below or right of from location
			if(!(board.get_cell(from_row + 1, from_col).get_data() == Cell_Status.FREE) && 
					can_move_to(from_row+1, from_col, to_row, to_col) && board.are_cells_neighbors(from_row, from_col, from_row+1, from_col)){
				//and straight jump must not be possible
				if(!(can_move_to(from_row+1, from_col, from_row+2, from_col))){
					return true;
				}
			}
			else if(!(board.get_cell(from_row, from_col+1).get_data() == Cell_Status.FREE) && 
			can_move_to(from_row, from_col+1, to_row, to_col) && board.are_cells_neighbors(from_row, from_col, from_row, from_col+1)){
				//and straight jump must not be possible
				if(!(can_move_to(from_row, from_col+1, from_row, from_col+2))){
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * A pair of accessors for the wall locations maps.
	 */
	public HashMap<My_Coordinate_Pair, Boolean> getHorizontal_wall_placement_locations() {
		return horizontal_wall_placement_locations;
	}

	public HashMap<My_Coordinate_Pair, Boolean> getVertical_wall_placement_locations() {
		return vertical_wall_placement_locations;
	}

	private void remove_players() {
		board.get_cell(player_location[0].get_y_coordinate(), player_location[0].get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(player_location[1].get_y_coordinate(), player_location[1].get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(player_location[2].get_y_coordinate(), player_location[2].get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(player_location[3].get_y_coordinate(), player_location[3].get_x_coordinate()).set_data(Cell_Status.FREE);
	}

	private void replace_players() {
		board.get_cell(player_location[0].get_y_coordinate(), player_location[0].get_x_coordinate()).set_data(Cell_Status.P1);
		board.get_cell(player_location[1].get_y_coordinate(), player_location[1].get_x_coordinate()).set_data(Cell_Status.P2);
		board.get_cell(player_location[2].get_y_coordinate(), player_location[2].get_x_coordinate()).set_data(Cell_Status.P3);
		board.get_cell(player_location[3].get_y_coordinate(), player_location[3].get_x_coordinate()).set_data(Cell_Status.P4);
	}

// Copied a bit from http://code.google.com/p/a-star/source/browse/trunk/java/AStar.java?r=8
	private class Path implements Comparable{
		// Path for A* search

		public My_Coordinate_Pair point;
		public int f;
		public int g;
		public Path parent;

		public Path(){
			parent = null;
			point = null;
			g = f = 0;
		}

		public Path(My_Coordinate_Pair p, int m_g, int m_f){
			parent = null;
			point = p;
			g = m_g;
			f = m_f;
		}

		// Abuse of copy constructor
		public Path(Path p) {
			this();
			parent = p;
			g = p.g;
			f = p.f;
		}

		// return less than 0 if this object is smaller than o
		// greater than 0 if this object is bigger
		public int compareTo( Object o){ 
			Path p = (Path) o;
			return (int) (f - p.f);
		}
	}

	// Get moves that involve just one step, (i.e. no jumps
	// Assume this position is a valid one
	public My_Coordinate_Pair[] one_moves_from( My_Coordinate_Pair p ) {

		Cell<Cell_Status> n_pair; // pulling out neighbors.get call give 6.6% increase to evals/sec
		Vector<Cell<Cell_Status>> neighbors = board.get_cell(p.row(), p.col()).get_neighbors();
		My_Coordinate_Pair res[] = new My_Coordinate_Pair[neighbors.size()];
		for( int i = 0 ; i < neighbors.size(); i++ ) {
			n_pair = neighbors.get(i);
			res[i] = new My_Coordinate_Pair( n_pair.get_row(), n_pair.get_col() );
		}
		return res;
	}

	public boolean is_valid_path( Path path ) {
		Path p = path;
		Cell<Cell_Status> n_pair; // pulling out neighbors.get call give theoritcal incresse in perfomacne
		// While we aren't at the end of the path
		while ( p.parent != null ) {
			// path is blocked
			Vector<Cell<Cell_Status>> neighbors = board.get_cell(p.point.row(), p.point.col()).get_neighbors();
			boolean found_parent = false;
			for( int i = 0 ; i < neighbors.size(); i++ ) {
				n_pair = neighbors.get(i);
				if( n_pair.get_row() == p.parent.point.row() && n_pair.get_col() == p.parent.point.col() )
				{
					found_parent = true;
					break;
				}
			} // if we didn't find the parent we have failed the path is invalid
			if ( ! found_parent ){
				return false;
			}
			// go up path
			p = p.parent;
		}
		// Got trough whole path
		return true;
	}

	private boolean path_exists(Player_ID player, int target, boolean row){
		// Check cached path first
		if (cached_path[player.ordinal() ] != null ) {
			if( is_valid_path( cached_path[player.ordinal() ] ) )
			{
				cache_hit ++;
				return true;
			}
		}
		cache_miss ++;

		Path p = a_star( player_location[player.ordinal()], target, row);
		if ( p != null ) { // good path
			if (cached_path[player.ordinal() ] == null) { // only update if bad path;
				cached_path[player.ordinal() ] = p;
			}
			return true;
		} else 
			return false;
	}


	/**
	 * Does a path exist from the provided board location to the specified row.
	 * 
	 * @param from_row
	 * @param from_col
	 * @param to_row
	 * @return
	 */
	private boolean path_exists_to_row(Player_ID player, int to_row){
		return path_exists( player, to_row, true );
	}

	/**
	 * Does a path exist from the provided board location to the specified column.
	 * 
	 * @param from_row
	 * @param from_col
	 * @param to_col
	 * @return
	 */
	private boolean path_exists_to_column(Player_ID player, int to_col){
		return path_exists( player, to_col, false );
	}


	/**
	* Do and a star seach and return a path.  The starting point is start.
	* target is the target row or column row is if it is a row or if it is a column
	*/
	private Path a_star(My_Coordinate_Pair start, int target, boolean row){
		// blatentlly copied form wikipedia a star

		HashSet<My_Coordinate_Pair> closed = new HashSet<My_Coordinate_Pair>( ); // half the boardish
		PriorityQueue<Path> open = new PriorityQueue<Path>( );

		int h_score = 0;
		if ( row )
			h_score = Math.abs ( start.row() - target );
		else
			h_score = Math.abs ( start.col() - target );
		// h_score = f_score for first root
		Path root = new Path( start, 0 , h_score );

		open.add(root );

		while(!open.isEmpty()){
			// next = x
			Path next = open.poll();
			// if next is goal i.e. row/col match up
			if ( row && next.point.row() - target == 0 || 
				!row && next.point.col() - target == 0 ){
				return next;
			}

			// check that the node hasn't already been expanded
			if ( closed.contains( next.point ) ) {
				continue;
			}
			// Add point to closed so we know not to look at it again
			closed.add( next.point );
			// For each child i
			My_Coordinate_Pair[] child = one_moves_from( next.point );
			for(int i = 0; i < child.length; i++  ) {
				if ( closed.contains( child[i]) )
					continue;
				// each node is one away from parent
				int g_score = next.g + 1;

				// Cacluate heurstic
				if ( row )
					h_score = Math.abs ( child[i].row() - target );
				else
					h_score = Math.abs ( child[i].col() - target );

				Path p = new Path ( child[i] , g_score , g_score + h_score );
				p.parent = next ;

				// we found the the goal we can return now because
				// all path costs are the same
				// 4% increase in speed
				if (h_score == 0 )
					return p;
				open.add ( p );  // the same node can go in more than once
			}
		}
		return null;
	} // end a star

	
	/**
	 * Computes the shortest path to the goal on the given board for player p
	 * Returns the shortest path distance.
	 */
	public int shortest_path(Player_ID p){
		int target = 0;
		boolean row = true;
		
		switch(p){
			// player 1 trying to reach row BOARD_SIZE -1
			case PLAYER_1:
				row = true;
				target = BOARD_SIZE - 1;
				break;
			// player 2 trying to reach column 0	
			case PLAYER_2:
				row = false;
				target = 0;
				break;
			// player 3 trying to reach row 0
			case PLAYER_3:
				row = true;
				target = 0;
				break;
			// player 4 trying to reach column BOARD_SIZE - 1
			case PLAYER_4:
				row = false;
				target = BOARD_SIZE - 1;
				break;
		}
		Path path = a_star( player_location[p.ordinal()], target, row );
		// the f value for the goal node should be 100% accurate
		return path.f;
	}
	
	/**
	 * can a vertical/horizontal wall be placed starting at the giving grid location?
	 * for horizontal walls the second wall component will fall to the right (i.e., col+1)
	 * for vertical walls the second wall component will fall below (i.e., row + 1)
	 * 
	 * Does *not* check if the walls are available to any player, only if the location
	 * is a valid location.
	 * 
	 * @return true if the wall placement is valid, false otherwise
	 */
	public boolean can_place_wall(int row, int col, boolean place_horizontally){
		//does a horizontal wall land off the board?
		if(place_horizontally && (row >= BOARD_SIZE || col >= BOARD_SIZE-1 || row <= 0 || col < 0))
			return false;

		//does a vertical wall land off the board?
		if(!place_horizontally && (row >= BOARD_SIZE-1 || col >= BOARD_SIZE || row < 0 || col <= 0))
			return false;
		
		//cannot place a wall overlapping an existing wall
		if(place_horizontally){
			if(!(board.get_cell(row, col).get_neighbors().contains(board.get_cell(row-1, col)) && 
					board.get_cell(row, col+1).get_neighbors().contains(board.get_cell(row-1, col+1)))){
				return false;	
			}
		}
		else{
			if(!(board.get_cell(row, col).get_neighbors().contains(board.get_cell(row, col-1)) && 
					board.get_cell(row+1, col).get_neighbors().contains(board.get_cell(row+1, col-1)))){
				return false;	
			}
		}
		
		//will the wall intersect another wall
		if(place_horizontally){
			if(vertical_wall_placement_locations.get(new My_Coordinate_Pair(row - 1, col+1)) != null){
				//check for a vertical intersecting wall
				return false;
			}
		}
		else{
			if(horizontal_wall_placement_locations.get(new My_Coordinate_Pair(row + 1, col-1)) != null){
				//check for a horizontal intersecting wall
				return false;
			}
		}
		
		//walls must be within 1 space of another wall or 2 spaces of a pawn
		boolean is_wall_too_far_from_walls = true;
		for(My_Coordinate_Pair wall : horizontal_wall_placement_locations.keySet()){
			if(distance_between_walls(row, col, place_horizontally, wall.get_y_coordinate(), wall.get_x_coordinate(), true) <= 1){
				is_wall_too_far_from_walls = false;
			}
		}
		for(My_Coordinate_Pair wall : vertical_wall_placement_locations.keySet()){
			if(distance_between_walls(row, col, place_horizontally, wall.get_y_coordinate(), wall.get_x_coordinate(), false) <= 1){
				is_wall_too_far_from_walls = false;
			}
		}
		
		boolean is_wall_too_far_from_pawns = true;
		if(distance_between_players_and_wall(row, col, place_horizontally) <= 1){
			is_wall_too_far_from_pawns = false;
		}
		
		if(is_wall_too_far_from_walls && is_wall_too_far_from_pawns){
			return false;
		}
		
		//temporarily place wall on board
		if(place_horizontally){
			board.get_cell(row, col).get_neighbors().remove(board.get_cell(row-1, col));
			board.get_cell(row-1, col).get_neighbors().remove(board.get_cell(row, col));
			board.get_cell(row, col+1).get_neighbors().remove(board.get_cell(row-1, col+1));
			board.get_cell(row-1, col+1).get_neighbors().remove(board.get_cell(row, col+1));
		}
		else{
			board.get_cell(row, col).get_neighbors().remove(board.get_cell(row, col-1));
			board.get_cell(row, col-1).get_neighbors().remove(board.get_cell(row, col));
			board.get_cell(row+1, col).get_neighbors().remove(board.get_cell(row+1, col-1));
			board.get_cell(row+1, col-1).get_neighbors().remove(board.get_cell(row+1, col));
		}
		
		//cannot place a wall that leaves a player with 0 paths to its goal
		boolean ret = false;
		if(path_exists_to_row(Player_ID.PLAYER_1, BOARD_SIZE-1) &&
				path_exists_to_column(Player_ID.PLAYER_2, 0) &&
				path_exists_to_row(Player_ID.PLAYER_3, 0) &&
				path_exists_to_column(Player_ID.PLAYER_4, BOARD_SIZE-1)){
			ret = true; //all players still have AT LEAST one path with the new wall in place
		}
		
		//reset the temporary wall placement
		if(place_horizontally){
			board.get_cell(row, col).get_neighbors().add(board.get_cell(row-1, col));
			board.get_cell(row-1, col).get_neighbors().add(board.get_cell(row, col));
			board.get_cell(row, col+1).get_neighbors().add(board.get_cell(row-1, col+1));
			board.get_cell(row-1, col+1).get_neighbors().add(board.get_cell(row, col+1));
		}
		else{
			board.get_cell(row, col).get_neighbors().add(board.get_cell(row, col-1));
			board.get_cell(row, col-1).get_neighbors().add(board.get_cell(row, col));
			board.get_cell(row+1, col).get_neighbors().add(board.get_cell(row+1, col-1));
			board.get_cell(row+1, col-1).get_neighbors().add(board.get_cell(row+1, col));
		}
		return ret;
	}
	
	/**
	 * Compute the minimum manhattan distance between the specified wall and the players' pawns.
	 * 
	 * @param wall_row
	 * @param wall_col
	 * @param is_horizontal
	 * @return
	 */
	private int distance_between_players_and_wall(int wall_row, int wall_col, boolean is_horizontal){
		int wall_end_row, wall_end_col;
		if(is_horizontal){
			wall_end_row = wall_row;
			wall_end_col = wall_col+2;
		}
		else{
			wall_end_row = wall_row+2;
			wall_end_col = wall_col;
		}
		
		
		int[] point_distances = new int[8];
		Integer curr_min = Integer.MAX_VALUE; 
		My_Coordinate_Pair[] locations = new My_Coordinate_Pair[]{player_location[0], player_location[1], player_location[2], player_location[3]};
		for(int i= 0; i < locations.length; i++){
			point_distances[0] = Math.abs(wall_row - ((My_Coordinate_Pair)locations[i]).get_y_coordinate()) + Math.abs(wall_col - ((My_Coordinate_Pair)locations[i]).get_x_coordinate());
			point_distances[1] = Math.abs(wall_row - ((My_Coordinate_Pair)locations[i]).get_y_coordinate()) + Math.abs(wall_col - (((My_Coordinate_Pair)locations[i]).get_x_coordinate()+1));
			point_distances[2] = Math.abs(wall_row - (((My_Coordinate_Pair)locations[i]).get_y_coordinate() +1)) + Math.abs(wall_col - (((My_Coordinate_Pair)locations[i]).get_x_coordinate()+1));
			point_distances[3] = Math.abs(wall_row - (((My_Coordinate_Pair)locations[i]).get_y_coordinate()+1)) + Math.abs(wall_col - ((My_Coordinate_Pair)locations[i]).get_x_coordinate());
			point_distances[4] = Math.abs(wall_end_row - ((My_Coordinate_Pair)locations[i]).get_y_coordinate()) + Math.abs(wall_end_col - ((My_Coordinate_Pair)locations[i]).get_x_coordinate());
			point_distances[5] = Math.abs(wall_end_row - ((My_Coordinate_Pair)locations[i]).get_y_coordinate()) + Math.abs(wall_end_col - (((My_Coordinate_Pair)locations[i]).get_x_coordinate()+1));
			point_distances[6] = Math.abs(wall_end_row - (((My_Coordinate_Pair)locations[i]).get_y_coordinate() +1)) + Math.abs(wall_end_col - (((My_Coordinate_Pair)locations[i]).get_x_coordinate()+1));
			point_distances[7] = Math.abs(wall_end_row - (((My_Coordinate_Pair)locations[i]).get_y_coordinate()+1)) + Math.abs(wall_end_col - ((My_Coordinate_Pair)locations[i]).get_x_coordinate());

			if(minimum_value(point_distances) < curr_min){
				curr_min = minimum_value(point_distances);
			}
		}
	
		return curr_min;
	}
	
	/**
	 * Compute the manhattan distance between the two specified walls.
	 * 
	 * @param wall1_row
	 * @param wall1_col
	 * @param wall1_is_horizontal
	 * @param wall2_row
	 * @param wall2_col
	 * @param wall2_is_horizontal
	 * @return
	 */
	private int distance_between_walls(int wall1_row, int wall1_col, boolean wall1_is_horizontal,
			int wall2_row, int wall2_col, boolean wall2_is_horizontal){
		int wall1_end_row, wall1_end_col, wall2_end_row, wall2_end_col, wall1_mid_row, wall1_mid_col, wall2_mid_row, wall2_mid_col;
		if(wall1_is_horizontal){
			wall1_end_row = wall1_row;
			wall1_mid_row = wall1_row;
			wall1_mid_col = wall1_col+1;
			wall1_end_col = wall1_col+2;
		}
		else{
			wall1_mid_row = wall1_row+1;
			wall1_end_row = wall1_row+2;
			wall1_end_col = wall1_col;
			wall1_mid_col = wall1_col;
		}
		if(wall2_is_horizontal){
			wall2_end_row = wall2_row;
			wall2_mid_row = wall2_row;
			wall2_mid_col = wall2_col+1;
			wall2_end_col = wall2_col+2;
		}
		else{
			wall2_mid_row = wall2_row+1;
			wall2_end_row = wall2_row+2;
			wall2_mid_col = wall2_col;
			wall2_end_col = wall2_col;
		}
	
		int[] point_distances = new int[8];
		point_distances[0] = Math.abs(wall1_row - wall2_row) + Math.abs(wall1_col - wall2_col);
		point_distances[1] = Math.abs(wall1_row - wall2_end_row) + Math.abs(wall1_col - wall2_end_col);
		point_distances[2] = Math.abs(wall1_end_row - wall2_row) + Math.abs(wall1_end_col - wall2_col);
		point_distances[3] = Math.abs(wall1_end_row - wall2_end_row) + Math.abs(wall1_end_col - wall2_end_col);
		point_distances[4] = Math.abs(wall1_row - wall2_mid_row) + Math.abs(wall1_col - wall2_mid_col);
		point_distances[5] = Math.abs(wall1_end_row - wall2_mid_row) + Math.abs(wall1_end_col - wall2_mid_col);
		point_distances[6] = Math.abs(wall1_mid_row - wall2_row) + Math.abs(wall1_mid_col - wall2_col);
		point_distances[7] = Math.abs(wall1_mid_row - wall2_end_row) + Math.abs(wall1_mid_col - wall2_end_col);
		return minimum_value(point_distances);
	}
	
	/**
	 * Finds the minimum value in an array of integers.
	 * 
	 * @param a
	 * @return min value
	 */
	private int minimum_value(int[] a){
		int min_idx = 0;
		for(int i = 0; i < a.length; i++){
			if(a[i] < a[min_idx]){
				min_idx = i;
			}
		}
		
		return a[min_idx];
	}
	
	/**
	 * Place a wall, starting at the specified location, and one space to the right or bottom
	 * for a horizontal or vertical wall, respectively.
	 *   
	 * @param row
	 * @param col
	 * @param place_horizontally
	 * @return
	 */
	private boolean place_wall(int row, int col, boolean place_horizontally){
		if(can_place_wall(row, col, place_horizontally)){	
			if(place_horizontally){
				board.get_cell(row, col).get_neighbors().remove(board.get_cell(row-1, col));
				board.get_cell(row-1, col).get_neighbors().remove(board.get_cell(row, col));
				board.get_cell(row, col+1).get_neighbors().remove(board.get_cell(row-1, col+1));
				board.get_cell(row-1, col+1).get_neighbors().remove(board.get_cell(row, col+1));
				horizontal_wall_placement_locations.put(new My_Coordinate_Pair(row, col), true);
			}
			else{
				board.get_cell(row, col).get_neighbors().remove(board.get_cell(row, col-1));
				board.get_cell(row, col-1).get_neighbors().remove(board.get_cell(row, col));
				board.get_cell(row+1, col).get_neighbors().remove(board.get_cell(row+1, col-1));
				board.get_cell(row+1, col-1).get_neighbors().remove(board.get_cell(row+1, col));
				vertical_wall_placement_locations.put(new My_Coordinate_Pair(row, col), true);
			}
			
			return true; //the wall could was successfully placed in the desired location
		}
		
		return false; //the wall could not be placed in the desired location
	}
	
	/**
	 * My_Move the specified player from the given location to the new location. 
	 * 
	 * @param from_row
	 * @param from_col
	 * @param to_row
	 * @param to_col
	 * @param player
	 * @return true if the move is possible, false otherwise
	 */
	private boolean move(int from_row, int from_col, int to_row, int to_col, Player_ID player){
		if(can_move_to(from_row, from_col, to_row, to_col)){
			// invalidate the cached path for this player
			cached_path[player.ordinal()] = null;
			switch (player) {
			case PLAYER_1:
				player_location[0] = new My_Coordinate_Pair(to_row, to_col);
				board.get_cell(to_row, to_col).set_data(Cell_Status.P1);
				break;
			case PLAYER_2:
				player_location[1] = new My_Coordinate_Pair(to_row, to_col);
				board.get_cell(to_row, to_col).set_data(Cell_Status.P2);
				break;
			case PLAYER_3:
				player_location[2] = new My_Coordinate_Pair(to_row, to_col);
				board.get_cell(to_row, to_col).set_data(Cell_Status.P3);
				break;
			case PLAYER_4:
				player_location[3] = new My_Coordinate_Pair(to_row, to_col);
				board.get_cell(to_row, to_col).set_data(Cell_Status.P4);
				break;
			default:
				break;
			}
			board.get_cell(from_row, from_col).set_data(Cell_Status.FREE);
			return true;
		}
		
		return false;
	}	
	
	/**
	 * Convert the board to an ascii representation
	 */
	public String toString(){
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < 2*BOARD_SIZE +1; i++){
			buf.append("*");
		}
		buf.append("\n");
		for(int i = 0; i < BOARD_SIZE; i++){
			buf.append("*");
			for(int j = 0; j < BOARD_SIZE; j++){
				if(board.get_cell(i, j).get_data() == Cell_Status.P1){
					buf.append("1");
				}
				else if(board.get_cell(i, j).get_data() == Cell_Status.P2){
					buf.append("2");
				}
				else if(board.get_cell(i, j).get_data() == Cell_Status.P3){
					buf.append("3");
				}
				else if(board.get_cell(i, j).get_data() == Cell_Status.P4){
					buf.append("4");
				}
				else{
					buf.append(" ");
				}
				
				//right neighbor wall check
				if(j < BOARD_SIZE-1){
					if(board.get_cell(i, j).get_neighbors().contains(board.get_cell(i, j+1))){
						buf.append("|");
					}
					else{
						buf.append("#");
					}
				}
			}
			buf.append("*\n");
			
			if(i < BOARD_SIZE - 1 && i >= 0){
				int board_j = 0;
				for(int j = 0; j < 2*BOARD_SIZE +1; j++){
					if(j == 0 || j == 2*BOARD_SIZE)
						buf.append("*");
					else if(j%2 == 0){
						if(  j <= 2*(BOARD_SIZE-1)
						  && !board.get_cell(i, j/2-1).get_neighbors().contains(board.get_cell(i, j/2))
						  || !board.get_cell(i+1, j/2-1).get_neighbors().contains(board.get_cell(i+1, j/2))){
							buf.append("#");
						}
						else
							buf.append("-");
					}
					else{
						if(board.get_cell(i+1, board_j).get_neighbors().contains(board.get_cell(i, board_j))){
							buf.append("-");
						}
						else {
							buf.setCharAt(buf.length()-1, '#');
							buf.append("##");
							j++;
						}
						board_j++;
					}
				}
				buf.append("\n");
			}
		}
		
		for(int i = 0; i < 2*BOARD_SIZE +1; i++){
			buf.append("*");
		}
		buf.append("\n");
		return buf.toString();
	}
	
	/**
	 * Modifies the board to represent the effects of the specified move.
	 * 
	 * @param m
	 * @return true if the move is applied successfully, false if the move was invalid and not applied
	 */
	public boolean apply_move(My_Move m){
		if(m.getMove_type() == My_Move.MOVE_TYPE.MOVE_PAWN){
			switch(m.getPlayer_making_move()){
				case PLAYER_1:
					return move(player_location[0].get_y_coordinate(), player_location[0].get_x_coordinate(), 
							    m.getTarget_cell_coordinates().get_y_coordinate(), 
							    m.getTarget_cell_coordinates().get_x_coordinate(), Player_ID.PLAYER_1);
				
				case PLAYER_2:
					return move(player_location[1].get_y_coordinate(), player_location[1].get_x_coordinate(), 
							    m.getTarget_cell_coordinates().get_y_coordinate(), 
							    m.getTarget_cell_coordinates().get_x_coordinate(), Player_ID.PLAYER_2);
				
				case PLAYER_3:
					return move(player_location[2].get_y_coordinate(), player_location[2].get_x_coordinate(), 
							    m.getTarget_cell_coordinates().get_y_coordinate(), 
							    m.getTarget_cell_coordinates().get_x_coordinate(), Player_ID.PLAYER_3);
				
				case PLAYER_4:
					return move(player_location[3].get_y_coordinate(), player_location[3].get_x_coordinate(), 
							    m.getTarget_cell_coordinates().get_y_coordinate(), 
							    m.getTarget_cell_coordinates().get_x_coordinate(), Player_ID.PLAYER_4);
				
				default: // shouldn't reach this case EVER
					return false;
			}
			
		}
		else{ //placing a wall
			if(place_wall(m.getTarget_cell_coordinates().get_y_coordinate(), m.getTarget_cell_coordinates().get_x_coordinate(), m.getIs_horizontal())){
				switch(m.getPlayer_making_move()){
					case PLAYER_1:
						walls[0]--;
						break;
						
					case PLAYER_2:
						walls[1]--;
						break;
						
					case PLAYER_3:
						walls[2]--;
						break;
						
					case PLAYER_4:
						walls[3]--;
						break;
				}
				
				return true;
			}
			
			return false;
		}
	}
	
	/**
	 * Determine the set of possible moves available to the player with the specified id from
	 * this state of the game. 
	 * 
	 * @param player_id
	 * @return
	 */
	public Vector<My_Move> get_possible_moves(Player_ID player_id){
		Vector<My_Move> possible_moves = new Vector<My_Move>();
		
		//moving options
		int player_row_location;
		int player_col_location;
		int walls_available = 0;
		switch(player_id){
			case PLAYER_1:
				player_row_location = player_location[0].get_y_coordinate();
				player_col_location = player_location[0].get_x_coordinate();
				walls_available = walls[0];
				break;
			case PLAYER_2:
				player_row_location = player_location[1].get_y_coordinate();
				player_col_location = player_location[1].get_x_coordinate();
				walls_available = walls[1];
				break;
			case PLAYER_3:
				player_row_location = player_location[2].get_y_coordinate();
				player_col_location = player_location[2].get_x_coordinate();
				walls_available = walls[2];
				break;
			case PLAYER_4:
				player_row_location = player_location[3].get_y_coordinate();
				player_col_location = player_location[3].get_x_coordinate();
				walls_available = walls[3];
				break;
			default:
				//this should never happen
				player_col_location = 0;
				player_row_location = 0;
				break;
					
		}
		//up
		if(can_move_to(player_row_location, player_col_location, player_row_location-1, player_col_location)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location-1, player_col_location)));
		}
		//down
		if(can_move_to(player_row_location, player_col_location, player_row_location+1, player_col_location)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location+1, player_col_location)));
		}
		//left
		if(can_move_to(player_row_location, player_col_location, player_row_location, player_col_location-1)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location, player_col_location-1)));
		}
		//right
		if(can_move_to(player_row_location, player_col_location, player_row_location, player_col_location+1)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location, player_col_location+1)));
		}
		//up-left
		if(can_move_to(player_row_location, player_col_location, player_row_location-1, player_col_location-1)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location-1, player_col_location-1)));
		}
		//up-right
		if(can_move_to(player_row_location, player_col_location, player_row_location-1, player_col_location+1)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location-1, player_col_location+1)));
		}
		//down-right
		if(can_move_to(player_row_location, player_col_location, player_row_location+1, player_col_location+1)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location+1, player_col_location+1)));
		}
		//down-left
		if(can_move_to(player_row_location, player_col_location, player_row_location+1, player_col_location-1)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location+1, player_col_location-1)));
		}
		//jumping moves
		//up2
		if(can_move_to(player_row_location, player_col_location, player_row_location-2, player_col_location)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location-2, player_col_location)));
		}
		//down2
		if(can_move_to(player_row_location, player_col_location, player_row_location+2, player_col_location)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location+2, player_col_location)));
		}
		//left2
		if(can_move_to(player_row_location, player_col_location, player_row_location, player_col_location-2)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location, player_col_location-2)));
		}
		//right2
		if(can_move_to(player_row_location, player_col_location, player_row_location, player_col_location+2)){
			possible_moves.add(new My_Move(MOVE_TYPE.MOVE_PAWN, player_id, new My_Coordinate_Pair(player_row_location, player_col_location+2)));
		}
		
		//wall-placing options
		if(walls_available > 0){
			for(int i = 0; i <= BOARD_SIZE-1; i++){
				for(int j = 0; j <= BOARD_SIZE-1; j++){
					if(can_place_wall(i, j, true)){
						possible_moves.add(new My_Move(MOVE_TYPE.PLACE_WALL, player_id, new My_Coordinate_Pair(i, j), true));
					}
					if(can_place_wall(i, j, false)){
						possible_moves.add(new My_Move(MOVE_TYPE.PLACE_WALL, player_id, new My_Coordinate_Pair(i, j), false));
					}
				}
			}
		}
		
		return possible_moves;
	}
	
	/**
	 * Did a player reach their goal?
	 * 
	 * @return true if yes, false otherwise.
	 */
	public boolean is_game_over(){
		return player_location[0].get_y_coordinate() == BOARD_SIZE-1 ||  
			   player_location[1].get_x_coordinate() == 0 ||
			   player_location[2].get_y_coordinate() == 0 ||
			   player_location[3].get_x_coordinate() == BOARD_SIZE-1;
	}
	
	/**
	 * Determine the winner of the game. If no one is the winner, return the player in the
	 * lead (i.e., player closest to their goal). 
	 * 
	 * @return the id of the winner, if there is a tie, null is returned.
	 */
	public Player_ID compute_winner(){
		if(player_location[0].get_y_coordinate() == BOARD_SIZE-1)
			return Player_ID.PLAYER_1;
		else if(player_location[1].get_x_coordinate() == 0)
			return Player_ID.PLAYER_2;
		else if(player_location[2].get_y_coordinate() == 0)
			return Player_ID.PLAYER_3;
		else if(player_location[3].get_x_coordinate() == BOARD_SIZE-1)
			return Player_ID.PLAYER_4;
		else{
			int[] shortest_path_distances = new int[4];
			shortest_path_distances[0] = shortest_path(Player_ID.PLAYER_1);
			shortest_path_distances[1] = shortest_path(Player_ID.PLAYER_2);
			shortest_path_distances[2] = shortest_path(Player_ID.PLAYER_3);
			shortest_path_distances[3] = shortest_path(Player_ID.PLAYER_4);
			int min_idx = minimum_index(shortest_path_distances);
			
			switch(min_idx){
				case 0:
					return Player_ID.PLAYER_1;
				case 1:
					return Player_ID.PLAYER_2;
				case 2:
					return Player_ID.PLAYER_3;
				case 3:
					return Player_ID.PLAYER_4;
				default:
					return null;
			}
			
		}
	}
	
	/**
	 * Finds the minimum index in an array of integers.
	 * 
	 * @param a
	 * @return min index, -1 indicates tie between 2 or more indices
	 */
	private int minimum_index(int[] a){
		int min_idx = 0;
		boolean tie_max = false;
		for(int i = 0; i < a.length; i++){
			if(a[i] < a[min_idx]){
				min_idx = i;
				tie_max = false;
			}
			else if(a[i] == a[min_idx]){
				tie_max = true;
			}
		}
		
		if(tie_max)
			return -1;
		
		return min_idx;
	}
	
	public My_Coordinate_Pair get_player_location(Player_ID p){
		switch(p){
			case PLAYER_1:
				return player_location[0];
			case PLAYER_2:
				return player_location[1];
			case PLAYER_3:
				return player_location[2];
			case PLAYER_4:
				return player_location[3];
		}
		return null; //satisfies compiler, should not be reached
	}
	
	public int get_wall_count(Player_ID p){
		switch(p){
			case PLAYER_1:
				return walls[0];
			case PLAYER_2:
				return walls[1];
			case PLAYER_3:
				return walls[2];
			case PLAYER_4:
				return walls[3];
		}
		return 0;
	}
	
	/**
	 * Accessor for the underlying grid structure.
	 * 
	 * @return
	 */
	public RectangularGrid<Cell_Status> get_board(){
		return board;
	}
	
	/**
	 * A few tests for the board class. Nothing fancy or special here.
	 * 
	 * @param args - ignored CLA's
	 */
	public static void main(String[] args){
		My_Board b = new My_Board();
		
		System.out.println("Created the initial board: \n" + b);
		
		/*
		 * -------------------
| | | | | | | | | |
---------*-*---*-*-
| | | * * | | * | |
-------------------
| | | * * * * * | |
-*-*-*-*-----------
|3|4| | | *1* | | |
-*-*-----*-*-------
| | | | | | * | | |
-----*-*-----------
| | * * * | * |2| |
-*-*---------*-*---
| | * * * * | | | |
-----*-*-----------
| | | | | * | | | |
-------------------
| | | | | | | | | |
-------------------
		 */
				
		b.board.get_cell(b.player_location[2].get_y_coordinate(), b.player_location[2].get_x_coordinate()).set_data(Cell_Status.FREE);
		b.player_location[2] = new My_Coordinate_Pair(3, 0);
		b.board.get_cell(3, 0).set_data(Cell_Status.P3);
		
		b.board.get_cell(b.player_location[3].get_y_coordinate(), b.player_location[3].get_x_coordinate()).set_data(Cell_Status.FREE);
		b.player_location[3] = new My_Coordinate_Pair(3, 1);
		b.board.get_cell(3, 1).set_data(Cell_Status.P4);
		
		b.place_wall(7, 7, true);
		System.out.println("After placing a walls: \n" + b);
		
		
//		b.place_wall(3, 0, true);
//		b.place_wall(3, 2, true);
//		System.out.println(b.can_move_to(3, 0, 3, 2));
//
//		b.place_wall(4, 0, true);
//		System.out.println("After placing a walls: \n" + b);
		
//		b.place_wall(7, 3, true);
//		System.out.println("After placing a horizontal wall: \n" + b);
//		
//		b.place_wall(6, 3, false);
//		System.out.println("After placing a vertical wall: \n" + b);
//		
//		b.place_wall(4, 2, false);
//		System.out.println("After placing a vertical wall: \n" + b);
//		
//		b.place_wall(5, 2, true);
//		System.out.println("After placing a horizontal that intersects the last: \n" + b);
//		
//		b.place_wall(0, 0, false);
//		System.out.println("After placing an invalid wall: \n" + b);
//		
//		b.move(0, 4, 0, 5, Player_ID.PLAYER_1);
//		System.out.println("After moving P1 to the right: \n" + b);
//	
//		b.move(4, 0, 4, 1, Player_ID.PLAYER_4);
//		System.out.println("After moving P3 to the right: \n" + b);
//		
//		b.move(4, 0, 4, 1, Player_ID.PLAYER_4);
//		System.out.println("After trying to move P3 through a wall: \n" + b);
		
//		System.out.println("Making a copy of the board: \n" + new Board(b));
	}
	
}

/**
 * A generic class for maintaining a set of coordinates mapping into
 * a coordinate graph.
 *
 */
class My_Coordinate_Pair {
	private int y; // row
	private int x; // col
	
	public My_Coordinate_Pair(int f, int s){
		this.y = f;
		this.x = s;
	}
	
	public int row(){
		return y;
	}

	public int get_y_coordinate(){
		return y;
	}
	
	public int col(){
		return x;
	}

	public int get_x_coordinate(){
		return x;
	}
	
	public int hashCode() {
        //int hashFirst = y != null ? y.hashCode() : 0;
        //int hashSecond = x != null ? x.hashCode() : 0;

        return (x) + (20 * y);
    }
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		if(!(o instanceof My_Coordinate_Pair))
			return false;
		My_Coordinate_Pair other = (My_Coordinate_Pair) o;
		
		return this.y == other.y && this.x == other.x;
	}
}

/**
 * The move class is intended to express a move in the game of Quoridor.
 * This is an immutable class.
 * 
 *  
	 * A move can be of two types: MOVE_PAWN or PLACE_WALL. This constructor does not check for
	 * legality of the Move.
	 * 
	 *     - MOVE_PAWN - in this case the identified player wants to move from their current location
	 *       to the target_cell_coordinates
	 *
	 *     - PLACE_WALL - in this case the identified player intends to place a wall that will start 
	 *       at the upper left corner of the cell identified by target_cell_coordinates and continue
	 *       right, if horizontal, or down, if vertical.
	 *       
	 *       for example, if placing a vertical wall at (4,3), the wall will block the space between 
	 *       cells (4,3) and (4,2) as well as (5,3) and (5,2)
	 *       the board will look like this (* indicates wall): 
	 *        -------------------
	 *        | | | | |1| | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        |3| | * | | | | |4|
	 *        ------------------
	 *        | | | * | | | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        | | | | |2| | | | |
	 *        -------------------
	 *        
	 *        likewise placing a horizontal wall at (4,3) will result in a wall that wall will block the 
	 *        space between cells (4,3) and (3,3) as well as (4,4) and (5,4) 
	 *        
	 *        -------------------
	 *        | | | | |1| | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        -------*-*--------
	 *        |3| | | | | | | |4|
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        | | | | | | | | | |
	 *        ------------------
	 *        | | | | |2| | | | |
	 *        -------------------
     */
class My_Move {
	//an enum to distinguish between the two types of moves
	public enum MOVE_TYPE{PLACE_WALL, MOVE_PAWN};
	
	private MOVE_TYPE move_type;
	private Player_ID player_making_move;
	private My_Coordinate_Pair target_cell_coordinates;
	private Boolean is_horizontal;
	
	/**
	 * A constructor for move-pawn type moves
	 * 
	 * @param type
	 * @param player_id
	 * @param target
	 */
	public My_Move(MOVE_TYPE type, Player_ID player_id, My_Coordinate_Pair target_cell_coordinates){
		this.move_type = type;
		this.player_making_move = player_id;
		this.target_cell_coordinates = target_cell_coordinates;
		this.is_horizontal = null;
	}
	
	/**
	 * An alternate constructor for place-wall type moves
	 * 
	 * @param type
	 * @param player_id
	 * @param target_cell_coordinates
	 * @param is_horizontal
	 */
	public My_Move(MOVE_TYPE type, Player_ID player_id, My_Coordinate_Pair target_cell_coordinates, boolean is_horizontal){
		this(type, player_id, target_cell_coordinates);
		
		this.is_horizontal = is_horizontal;
	}

	/**
	 * turn this move into a move that we can return from get_move function
	 */
	public Move to_OldMove() {
		if (move_type == MOVE_TYPE.MOVE_PAWN) 
			return new Move( Move.MOVE_TYPE.MOVE_PAWN, this.player_making_move, new Coordinate_Pair<Integer, Integer> (  target_cell_coordinates.row(),  target_cell_coordinates.col() ) );
		else //(MOVE_TYPE == PLACE_WALL) 
			return new Move( Move.MOVE_TYPE.PLACE_WALL, this.player_making_move, new Coordinate_Pair<Integer, Integer> (  target_cell_coordinates.row(),  target_cell_coordinates.col() ), this.is_horizontal );
	}

	public MOVE_TYPE getMove_type() {
		return move_type;
	}

	public Player_ID getPlayer_making_move() {
		return player_making_move;
	}

	public My_Coordinate_Pair getTarget_cell_coordinates() {
		return target_cell_coordinates;
	}

	public Boolean getIs_horizontal() {
		return is_horizontal;
	}
	
	public String toString(){
		StringBuffer buf = new StringBuffer();
		buf.append("MOVE: ");
		
		if(move_type == MOVE_TYPE.PLACE_WALL){
			buf.append("place ");
			if(is_horizontal){
				buf.append("horizontal ");
			}
			else{
				buf.append("vertical ");
			}
			
			buf.append("wall at [");
			buf.append(target_cell_coordinates.get_y_coordinate());
			buf.append(", ");
			buf.append(target_cell_coordinates.get_x_coordinate());
			buf.append("]");
		}
		else{
			buf.append("move pawn to ");
			
			buf.append("[");
			buf.append(target_cell_coordinates.get_y_coordinate());
			buf.append(", ");
			buf.append(target_cell_coordinates.get_x_coordinate());
			buf.append("]");
		}
		
		return buf.toString();
	}
	
}
