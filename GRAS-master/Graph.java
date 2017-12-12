import java.util.ArrayList;
import java.util.List;

public class Graph {
	
	// array containing the nodes of the graph. Node's id is its index in the array
	private Node[] G;			
	// list of nodes ordered by ascending degree
	List<Integer> sortedNodes; 
	
	// Class representing a single node 
	private class Node {
		
		// adjacency list ordered by descending weight
		private List<Adjacency> AdjacencyList;
		
		public Node()
		{
			AdjacencyList = new ArrayList<Adjacency>();
		}
		
		// add an adjacency to the node with the specified weight
		public void addAdjacency(int node, int weight)
		{
			int i=0;
			while(i<AdjacencyList.size() && weight<AdjacencyList.get(i).w)i++;
			AdjacencyList.add(i,new Adjacency(node,weight));
		}
		
		// return an array of adjacent nodes' id sorted by descending weight
		public int[] getAdjacencyList()
		{
			int[] res = new int[AdjacencyList.size()];
			for(int i=0;i<res.length;i++)
				res[i]=AdjacencyList.get(i).a;
			return res;
		}
		
		// remove the adjacency to a specific node
		public void removeAdjacency(int node)
		{
			int i=0;
			while(i<AdjacencyList.size() && AdjacencyList.get(i).a!=node) i++;
			if(i<AdjacencyList.size())
				AdjacencyList.remove(i);
		}
		
		public void removeAllAdjacency()
		{
			AdjacencyList.clear();
		}
		
		// get node degree
		public int getDegree() { return AdjacencyList.size(); }
	}
	
	// Class representing an adjacency
	private class Adjacency {
		int a;	// adjacent node
		int w;	// weight
		public Adjacency(int a, int w)
		{
			this.a=a;
			this.w=w;
		}
	}
	
	// Graph constructor
	// n = number of nodes
	public Graph(int n)
	{
		G=new Node[n];
		for(int i=0;i<n;i++)
			G[i]=new Node();
	}
	
	// add a weighted edge to the graph
	public void addEdge(int n1, int n2, int weight)
	{
		sortedNodes = null;
		G[n1].addAdjacency(n2, weight);
		G[n2].addAdjacency(n1, weight);
	}

	// sort nodes by ascending degree
	private void sortNodes()
	{
		sortedNodes = new ArrayList<Integer>();
		sortedNodes.add(0);
		for(int i=1;i<G.length;i++)
			sortedNodes.add(searchDegreePosition(G[i].getDegree(),0,sortedNodes.size()), i);
	}
	
	// get the starting index of a degree in the sorted nodes list
	private int searchDegreePosition(int degree, int from, int to)
	{
		if(sortedNodes.size() == 0)
			return 0;
		
		int position = (from+to)/2;
		
		if(from>=to || G[sortedNodes.get(position)].getDegree()==degree)
		{
			if(from>=to)
				position=from;
			
			while(position>0 && G[sortedNodes.get(position-1)].getDegree()>=degree) position--;
			while(position<sortedNodes.size() && G[sortedNodes.get(position)].getDegree()<degree) position++;
			return position;
		}
		
		if(G[sortedNodes.get(position)].getDegree()>degree)
			return searchDegreePosition(degree,from,position-1);
		else
			return searchDegreePosition(degree,position+1,to);
	}
	
	// search the position of a node in the sorted nodes list
	private int searchNodePosition(int n)
	{
		int d = G[n].getDegree();
		int i=searchDegreePosition(d,0,sortedNodes.size()); // get the first position where the degree of the node appears
		// find the node
		while(i<sortedNodes.size() && G[sortedNodes.get(i)].getDegree()==d && sortedNodes.get(i)!=n)
			i++;
		
		if(i<sortedNodes.size() && sortedNodes.get(i)==n)
			return i;
		return -1;
	}
	
	// remove n2's adjacency in n1
	private void removeAdjacency(int n1, int n2)
	{
		// get the position of n1 in the sorted list
		int pos = searchNodePosition(n1);
		// remove the adjacency
		G[n1].removeAdjacency(n2);
		// remove the node from the list
		sortedNodes.remove(pos);
		// find the new position of the node in the sorted list
		int newPosition = searchDegreePosition(G[n1].getDegree(),0,sortedNodes.size());
		sortedNodes.add(newPosition,n1);
	}
	
	// return the id of the node with max degree
	public int getNodeWithMaxDegree()
	{
		if(sortedNodes==null)	// if the list isn't initialized
			sortNodes();		// initialize it
		
		if(sortedNodes.size()>0)
			return sortedNodes.get(sortedNodes.size()-1);	// the node is the last of the sorted list
		else
			return -1;
	}
	
	// remove the node n from the graph
	public void removeNode(int n)
	{
		int pos = searchNodePosition(n);	// get node's position in the sorted list
		if(pos>=0){
			sortedNodes.remove(pos);	// remove from the list
			int[] list = G[n].getAdjacencyList();
			// remove the node from each adjacent node's adjacency list
			for(int i=0;i<list.length;i++)
			{
				// remove the adjacency
				removeAdjacency(list[i], n);
				// and update the sorted list
				pos = searchNodePosition(list[i]);
				G[list[i]].removeAdjacency(n);
				sortedNodes.remove(pos);
				sortedNodes.add(searchDegreePosition(G[list[i]].getDegree(),0,sortedNodes.size()),list[i]);
			}
			G[n].removeAllAdjacency();
		}
	}
	
	// remove the edge between two nodes
	public void removeEdge(int n1, int n2)
	{
		removeAdjacency(n1, n2);
		removeAdjacency(n2, n1);
	}
	
	// return the nodes in n's adjacent list
	public int[] getAdjacentList(int n)
	{
		return G[n].getAdjacencyList();
	}
}
