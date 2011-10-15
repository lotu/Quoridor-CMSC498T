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
 * Set up and run a simple Quoridor Tournament
 */
public class TournamentGame {
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

		Vector<PlayerFactory> p_builder = new Vector<PlayerFactory>();
		Vector<String> names = new Vector<String>();
		Random rng = new Random( seed );
		
		// Add 4 players
		switch( game_type ) {
			case 0:	// random
				for ( int i = 0; i < 4; i++){
					p_builder.add(new PlayerFactory(Random_Player.class) );
					names.add("Random");
				}
				break;
			case 1: // easy mix
				p_builder.add(new PlayerFactory(Random_Player.class));      names.add("Random");
				p_builder.add(new PlayerFactory(Wall_Follow_Player.class)); names.add("Wall Follower");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead");
				p_builder.add(new PlayerFactory(Wall_Follow_Player.class)); names.add("Wall Follower");
				break;
			case 2: // one Ahead (quick)
				for ( int i = 0; i < 4; i++){
					p_builder.add(new PlayerFactory(OneAhead_Player.class) );
					names.add("One Ahead");
				}
				break;
			case 3: // good mix (slow)
				p_builder.add(new PlayerFactory(MinMax_Player.class));      names.add("MinMax");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead");
				p_builder.add(new PlayerFactory(MinMax_Player.class));      names.add("MinMax");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead");
				break;
			case 4: // minMax (slow)
				for ( int i = 0; i < 4; i++){
					p_builder.add(new PlayerFactory(MinMax_Player.class) );
					names.add("MinMax");
				}
				break;
		}

		Player_ID[] players_ids = new Player_ID[]{Player_ID.PLAYER_1, Player_ID.PLAYER_2, 
		                                          Player_ID.PLAYER_3, Player_ID.PLAYER_4};
		Vector<Player> players = new Vector<Player>();
		int[] scores = {0,0,0,0};
		int[] wall_count = {0,0,0,0};
		int[] distance = {0,0,0,0};

		// run games
		for( int x = 0; x < 12; x++){
			players = new Vector<Player>();
			for(int i = 0; i < 4; i++){
				players.add( p_builder.get(i).getPlayer(rng ) );
			}

			Board b = Quoridor.run_game(players, false);
			Player_ID winner = b.compute_winner();

			System.out.println("               Current   Walls  Distance");
			System.out.println("               Location  Left    to Win");
			for(int i = 0; i < players.size(); i++){
				System.out.format("%13s:  (%d,%d) %6d %7d\n", names.get(i),
					b.get_player_location(players_ids[i]).get_x_coordinate(),
					b.get_player_location(players_ids[i]).get_y_coordinate(),
					b.get_wall_count(players_ids[i]),
					b.shortest_path(players_ids[i]) );

					wall_count[i] += b.get_wall_count(players_ids[i]);
					distance[i] += b.shortest_path(players_ids[i]) ;
			}

			if(winner == null){
				System.out.println("Tie game.");
			}
			else{
				scores[winner.ordinal()] += 1;
				System.out.format("Player %d (%s) wins.\n", 
						winner.ordinal() + 1, names.get(winner.ordinal()));
			}
			System.out.println("");
		}

		System.out.println("");
		System.out.println("");
		System.out.println("===============RESULTS==================");
		System.out.println("                Wins    Walls  Distance");
		System.out.println("                        Left    to Win");
		for(int i = 0; i < players.size(); i++){
			System.out.format("%13s: %4d %7d %7d\n", names.get(i),
				scores[i], wall_count[i], distance[i] );
		}

		System.out.println ("Game type: " + game_type );
		System.out.println ("Seed: " + seed );
	}
}

class PlayerFactory {
	private Class<?> player_type ;
	public PlayerFactory( Class<?> player )
	{ 
		player_type = player;
	}

	public Player getPlayer( Random rnd ) {
		try {
			Player p = (Player) player_type.newInstance();
			p.set_seed( rnd.nextLong() );
			return p;
		} catch ( Exception e ) {}
		return null;
	}
}
