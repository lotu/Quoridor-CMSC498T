package game;


import java.util.HashMap;
import java.util.Vector;

/**
 * A simple implementation of a rectangular grid. 
 */
public class RectangularGrid <T> {
	private Cell<T> m_cells[][];
	
	/**
	 * Creates a rectangular grid with the provided initial data in the respective
	 * grid locations.
	 * 
	 * @param rows
	 * @param cols
	 * @param cells
	 */
	@SuppressWarnings("unchecked")
	public RectangularGrid(int rows, int cols, T[][] cells){
		m_cells = new Cell[rows][cols];
		
		//instantiate all of the cells w/o neighbors
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				m_cells[i][j] = new Cell<T>(i, j, cells[i][j]);
			}
		}
		
		//now that all cells are created we can populate the neighbors
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				// up one cell
				if(i - 1 >= 0){
					m_cells[i][j].add_neighbor(m_cells[i-1][j]);
				}
				// down one cell
				if(i+1 < rows){
					m_cells[i][j].add_neighbor(m_cells[i+1][j]);
				}
				
				// left one cell
				if(j - 1 >= 0){
					m_cells[i][j].add_neighbor(m_cells[i][j-1]);
				}
				// right one cell
				if(j+1 < rows){
					m_cells[i][j].add_neighbor(m_cells[i][j+1]);
				}
			}
		}
	}
	
	public Cell<T> get_cell(int row, int col){
		return m_cells[row][col];
	}
	
	/**
	 * performs dijkstra's shortest path algorithm on the graph
	 *
	 * returns a hashmap where the entry cor an individual cell<t> i is the
	 * length of the shortest path from the specified source node to node i.
	 */
	public HashMap<Cell<T>, Integer> dijkstra(int row, int col){
		Vector<Cell<T>> queue = new Vector<Cell<T>>();
		HashMap<Cell<T>, Integer> distances = new HashMap<Cell<T>, Integer>();
		HashMap<Cell<T>, Boolean> visited = new HashMap<Cell<T>, Boolean>(); 
		for(int i = 0; i < m_cells.length; i++){
			for(int j = 0; j < m_cells[i].length; j++){
				queue.add(m_cells[i][j]);
				distances.put(m_cells[i][j], Integer.MAX_VALUE);
			}
		}

		distances.put(m_cells[row][col],  0);

		while(queue.size() > 0){
			// find the vertex v with the smallest dist[v]
			int v = 0;
			for(int i = 0; i < queue.size(); i++){
				if(distances.get(queue.get(i)) < distances.get(queue.get(v))){
					v = i;
				}
			}

			//break because all remaining nodes are unreachable from source
			if(distances.get(queue.get(v)) == Integer.MAX_VALUE){
				break;
			}

			Cell<T> u = queue.get(v);
			visited.put(u, true);
			queue.remove(v);

			//update all non-visited neighbors of u
			for(Cell<T> neighbor : u.get_neighbors()){
				int alt = distances.get(u) + 1;
				if(alt < distances.get(neighbor)){
					distances.put(neighbor, alt);
				}
			}
		}

		return distances;
	}
	
	/**
	 * Test driver.
	 * 
	 * @param args - none - ignored
	 */
	public static void main(String[] args){
		int rows = 9, cols = 9;
		Integer[][] data = new Integer[rows][cols];
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++)
				data[i][j] = i * cols + j;
		}
		
		RectangularGrid<Integer> grid = new RectangularGrid<Integer>(rows, cols, data);
		
		for(int i = 0; i < rows; i++){
			for(int j = 0; j < cols; j++){
				Vector<Cell<Integer>> neighbors = grid.get_cell(i,j).get_neighbors();
				System.out.print("Cell " + (i * cols + j) + " has " + neighbors.size() + " neighbors: ");
				for(int k = 0; k < neighbors.size(); k++){
					System.out.print(neighbors.get(k).get_data() + " ");
				}
				System.out.println();
			}
		}
	}
}
