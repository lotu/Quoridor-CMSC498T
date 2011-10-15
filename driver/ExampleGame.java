package driver;

import game.Board;
import game.Quoridor;

import java.util.Vector;
import java.util.Random;

import players.Player;
import players.Player_ID;
import players.Random_Player;
import players.MinMax_Player;
import players.OneAhead_Player;
import players.AlphaBeta_Player;
import players.Wall_Follow_Player;

/**
 * Set up and run a simple Quoridor game with 2 randomly moving agents and
 * two wall following agents.
 */
public class ExampleGame {
	public static void main(String[] args){
		// use a small seed by default
		long seed = System.currentTimeMillis() % 10000;
		int game_type = 1; // easy mix
		// parse arguments poorly
		if (args.length > 0) {
			try {
				game_type = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Argument must be a integer");
				System.exit(1);
			}
		}
		if (args.length > 1) {
			try {
				seed = Long.parseLong(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("Argument must be a integer");
				System.exit(1);
			}
		}
		System.out.println ("Game type: " + game_type );
		System.out.println ("Using seed: " + seed );

		Vector<Player> players = new Vector<Player>();
		Vector<String> names = new Vector<String>();
		Random rng = new Random( seed );
		
		// Add 4 players
		switch( game_type ) {
			case 0:	// random
				players.add(new Random_Player());      names.add("Random");
				players.add(new Random_Player());      names.add("Random");
				players.add(new Random_Player());      names.add("Random");
				players.add(new Random_Player());      names.add("Random");
				break;
			case 1: // easy mix
				players.add(new Random_Player());      names.add("Random");
				players.add(new Wall_Follow_Player()); names.add("Wall Follower");
				players.add(new OneAhead_Player());    names.add("One Ahead");
				players.add(new Wall_Follow_Player()); names.add("Wall Follower");
				break;
			case 2: // one Ahead (quick)
				players.add(new OneAhead_Player());    names.add("One Ahead");
				players.add(new OneAhead_Player());    names.add("One Ahead");
				players.add(new OneAhead_Player());    names.add("One Ahead");
				players.add(new OneAhead_Player());    names.add("One Ahead");
				break;
			case 3: // good mix (slow)
				players.add(new MinMax_Player());      names.add("MinMax");
				players.add(new OneAhead_Player());    names.add("One Ahead");
				players.add(new MinMax_Player());      names.add("MinMax");
				players.add(new OneAhead_Player());    names.add("One Ahead");
				break;
			case 4: // minMax (slow)
				players.add(new MinMax_Player());      names.add("MinMax");
				players.add(new MinMax_Player());      names.add("MinMax");
				players.add(new MinMax_Player());      names.add("MinMax");
				players.add(new MinMax_Player());      names.add("MinMax");
				break;
		}

		for(int i = 0; i < 4; i++){
			players.get(i).set_seed( rng.nextLong() );
		}
		
		Player_ID[] players_ids = new Player_ID[]{Player_ID.PLAYER_1, Player_ID.PLAYER_2, Player_ID.PLAYER_3, Player_ID.PLAYER_4};

		Board b = Quoridor.run_game(players, Boolean.TRUE);
		Player_ID winner = b.compute_winner();

		System.out.println("               Current   Walls  Distance");
		System.out.println("               Location  Left    to Win");
		for(int i = 0; i < players.size(); i++){
			System.out.format("%13s:  (%d,%d) %6d %7d\n", names.get(i),
				b.get_player_location(players_ids[i]).get_x_coordinate(),
				b.get_player_location(players_ids[i]).get_y_coordinate(),
				b.get_wall_count(players_ids[i]),
				b.shortest_path(players_ids[i]) );
		}
		System.out.println("");

		if(winner == null){
			System.out.println("Tie game.");
		}
		else{
			System.out.format("Player %d (%s) wins.\n", winner.ordinal() + 1, names.get(winner.ordinal()));
		}
		System.out.println ("Game type: " + game_type );
		System.out.println ("Seed: " + seed );
	}
}
