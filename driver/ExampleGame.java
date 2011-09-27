package driver;

import game.Quoridor;

import java.util.Vector;
import java.util.Random;

import players.Player;
import players.Player_ID;
import players.Random_Player;

/**
 * Set up and run a simple Quoridor game with 4 randomly moving agents.
 */
public class ExampleGame {
	public static void main(String[] args){
		// use a small seed by default
		long seed = System.currentTimeMillis() % 10000;
		// parse arguments poorly
		if (args.length > 0) {
			try {
				seed = Long.parseLong(args[0]);
			} catch (NumberFormatException e) {
				System.err.println("Argument must be a integer");
				System.exit(1);
			}
		}
		System.out.println ("Using seed: " + seed );

		Vector<Player> players = new Vector<Player>();
		Random rng = new Random( seed );
		
		for(int i = 0; i < 4; i++){
			players.add(new Random_Player());
			players.get(i).set_seed( rng.nextLong() );
		}
		
		Player_ID winner = Quoridor.run_game(players);
		
		if(winner == null){
			System.out.println("Tie game.");
		}
		else{
			System.out.format("%s wins.\n", winner);
		}
		System.out.println ("Seed: " + seed );
	}
}
