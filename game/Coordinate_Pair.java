package game;

/**
 * A generic class for maintaining a set of coordinates mapping into
 * a coordinate graph.
 *
 * @param <T>
 * @param <S>
 */
public class Coordinate_Pair <T,S>{
	private T y;
	private S x;
	
	public Coordinate_Pair(T f, S s){
		this.y = f;
		this.x = s;
	}
	
	public T get_y_coordinate(){
		return y;
	}
	
	public S get_x_coordinate(){
		return x;
	}
	
	public int hashCode() {
        int hashFirst = y != null ? y.hashCode() : 0;
        int hashSecond = x != null ? x.hashCode() : 0;

        return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }
	
	@SuppressWarnings("unchecked")
	public boolean equals(Object o){
		if(!(o instanceof Coordinate_Pair))
			return false;
		Coordinate_Pair other = (Coordinate_Pair) o;
		
		return this.y.equals(other.y) && this.x.equals(other.x);
	}
}
