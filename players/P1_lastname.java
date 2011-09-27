package players;

import game.Board;
import game.Move;

/**
 * #### README for basic instructions. ####
 * 
 * This is the stub for a new Quoridor player. Rename the class *and* file to include your last name 
 * then implement your player here. When complete, email this file to 
 * bswilson@cs.umd.edu. Make sure to include CMSC498T in the subject line. 
 */
public class P1_lastname implements Player{

	//implement your decision logic here.
	public Move make_move(Board b) {
		// TODO Auto-generated method stub
		return null;
	}

	//process notifications of moves made by other players here
	public void notify_of_move(Player_ID playerThatMadeMove, Move moveMade,
			Board resultingBoard) {
		// TODO Auto-generated method stub
		
	}

	//the game driver will notify your player of its ID using this method
	public void set_id(Player_ID id) {
		// TODO Auto-generated method stub
		
	}

}
