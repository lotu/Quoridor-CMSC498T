package players;
import game.Board;
import game.Move;


public interface Player {
	
	/**
	 * This method is where the primary  decision-making logic will be implemented.
	 * 
	 * @param b - the current state of the game is contained in the provided Board instance
	 * @return the desired move to be made
	 */
	public Move make_move(Board b);
	
	/**
	 * This method is provided as a source of feedback on the status of the game between turns.
	 * The player is notified each time an opponent makes a move. 
	 * 
	 * @param player_that_made_move - the id of the player that made the move
	 * @param move_made - the move that the player made
	 * @param resulting_board - the board AFTER the move is applied
	 */
	public void notify_of_move(Player_ID player_that_made_move, Move move_made, Board resulting_board);
	
	/**
	 * Notifies the player of it's id in the game.
	 * 
	 * @param id
	 */
	public void set_id(Player_ID id);

	/**
	 * Provide a seed to the player to allow random agents to run repeatablly.
	 * 
	 * @param seed
	 */
	public void set_seed(long seed);

}
