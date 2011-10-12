package driver;

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
		Random rng = new Random( seed );
		
		// Add 4 players
		switch( game_type ) {
			case 0:	// random
				players.add(new Random_Player());
				players.add(new Random_Player());
				players.add(new Random_Player());
				players.add(new Random_Player());
				break;
			case 1: // easy mix
				players.add(new Random_Player());
				players.add(new Wall_Follow_Player());
				players.add(new OneAhead_Player());
				players.add(new Wall_Follow_Player());
				break;
			case 2: // one Ahead (quick)
				players.add(new OneAhead_Player());
				players.add(new OneAhead_Player());
				players.add(new OneAhead_Player());
				players.add(new OneAhead_Player());
				break;
			case 3: // good mix (slow)
				players.add(new MinMax_Player());
				players.add(new OneAhead_Player());
				players.add(new MinMax_Player());
				players.add(new OneAhead_Player());
				break;
			case 4: // minMax (slow)
				players.add(new MinMax_Player());
				players.add(new MinMax_Player());
				players.add(new MinMax_Player());
				players.add(new MinMax_Player());
				break;
		}

		for(int i = 0; i < 4; i++){
			players.get(i).set_seed( rng.nextLong() );
		}
		
		Player_ID winner = Quoridor.run_game(players, Boolean.TRUE);
		
		if(winner == null){
			System.out.println("Tie game.");
		}
		else{
			System.out.format("%s wins.\n", winner);
		}
		System.out.println ("Game type: " + game_type );
		System.out.println ("Seed: " + seed );
	}
}
