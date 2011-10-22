package driver;

import game.Quoridor;

import java.util.Vector;

import players.Player;
import players.Player_ID;
import players.P1_Bullen;

/**
 * Set up and run a simple Quoridor game with 4 randomly moving agents.
 */
public class ExampleGame {
	public static void main(String[] args){
		Vector<Player> players = new Vector<Player>();
		
		for(int i = 0; i < 4; i++){
			players.add(new P1_Bullen());
		}
		
		Player_ID winner = Quoridor.run_game(players);
		
		if(winner == null){
			System.out.println("Tie game.");
		}
		else{
			System.out.format("%s wins.\n", winner);
		}
	}
}
