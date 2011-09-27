package game;


import java.util.Vector;

/**
 * A parameterized class to represent a cell in a grid/graph structure.
 * 
 * Each cell contains one data item of type T.
 *
 * @param <T>
 */
public class Cell <T> {
	private T m_data;
	private int m_row;
	private int m_col;
	private Vector<Cell<T>> m_neighbors;
	
	public Cell(int row, int col, T data, Vector<Cell<T>> neighbors){
		m_data = data;
		m_row = row;
		m_col = col;
		m_neighbors = neighbors;
	}
	
	public Cell(int row, int col, T data){
		this(row, col, data, new Vector<Cell<T>>());
	}
	
	public int get_row(){
		return m_row;
	}
	
	public int get_col(){
		return m_col;
	}
	
	public T get_data(){
		return m_data;
	}
	
	public void set_data(T d){
		m_data = d;
	}
	
	public Vector<Cell<T>> get_neighbors(){
		return m_neighbors;
	}
	
	public void add_neighbor(Cell<T> neighbor){
		m_neighbors.add(neighbor);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(obj.getClass() == this.getClass()){
			Cell<T> other = (Cell<T>)obj;
			return other.m_col == m_col && other.m_row == m_row && m_data == other.m_data;
		}
		
		return false;
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 31 + m_col;
		hash = hash * 31 + m_row;
		return hash;
	}
}
