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
import players.OneAheadNew_Player;
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
		int games = 8; // number of games
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
				games = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.err.println("Argument must be a integer");
				System.exit(1);
			}
		}
		if (args.length > 2) {
			try {
				seed = Long.parseLong(args[2]);
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
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead1");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead2");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead3");
				p_builder.add(new PlayerFactory(OneAheadNew_Player.class));    names.add("One New2");
				break;
			case 3: // one Ahead vs
				p_builder.add(new PlayerFactory(OneAheadNew_Player.class));    names.add("One New1");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead1");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead2");
				p_builder.add(new PlayerFactory(OneAheadNew_Player.class));    names.add("One New2");
				break;
			case 4: // one Ahead vs
				p_builder.add(new PlayerFactory(OneAheadNew_Player.class));    names.add("One New1");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead1");
				p_builder.add(new PlayerFactory(OneAheadNew_Player.class));    names.add("One New2");
				p_builder.add(new PlayerFactory(OneAheadNew_Player.class));    names.add("One New3");
				break;
			case 12: // one MinMax (1)
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead1");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead2");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead3");
				p_builder.add(new PlayerFactory(MinMax_Player.class));    names.add("MinMax");
				break;
			case 13: // one Ahead vs
				p_builder.add(new PlayerFactory(MinMax_Player.class));    names.add("MinMax1");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead1");
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead2");
				p_builder.add(new PlayerFactory(MinMax_Player.class));    names.add("MinMax2");
				break;
			case 14: // one Ahead vs
				p_builder.add(new PlayerFactory(OneAhead_Player.class));    names.add("One Ahead1");
				p_builder.add(new PlayerFactory(MinMax_Player.class));    names.add("MinMax1");
				p_builder.add(new PlayerFactory(MinMax_Player.class));    names.add("MinMax2");
				p_builder.add(new PlayerFactory(MinMax_Player.class));    names.add("MinMax3");
				break;
			case 15: // minMax (slow)
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

/*
		int[][] orders = {{0,1,2,3},
		{1,2,3,0},
		{2,3,0,1},
		{3,0,1,2}};
		*/

		int[][] orders = {
		{0,1,2,3},
		{0,1,3,2},
		{0,2,1,3},
		{0,2,3,1},
		{0,3,1,2},
		{0,3,2,1},
	
		{1,0,2,3},
		{1,0,3,2},
		{1,2,0,3},
		{1,2,3,0},
		{1,3,2,3},
		{1,3,3,2},

		{2,0,1,3},
		{2,0,3,1},
		{2,1,0,3},
		{2,1,3,0},
		{2,3,0,1},
		{2,3,1,0},
		
		{3,0,1,2},
		{3,0,2,1},
		{3,1,0,2},
		{3,1,2,0},
		{3,2,0,1},
		{3,2,1,0},
		};
		// run games
		for( int x = 0; x < games; x++){
			int [] order = orders[ x % orders.length ] ;
			//System.out.format( "Game %d out of %d\n", x, games);
			System.out.format( ".");
			players = new Vector<Player>();
			for(int i = 0; i < 4; i++){
				players.add( p_builder.get(order[i]).getPlayer(rng ) );
			}

			Board b = Quoridor.run_game(players, false);
			Player_ID winner = b.compute_winner();

//			System.out.println("               Current   Walls  Distance");
//			System.out.println("               Location  Left    to Win");
			for(int i = 0; i < players.size(); i++){
//				System.out.format("%13s:  (%d,%d) %6d %7d\n", names.get(order[i]),
//					b.get_player_location(players_ids[i]).get_x_coordinate(),
//					b.get_player_location(players_ids[i]).get_y_coordinate(),
//					b.get_wall_count(players_ids[i]),
//					b.shortest_path(players_ids[i]) );

					wall_count[order[i]] += b.get_wall_count(players_ids[i]);
					distance[order[i]] += b.shortest_path(players_ids[i]) ;
			}

			if(winner == null){
				//System.out.println("Tie game.");
			}
			else{
				scores[order[winner.ordinal()]] += 1;
				//System.out.format("Player %d (%s) wins.\n", 
						//winner.ordinal() + 1, names.get(winner.ordinal()));
			}
			//System.out.println("");
		}

		System.out.println("");
		System.out.println("");
		System.out.println("===============RESULTS==================");
		System.out.println("                Wins    Walls  Distance");
		System.out.println("                        Left    to Win");
		for(int i = 0; i < players.size(); i++){
			System.out.format("%13s: %4d %8.2f %8.2f\n", names.get(i),
				scores[i], wall_count[i]*1.0/games, distance[i]*1.0/games );
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
			p.set_debug( false );
			return p;
		} catch ( Exception e ) {}
		return null;
	}
}
