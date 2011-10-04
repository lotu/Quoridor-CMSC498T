package game;

import java.util.Vector;

import players.Player;
import players.Player_ID;

/**
 * This class implements the logic of running a game of quoridor as well as calculating the winner. 
 */
public class Quoridor {
	private static int MAX_TURNS = 200;
	private static Player_ID[] players_ids = new Player_ID[]{Player_ID.PLAYER_1, Player_ID.PLAYER_2, Player_ID.PLAYER_3, Player_ID.PLAYER_4};
	
	public static Player_ID run_game(Vector<Player> players){
		int current_turn = 0;
		Board b = new Board();
		int player_turn_idx = 0;
		
		for(int i = 0; i < 4; i++){
			players.get(i).set_id(players_ids[i]);
		}
		
		while(current_turn < MAX_TURNS && !(b.is_game_over())){
			// Handle odd situation where a player has no moves
			if(b.get_possible_moves(players_ids[player_turn_idx]).size() == 0){
				current_turn++;
				player_turn_idx = (player_turn_idx + 1) % 4;
				continue;
			}

			Move move_made = players.get(player_turn_idx).make_move(new Board(b));
			if(!b.apply_move(move_made)){
				System.err.println("Player " + player_turn_idx + " made an invalid move: " + move_made);
				System.exit(0);
			}
			
			for(int i = 0; i < players.size(); i++){
				if(i != player_turn_idx){
					players.get(i).notify_of_move(players_ids[player_turn_idx], move_made, new Board(b));
				}
			}
			
			System.out.println("Player " + player_turn_idx + " moved:\n" + b);
			
			current_turn++;
			player_turn_idx = (player_turn_idx + 1) % 4; 
		}
		
		return b.compute_winner();
	}
}
