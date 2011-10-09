package game;
import players.Player_ID;

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
public class Move {
	//an enum to distinguish between the two types of moves
	public enum MOVE_TYPE{PLACE_WALL, MOVE_PAWN};
	
	private MOVE_TYPE move_type;
	private Player_ID player_making_move;
	private Coordinate_Pair target_cell_coordinates;
	private Boolean is_horizontal;
	
	/**
	 * A constructor for move-pawn type moves
	 * 
	 * @param type
	 * @param player_id
	 * @param target
	 */
	public Move(MOVE_TYPE type, Player_ID player_id, Coordinate_Pair target_cell_coordinates){
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
	public Move(MOVE_TYPE type, Player_ID player_id, Coordinate_Pair target_cell_coordinates, boolean is_horizontal){
		this(type, player_id, target_cell_coordinates);
		
		this.is_horizontal = is_horizontal;
	}

	public MOVE_TYPE getMove_type() {
		return move_type;
	}

	public Player_ID getPlayer_making_move() {
		return player_making_move;
	}

	public Coordinate_Pair getTarget_cell_coordinates() {
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
