package game;

import java.util.Vector;

import players.Player;
import players.Player_ID;

/**
 * This class implements the logic of running a game of quoridor as well as calculating the winner. 
 */
public class Quoridor {
	private static int MAX_TURNS = 200;
	// Max time per turn in milliseconds
	private static long max_t = 2000;
	private static Player_ID[] players_ids = new Player_ID[]{Player_ID.PLAYER_1, Player_ID.PLAYER_2, Player_ID.PLAYER_3, Player_ID.PLAYER_4};
	
	public static Player_ID run_game(Vector<Player> players, Boolean print){
		int current_turn = 0;
		Board b = new Board();
		int player_turn_idx = 0;
		
		for(int i = 0; i < 4; i++){
			players.get(i).set_id(players_ids[i]);
		}
		
		while(current_turn < MAX_TURNS && !(b.is_game_over())){
			if ( print ) {
				// Print turn info here so debugging in player is printed after
				System.out.println("Ply " + (current_turn + 1)+ ": Turn " +  (current_turn / 4 + 1) +":" );
				System.out.println("Player " + (player_turn_idx + 1)+ " move:");
			}

			// Handle odd situation where a player has no moves
			if(b.get_possible_moves(players_ids[player_turn_idx]).size() == 0){
				if ( print )
					System.out.println("Player " + (player_turn_idx + 1) + " skipped:\n" + b);
				current_turn++;
				player_turn_idx = (player_turn_idx + 1) % 4;
				continue;
			}

			// Make move
			long start_t = System.currentTimeMillis();
			Move move_made = players.get(player_turn_idx).make_move(new Board(b));
			long delta_t = System.currentTimeMillis() - start_t;

			// if the move is invalid
			if(!b.apply_move(move_made)){
				System.err.println("Player " + (player_turn_idx + 1) +" made an invalid move: " + move_made);
				System.exit(0);
			}
			
			for(int i = 0; i < players.size(); i++){
				if(i != player_turn_idx){
					players.get(i).notify_of_move(players_ids[player_turn_idx], move_made, new Board(b));
				}
			}
			
			if ( print ) {
				// Print board and other usefull information
				System.out.println(move_made);
				System.out.println("Time: " + delta_t + "ms " + ( (delta_t > max_t) ? "OVERTIME": "" ) );
				System.out.println(b); // Print board
				System.out.println("          Current   Walls  Distance");
				System.out.println("          Location  Left    to Win");
				for(int i = 0; i < players.size(); i++){
					System.out.println("Player " + (i + 1) + ":  (" +
						b.get_player_location(players_ids[i]).get_x_coordinate() + "," +
						b.get_player_location(players_ids[i]).get_y_coordinate() + ")      " +
						b.get_wall_count(players_ids[i]) + "       " +
						b.shortest_path(players_ids[i] ) );
				}
				System.out.println("Hit/Miss: " + b.cache_hit + "/" + b.cache_miss );
				System.out.println("");
			}
			
			current_turn++;
			player_turn_idx = (player_turn_idx + 1) % 4;
		}
		
		return b.compute_winner();
	}
}
