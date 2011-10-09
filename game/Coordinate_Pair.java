package game;

/**
 * A generic class for maintaining a set of coordinates mapping into
 * a coordinate graph.
 *
 */
public class Coordinate_Pair {
	private int y; // row
	private int x; // col
	
	public Coordinate_Pair(int f, int s){
		this.y = f;
		this.x = s;
	}
	
	public int get_y_coordinate(){
		return y;
	}
	
	public int get_x_coordinate(){
		return x;
	}
	
	public int hashCode() {
        //int hashFirst = y != null ? y.hashCode() : 0;
        //int hashSecond = x != null ? x.hashCode() : 0;

        return (x) + (20 * y);
    }
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		if(!(o instanceof Coordinate_Pair))
			return false;
		Coordinate_Pair other = (Coordinate_Pair) o;
		
		return this.y == other.y && this.x == other.x;
	}
}
