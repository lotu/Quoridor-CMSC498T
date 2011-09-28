package game;
import game.Move.MOVE_TYPE;

import java.util.HashMap;
import java.util.Stack;
import java.util.Vector;

import players.Player_ID;

/**
 * This class contains all of the state information for a game of Quoridor. The methods for manipulating
 * this state, according to the rules of Quoridor, are also provided. 
 * 
 * @author bswilson
 *
 */
public class Board {
	private static final int BOARD_SIZE = 9;
	private static final int INITIAL_WALLS = 5;
	private RectangularGrid<Cell_Status> board;
	private Coordinate_Pair<Integer, Integer> p1_location;
	private Coordinate_Pair<Integer, Integer> p2_location;
	private Coordinate_Pair<Integer, Integer> p3_location;
	private Coordinate_Pair<Integer, Integer> p4_location;
	private HashMap<Coordinate_Pair<Integer, Integer>, Boolean> horizontal_wall_placement_locations;
	private HashMap<Coordinate_Pair<Integer, Integer>, Boolean> vertical_wall_placement_locations;
	private int[] walls;
	
	/**
	 * Construct the initial Quoridor board and player locations.
	 */
	public Board(){
		//initialize an empty board with all cell open
		Cell_Status[][] cells = new Cell_Status[BOARD_SIZE][BOARD_SIZE];
		for(int i = 0; i < BOARD_SIZE; i++){
			for(int j = 0; j < BOARD_SIZE; j++){
				cells[i][j] = Cell_Status.FREE;
			}
		}
		board = new RectangularGrid<Cell_Status>(BOARD_SIZE, BOARD_SIZE, cells);
		walls = new int[]{INITIAL_WALLS, INITIAL_WALLS, INITIAL_WALLS, INITIAL_WALLS};
		
		//put player pawns on the board in starting locations
		board.get_cell(0, BOARD_SIZE/2).set_data(Cell_Status.P1);
		p1_location = new Coordinate_Pair<Integer, Integer>(0, BOARD_SIZE/2);
		board.get_cell(BOARD_SIZE/2, BOARD_SIZE-1).set_data(Cell_Status.P2);
		p2_location = new Coordinate_Pair<Integer, Integer>(BOARD_SIZE/2, BOARD_SIZE - 1);
		board.get_cell(BOARD_SIZE-1, BOARD_SIZE/2).set_data(Cell_Status.P3);
		p3_location = new Coordinate_Pair<Integer, Integer>(BOARD_SIZE-1, BOARD_SIZE/2);
		board.get_cell(BOARD_SIZE/2, 0).set_data(Cell_Status.P4);
		p4_location = new Coordinate_Pair<Integer, Integer>(BOARD_SIZE/2, 0);
		
		horizontal_wall_placement_locations = new HashMap<Coordinate_Pair<Integer,Integer>, Boolean>();
		vertical_wall_placement_locations = new HashMap<Coordinate_Pair<Integer,Integer>, Boolean>();
	}
	
	/**
	 * Copy constructor to make a deep copy
	 * @param b
	 */
	public Board(Board b){
		//copy all cell statuses
		Cell_Status[][] cells = new Cell_Status[BOARD_SIZE][BOARD_SIZE];
		for(int i = 0; i < BOARD_SIZE; i++){
			for(int j = 0; j < BOARD_SIZE; j++){
				cells[i][j] = b.get_board().get_cell(i, j).get_data();
			}
		}
		
		board = new RectangularGrid<Cell_Status>(BOARD_SIZE, BOARD_SIZE, cells);
		walls = new int[]{b.walls[0], b.walls[1], b.walls[2], b.walls[3]};
		
		//put player pawns on the board in starting locations
		board.get_cell(b.p1_location.get_y_coordinate(), b.p1_location.get_x_coordinate()).set_data(Cell_Status.P1);
		p1_location = new Coordinate_Pair<Integer, Integer>(b.p1_location.get_y_coordinate(), b.p1_location.get_x_coordinate());
		board.get_cell(b.p2_location.get_y_coordinate(), b.p2_location.get_x_coordinate()).set_data(Cell_Status.P2);
		p2_location = new Coordinate_Pair<Integer, Integer>(b.p2_location.get_y_coordinate(), b.p2_location.get_x_coordinate());
		board.get_cell(b.p3_location.get_y_coordinate(), b.p3_location.get_x_coordinate()).set_data(Cell_Status.P3);
		p3_location = new Coordinate_Pair<Integer, Integer>(b.p3_location.get_y_coordinate(), b.p3_location.get_x_coordinate());
		board.get_cell(b.p4_location.get_y_coordinate(), b.p4_location.get_x_coordinate()).set_data(Cell_Status.P4);
		p4_location = new Coordinate_Pair<Integer, Integer>(b.p4_location.get_y_coordinate(), b.p4_location.get_x_coordinate());
	
		horizontal_wall_placement_locations = new HashMap<Coordinate_Pair<Integer,Integer>, Boolean>();
		vertical_wall_placement_locations = new HashMap<Coordinate_Pair<Integer,Integer>, Boolean>();
		for(Coordinate_Pair<Integer, Integer> key : b.horizontal_wall_placement_locations.keySet()){
			int row = key.get_y_coordinate();
			int col = key.get_x_coordinate();
			board.get_cell(row, col).get_neighbors().remove(board.get_cell(row-1, col));
			board.get_cell(row-1, col).get_neighbors().remove(board.get_cell(row, col));
			board.get_cell(row, col+1).get_neighbors().remove(board.get_cell(row-1, col+1));
			board.get_cell(row-1, col+1).get_neighbors().remove(board.get_cell(row, col+1));

			horizontal_wall_placement_locations.put(new Coordinate_Pair<Integer, Integer>(row, col), true);
		}
		for(Coordinate_Pair<Integer, Integer> key : b.vertical_wall_placement_locations.keySet()){
			int row = key.get_y_coordinate();
			int col = key.get_x_coordinate();
	
			board.get_cell(row, col).get_neighbors().remove(board.get_cell(row, col-1));
			board.get_cell(row, col-1).get_neighbors().remove(board.get_cell(row, col));
			board.get_cell(row+1, col).get_neighbors().remove(board.get_cell(row+1, col-1));
			board.get_cell(row+1, col-1).get_neighbors().remove(board.get_cell(row+1, col));

			vertical_wall_placement_locations.put(new Coordinate_Pair<Integer, Integer>(row, col), true);
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
	public HashMap<Coordinate_Pair<Integer, Integer>, Boolean> getHorizontal_wall_placement_locations() {
		return horizontal_wall_placement_locations;
	}

	public HashMap<Coordinate_Pair<Integer, Integer>, Boolean> getVertical_wall_placement_locations() {
		return vertical_wall_placement_locations;
	}

	/**
	 * Does a path exist from the provided board location to the specified column.
	 * 
	 * @param from_row
	 * @param from_col
	 * @param to_col
	 * @return
	 */
	private boolean path_exists_to_column(int from_row, int from_col, int to_col){
		//remove pawns from the board temporaily so jumps can be ignored
		board.get_cell(p1_location.get_y_coordinate(), p1_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(p2_location.get_y_coordinate(), p2_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(p3_location.get_y_coordinate(), p3_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(p4_location.get_y_coordinate(), p4_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		
		Stack<Coordinate_Pair<Integer, Integer>> tovisit_stack = new Stack<Coordinate_Pair<Integer, Integer>>();
		HashMap<Coordinate_Pair<Integer, Integer>, Boolean> visited_map = new HashMap<Coordinate_Pair<Integer, Integer>, Boolean>();
		tovisit_stack.push(new Coordinate_Pair<Integer, Integer>(from_row, from_col));

		while(!tovisit_stack.isEmpty()){
			Coordinate_Pair<Integer, Integer> next = tovisit_stack.pop();
			
			//is this in the target column?
			if(next.get_x_coordinate() == to_col){
				//replace pawns
				board.get_cell(p1_location.get_y_coordinate(), p1_location.get_x_coordinate()).set_data(Cell_Status.P1);
				board.get_cell(p2_location.get_y_coordinate(), p2_location.get_x_coordinate()).set_data(Cell_Status.P2);
				board.get_cell(p3_location.get_y_coordinate(), p3_location.get_x_coordinate()).set_data(Cell_Status.P3);
				board.get_cell(p4_location.get_y_coordinate(), p4_location.get_x_coordinate()).set_data(Cell_Status.P4);
				return true;
			}
			
			//set the coordinates as having been visited to prevent cycles
			visited_map.put(next, true);
			
			//up
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()-1, next.get_x_coordinate())){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()-1, next.get_x_coordinate());
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
				
			//down
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()+1, next.get_x_coordinate())){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()+1, next.get_x_coordinate());
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
			
			//left
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate(), next.get_x_coordinate()-1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate(), next.get_x_coordinate()-1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
			
			//right
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate(), next.get_x_coordinate()+1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate(), next.get_x_coordinate()+1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
			
			//check for jumpable players - i.e., check the four diagonal moves
			//up-left
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()-1, next.get_x_coordinate()-1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()-1, next.get_x_coordinate()-1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
			//up-right
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()-1, next.get_x_coordinate()+1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()-1, next.get_x_coordinate()+1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
			//down-right
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()+1, next.get_x_coordinate()+1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()+1, next.get_x_coordinate()+1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
			//down-left
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()+1, next.get_x_coordinate()-1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()+1, next.get_x_coordinate()-1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
		}
		
		//replace player pawns
		board.get_cell(p1_location.get_y_coordinate(), p1_location.get_x_coordinate()).set_data(Cell_Status.P1);
		board.get_cell(p2_location.get_y_coordinate(), p2_location.get_x_coordinate()).set_data(Cell_Status.P2);
		board.get_cell(p3_location.get_y_coordinate(), p3_location.get_x_coordinate()).set_data(Cell_Status.P3);
		board.get_cell(p4_location.get_y_coordinate(), p4_location.get_x_coordinate()).set_data(Cell_Status.P4);
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
	private boolean path_exists_to_row(int from_row, int from_col, int to_row){
		//remove pawns from the board temporaily so jumps can be ignored
		board.get_cell(p1_location.get_y_coordinate(), p1_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(p2_location.get_y_coordinate(), p2_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(p3_location.get_y_coordinate(), p3_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(p4_location.get_y_coordinate(), p4_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		
		Stack<Coordinate_Pair<Integer, Integer>> tovisit_stack = new Stack<Coordinate_Pair<Integer, Integer>>();
		HashMap<Coordinate_Pair<Integer, Integer>, Boolean> visited_map = new HashMap<Coordinate_Pair<Integer, Integer>, Boolean>();
		tovisit_stack.push(new Coordinate_Pair<Integer, Integer>(from_row, from_col));

		while(!tovisit_stack.isEmpty()){
			Coordinate_Pair<Integer, Integer> next = tovisit_stack.pop();
			
			//is this in the target column?
			if(next.get_y_coordinate() == to_row){
				//replace player pawns
				board.get_cell(p1_location.get_y_coordinate(), p1_location.get_x_coordinate()).set_data(Cell_Status.P1);
				board.get_cell(p2_location.get_y_coordinate(), p2_location.get_x_coordinate()).set_data(Cell_Status.P2);
				board.get_cell(p3_location.get_y_coordinate(), p3_location.get_x_coordinate()).set_data(Cell_Status.P3);
				board.get_cell(p4_location.get_y_coordinate(), p4_location.get_x_coordinate()).set_data(Cell_Status.P4);
				return true;
			}
			
			//set the coordinates as having been visited to prevent cycles
			visited_map.put(next, true);
			
			//up
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()-1, next.get_x_coordinate())){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()-1, next.get_x_coordinate());
				if(visited_map.get(next_cell) == null){
					tovisit_stack.push(next_cell);
				}
			}
				
			//down
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()+1, next.get_x_coordinate())){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()+1, next.get_x_coordinate());
				if(visited_map.get(next_cell) == null){
					tovisit_stack.push(next_cell);
				}
			}
			
			//left
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate(), next.get_x_coordinate()-1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate(), next.get_x_coordinate()-1);
				if(visited_map.get(next_cell) == null){
					tovisit_stack.push(next_cell);
				}
			}
			
			//right
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate(), next.get_x_coordinate()+1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate(), next.get_x_coordinate()+1);
				if(visited_map.get(next_cell) == null){
					tovisit_stack.push(next_cell);
				}
			}
			
			//check for jumpable players - i.e., check the four diagonal moves
			//up-left
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()-1, next.get_x_coordinate()-1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()-1, next.get_x_coordinate()-1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
			//up-right
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()-1, next.get_x_coordinate()+1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()-1, next.get_x_coordinate()+1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
			//down-right
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()+1, next.get_x_coordinate()+1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()+1, next.get_x_coordinate()+1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
			//down-left
			if(can_move_to(next.get_y_coordinate(), next.get_x_coordinate(), next.get_y_coordinate()+1, next.get_x_coordinate()-1)){
				Coordinate_Pair<Integer, Integer> next_cell = new Coordinate_Pair<Integer, Integer>(next.get_y_coordinate()+1, next.get_x_coordinate()-1);
				if(visited_map.get(next_cell) == null){ //prevent cycles
					tovisit_stack.push(next_cell);
				}
			}
		}
		
		//replace player pawns
		board.get_cell(p1_location.get_y_coordinate(), p1_location.get_x_coordinate()).set_data(Cell_Status.P1);
		board.get_cell(p2_location.get_y_coordinate(), p2_location.get_x_coordinate()).set_data(Cell_Status.P2);
		board.get_cell(p3_location.get_y_coordinate(), p3_location.get_x_coordinate()).set_data(Cell_Status.P3);
		board.get_cell(p4_location.get_y_coordinate(), p4_location.get_x_coordinate()).set_data(Cell_Status.P4);
		
		return false;
	}
	
	/**
	 * Computes the shortest path to the goal on the given board for player p
	 * Returns the shortest path distance.
	 */
	public int shortest_path(Player_ID p){
		HashMap<Cell<Cell_Status>, Integer> distances = null;
		Integer min_dist = null;
		int ret = 0;
		
		//remove pawns from the board temporaily so jumps can be ignored
		board.get_cell(p1_location.get_y_coordinate(), p1_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(p2_location.get_y_coordinate(), p2_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(p3_location.get_y_coordinate(), p3_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		board.get_cell(p4_location.get_y_coordinate(), p4_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		
		switch(p){
			// player 1 trying to reach row BOARD_SIZE -1
			case PLAYER_1:
				distances = board.dijkstra(p1_location.get_y_coordinate(), p1_location.get_x_coordinate());
				min_dist = distances.get(board.get_cell(BOARD_SIZE-1, 0));
				for(int i = 1; i < BOARD_SIZE; i++){
					if(distances.get(board.get_cell(BOARD_SIZE-1, i)) < min_dist){
						min_dist = distances.get(board.get_cell(BOARD_SIZE-1, i));
					}
				}
				ret = min_dist;
				break;
			// player 2 trying to reach column 0	
			case PLAYER_2:
				distances = board.dijkstra(p2_location.get_y_coordinate(), p2_location.get_x_coordinate());
				min_dist = distances.get(board.get_cell(0, 0));
				for(int i = 1; i < BOARD_SIZE; i++){
					if(distances.get(board.get_cell(i, 0)) < min_dist){
						min_dist = distances.get(board.get_cell(i, 0));
					}
				}
				ret = min_dist;
				break;
			// player 3 trying to reach row 0
			case PLAYER_3:
				distances = board.dijkstra(p3_location.get_y_coordinate(), p3_location.get_x_coordinate());
				min_dist = distances.get(board.get_cell(0, 0));
				for(int i = 1; i < BOARD_SIZE; i++){
					if(distances.get(board.get_cell(0, i)) < min_dist){
						min_dist = distances.get(board.get_cell(0, i));
					}
				}
				ret = min_dist;
				break;
				
			// player 4 trying to reach column BOARD_SIZE - 1
			case PLAYER_4:
				distances = board.dijkstra(p4_location.get_y_coordinate(), p4_location.get_x_coordinate());
				min_dist = distances.get(board.get_cell(0, BOARD_SIZE - 1));
				for(int i = 1; i < BOARD_SIZE; i++){
					if(distances.get(board.get_cell(i, BOARD_SIZE - 1)) < min_dist){
						min_dist = distances.get(board.get_cell(i, BOARD_SIZE - 1));
					}
				}
				ret = min_dist;
				break;
		}
		
		//replace player pawns
		board.get_cell(p1_location.get_y_coordinate(), p1_location.get_x_coordinate()).set_data(Cell_Status.P1);
		board.get_cell(p2_location.get_y_coordinate(), p2_location.get_x_coordinate()).set_data(Cell_Status.P2);
		board.get_cell(p3_location.get_y_coordinate(), p3_location.get_x_coordinate()).set_data(Cell_Status.P3);
		board.get_cell(p4_location.get_y_coordinate(), p4_location.get_x_coordinate()).set_data(Cell_Status.P4);
		return ret;
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
			if(vertical_wall_placement_locations.get(new Coordinate_Pair<Integer, Integer>(row - 1, col+1)) != null){
				//check for a vertical intersecting wall
				return false;
			}
		}
		else{
			if(horizontal_wall_placement_locations.get(new Coordinate_Pair<Integer, Integer>(row + 1, col-1)) != null){
				//check for a horizontal intersecting wall
				return false;
			}
		}
		
		//walls must be within 1 space of another wall or 2 spaces of a pawn
		boolean is_wall_too_far_from_walls = true;
		for(Coordinate_Pair<Integer, Integer> wall : horizontal_wall_placement_locations.keySet()){
			if(distance_between_walls(row, col, place_horizontally, wall.get_y_coordinate(), wall.get_x_coordinate(), true) <= 1){
				is_wall_too_far_from_walls = false;
			}
		}
		for(Coordinate_Pair<Integer, Integer> wall : vertical_wall_placement_locations.keySet()){
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
		if(path_exists_to_row(p1_location.get_y_coordinate(), p1_location.get_x_coordinate(), BOARD_SIZE-1) && 
				path_exists_to_column(p2_location.get_y_coordinate(), p2_location.get_x_coordinate(), 0) &&
				path_exists_to_row(p3_location.get_y_coordinate(), p3_location.get_x_coordinate(), 0) &&
				path_exists_to_column(p4_location.get_y_coordinate(), p4_location.get_x_coordinate(), BOARD_SIZE-1)){
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
			wall_end_col = wall_col+1;
		}
		else{
			wall_end_row = wall_row+1;
			wall_end_col = wall_col;
		}
		
		
		int[] point_distances = new int[8];
		Integer curr_min = Integer.MAX_VALUE; 
		Coordinate_Pair[] locations = new Coordinate_Pair[]{p1_location, p2_location, p3_location, p4_location};
		for(int i= 0; i < locations.length; i++){
			point_distances[0] = Math.abs(wall_row - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_y_coordinate()) + Math.abs(wall_col - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_x_coordinate());
			point_distances[1] = Math.abs(wall_row - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_y_coordinate()) + Math.abs(wall_col - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_x_coordinate()+1);
			point_distances[2] = Math.abs(wall_row - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_y_coordinate() +1) + Math.abs(wall_col - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_x_coordinate()+1);
			point_distances[3] = Math.abs(wall_row - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_y_coordinate()+1) + Math.abs(wall_col - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_x_coordinate());
			
			point_distances[4] = Math.abs(wall_end_row - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_y_coordinate()) + Math.abs(wall_end_col - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_x_coordinate());
			point_distances[5] = Math.abs(wall_end_row - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_y_coordinate()) + Math.abs(wall_end_col - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_x_coordinate()+1);
			point_distances[6] = Math.abs(wall_end_row - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_y_coordinate() +1) + Math.abs(wall_end_col - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_x_coordinate()+1);
			point_distances[7] = Math.abs(wall_end_row - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_y_coordinate()+1) + Math.abs(wall_end_col - ((Coordinate_Pair<Integer, Integer>)locations[i]).get_x_coordinate());
			
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
		int wall1_end_row, wall1_end_col, wall2_end_row, wall2_end_col;
		if(wall1_is_horizontal){
			wall1_end_row = wall1_row;
			wall1_end_col = wall1_col+1;
		}
		else{
			wall1_end_row = wall1_row+1;
			wall1_end_col = wall1_col;
		}
		if(wall2_is_horizontal){
			wall2_end_row = wall2_row;
			wall2_end_col = wall2_col+1;
		}
		else{
			wall2_end_row = wall2_row+1;
			wall2_end_col = wall2_col;
		}
	
		int[] point_distances = new int[4];
		point_distances[0] = Math.abs(wall1_row - wall2_row) + Math.abs(wall1_col - wall2_col);
		point_distances[1] = Math.abs(wall1_row - wall2_end_row) + Math.abs(wall1_col - wall2_end_col);
		point_distances[2] = Math.abs(wall1_end_row - wall2_row) + Math.abs(wall1_end_col - wall2_col);
		point_distances[3] = Math.abs(wall1_end_row - wall2_end_row) + Math.abs(wall1_end_col - wall2_end_col);
	
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
				horizontal_wall_placement_locations.put(new Coordinate_Pair<Integer, Integer>(row, col), true);
			}
			else{
				board.get_cell(row, col).get_neighbors().remove(board.get_cell(row, col-1));
				board.get_cell(row, col-1).get_neighbors().remove(board.get_cell(row, col));
				board.get_cell(row+1, col).get_neighbors().remove(board.get_cell(row+1, col-1));
				board.get_cell(row+1, col-1).get_neighbors().remove(board.get_cell(row+1, col));
				vertical_wall_placement_locations.put(new Coordinate_Pair<Integer, Integer>(row, col), true);
			}
			
			return true; //the wall could was successfully placed in the desired location
		}
		
		return false; //the wall could not be placed in the desired location
	}
	
	/**
	 * Move the specified layer from the given location to the new location. 
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
			switch (player) {
			case PLAYER_1:
				p1_location = new Coordinate_Pair<Integer, Integer>(to_row, to_col);
				board.get_cell(to_row, to_col).set_data(Cell_Status.P1);
				break;
			case PLAYER_2:
				p2_location = new Coordinate_Pair<Integer, Integer>(to_row, to_col);
				board.get_cell(to_row, to_col).set_data(Cell_Status.P2);
				break;
			case PLAYER_3:
				p3_location = new Coordinate_Pair<Integer, Integer>(to_row, to_col);
				board.get_cell(to_row, to_col).set_data(Cell_Status.P3);
				break;
			case PLAYER_4:
				p4_location = new Coordinate_Pair<Integer, Integer>(to_row, to_col);
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
			buf.append("-");
		}
		buf.append("\n");
		for(int i = 0; i < BOARD_SIZE; i++){
			buf.append("|");
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
						buf.append("*");
					}
				}
			}
			buf.append("|\n");
			
			if(i < BOARD_SIZE - 1 && i >= 0){
				int board_j = 0;
				for(int j = 0; j < 2*BOARD_SIZE +1; j++){
					if(j%2==0){
						buf.append("-");
					}
					else{
						if(board.get_cell(i+1, board_j).get_neighbors().contains(board.get_cell(i, board_j))){
							buf.append("-");
						}
						else {
							buf.append("*");
						}
						board_j++;
					}
				}
				buf.append("\n");
			}
		}
		
		for(int i = 0; i < 2*BOARD_SIZE +1; i++){
			buf.append("-");
		}
		return buf.toString();
	}
	
	/**
	 * Modifies the board to represent the effects of the specified move.
	 * 
	 * @param m
	 * @return true if the move is applied successfully, false if the move was invalid and not applied
	 */
	public boolean apply_move(Move m){
		if(m.getMove_type() == Move.MOVE_TYPE.MOVE_PAWN){
			switch(m.getPlayer_making_move()){
				case PLAYER_1:
					return move(p1_location.get_y_coordinate(), p1_location.get_x_coordinate(), 
							    m.getTarget_cell_coordinates().get_y_coordinate(), 
							    m.getTarget_cell_coordinates().get_x_coordinate(), Player_ID.PLAYER_1);
				
				case PLAYER_2:
					return move(p2_location.get_y_coordinate(), p2_location.get_x_coordinate(), 
							    m.getTarget_cell_coordinates().get_y_coordinate(), 
							    m.getTarget_cell_coordinates().get_x_coordinate(), Player_ID.PLAYER_2);
				
				case PLAYER_3:
					return move(p3_location.get_y_coordinate(), p3_location.get_x_coordinate(), 
							    m.getTarget_cell_coordinates().get_y_coordinate(), 
							    m.getTarget_cell_coordinates().get_x_coordinate(), Player_ID.PLAYER_3);
				
				case PLAYER_4:
					return move(p4_location.get_y_coordinate(), p4_location.get_x_coordinate(), 
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
	public Vector<Move> get_possible_moves(Player_ID player_id){
		Vector<Move> possible_moves = new Vector<Move>();
		
		//moving options
		int player_row_location;
		int player_col_location;
		int walls_available = 0;
		switch(player_id){
			case PLAYER_1:
				player_row_location = p1_location.get_y_coordinate();
				player_col_location = p1_location.get_x_coordinate();
				walls_available = walls[0];
				break;
			case PLAYER_2:
				player_row_location = p2_location.get_y_coordinate();
				player_col_location = p2_location.get_x_coordinate();
				walls_available = walls[1];
				break;
			case PLAYER_3:
				player_row_location = p3_location.get_y_coordinate();
				player_col_location = p3_location.get_x_coordinate();
				walls_available = walls[2];
				break;
			case PLAYER_4:
				player_row_location = p4_location.get_y_coordinate();
				player_col_location = p4_location.get_x_coordinate();
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
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location-1, player_col_location)));
		}
		//down
		if(can_move_to(player_row_location, player_col_location, player_row_location+1, player_col_location)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location+1, player_col_location)));
		}
		//left
		if(can_move_to(player_row_location, player_col_location, player_row_location, player_col_location-1)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location, player_col_location-1)));
		}
		//right
		if(can_move_to(player_row_location, player_col_location, player_row_location, player_col_location+1)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location, player_col_location+1)));
		}
		//up-left
		if(can_move_to(player_row_location, player_col_location, player_row_location-1, player_col_location-1)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location-1, player_col_location-1)));
		}
		//up-right
		if(can_move_to(player_row_location, player_col_location, player_row_location-1, player_col_location+1)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location-1, player_col_location+1)));
		}
		//down-right
		if(can_move_to(player_row_location, player_col_location, player_row_location+1, player_col_location+1)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location+1, player_col_location+1)));
		}
		//down-left
		if(can_move_to(player_row_location, player_col_location, player_row_location+1, player_col_location-1)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location+1, player_col_location-1)));
		}
		//jumping moves
		//up2
		if(can_move_to(player_row_location, player_col_location, player_row_location-2, player_col_location)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location-2, player_col_location)));
		}
		//down2
		if(can_move_to(player_row_location, player_col_location, player_row_location+2, player_col_location)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location+2, player_col_location)));
		}
		//left2
		if(can_move_to(player_row_location, player_col_location, player_row_location, player_col_location-2)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location, player_col_location-2)));
		}
		//right2
		if(can_move_to(player_row_location, player_col_location, player_row_location, player_col_location+2)){
			possible_moves.add(new Move(MOVE_TYPE.MOVE_PAWN, player_id, new Coordinate_Pair<Integer, Integer>(player_row_location, player_col_location+2)));
		}
		
		//wall-placing options
		if(walls_available > 0){
			for(int i = 0; i < BOARD_SIZE-1; i++){
				for(int j = 0; j < BOARD_SIZE-1; j++){
					if(can_place_wall(i, j, true)){
						possible_moves.add(new Move(MOVE_TYPE.PLACE_WALL, player_id, new Coordinate_Pair<Integer, Integer>(i, j), true));
					}
					if(can_place_wall(i, j, false)){
						possible_moves.add(new Move(MOVE_TYPE.PLACE_WALL, player_id, new Coordinate_Pair<Integer, Integer>(i, j), false));
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
		return p1_location.get_y_coordinate() == BOARD_SIZE-1 ||  
			   p2_location.get_x_coordinate() == 0 ||
			   p3_location.get_y_coordinate() == 0 ||
			   p4_location.get_x_coordinate() == BOARD_SIZE-1;
	}
	
	/**
	 * Determine the winner of the game. If no one is the winner, return the player in the
	 * lead (i.e., player closest to their goal). 
	 * 
	 * @return the id of the winner, if there is a tie, null is returned.
	 */
	public Player_ID compute_winner(){
		if(p1_location.get_y_coordinate() == BOARD_SIZE-1)
			return Player_ID.PLAYER_1;
		else if(p2_location.get_x_coordinate() == 0)
			return Player_ID.PLAYER_2;
		else if(p3_location.get_y_coordinate() == 0)
			return Player_ID.PLAYER_3;
		else if(p4_location.get_x_coordinate() == BOARD_SIZE-1)
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
	
	public Coordinate_Pair<Integer, Integer> get_player_location(Player_ID p){
		switch(p){
			case PLAYER_1:
				return p1_location;
			case PLAYER_2:
				return p2_location;
			case PLAYER_3:
				return p3_location;
			case PLAYER_4:
				return p4_location;
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
		Board b = new Board();
		
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
				
		b.board.get_cell(b.p3_location.get_y_coordinate(), b.p3_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		b.p3_location = new Coordinate_Pair<Integer, Integer>(3, 0);
		b.board.get_cell(3, 0).set_data(Cell_Status.P3);
		
		b.board.get_cell(b.p4_location.get_y_coordinate(), b.p4_location.get_x_coordinate()).set_data(Cell_Status.FREE);
		b.p4_location = new Coordinate_Pair<Integer, Integer>(3, 1);
		b.board.get_cell(3, 1).set_data(Cell_Status.P4);
		
		b.place_wall(3, 0, true);
		b.place_wall(3, 2, true);
		System.out.println(b.can_move_to(3, 0, 3, 2));

		b.place_wall(4, 0, true);
		System.out.println("After placing a walls: \n" + b);
		
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
