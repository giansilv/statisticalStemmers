
/**
 * @author Andrea Langeli, Giacomo Rocco
 * @version 29-03-17
 */

//standard library
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.HashSet;
import java.util.Iterator;

//fastutil library
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

//jgrapht library
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;

public class SnsTool 
{
	// parameters l1 and l2.
	int l1, l2;

	/**
	 * It simply initializes the private variables l1 and l2 with the input
	 * values.
	 * 
	 * @param paramL1
	 * @param paramL2
	 */

	public SnsTool(int paramL1, int paramL2) 
	{
		l1 = paramL1;
		l2 = paramL2;
	}

	/**
	 * This method is responsible of carrying out the stemming process. This is
	 * made by several procedures which are invoked one at a time, in the right
	 * order. First of all, the lexicon and inverted files are read in order to
	 * associate each index term to a IndexElement object. This object maintains
	 * for each document of the input collection the number of occurrences of
	 * that term for that document. This operation is executed in the readAll
	 * method. Then for each couple of descriptors, the cooccurrence measure is
	 * evaluated. This is equal to the sum of the minimum frequency between the
	 * two terms for each document that contains both the descriptors. This
	 * calculation is done in arcs_nodes_evalutation method. Then in the
	 * creationGraph method, a simple weighted graph is built from the
	 * informations obtained in the previous methods. The following procedure is
	 * the update of the weights of the arcs of the graph, based on the data
	 * associated with the nodes that are directly connected with the ones that
	 * are related to each other by the selected edge. Then the addStrongEdge
	 * method removes from the graph the edges that are not strong edges.
	 * Finally the connected components are found in the findConnectedComponents
	 * method and the output of the stemming process is obtained.
	 * 
	 * @param lexicon
	 * @param inverted
	 * @param expected_elements
	 * @throws FileNotFoundException
	 * @throws IOException
	 */

	public void executeSnsStemmer(String lexicon, String inverted, int expected_elements) throws FileNotFoundException, IOException
	{

		long startTime = System.currentTimeMillis();

		PrintWriter time = new PrintWriter("execution_time.txt", "UTF-8");

		//It stores an IndexElement for each index term. This is filled up with
		//IndexElement objects in the readAll method.
		Int2ObjectOpenHashMap<IndexElement> map = new Int2ObjectOpenHashMap<IndexElement>(expected_elements);

		//The key of an entry is made by the concatenation of the suffixes of a
		//couple of index terms which share the same prefix, according to the
		//lexicographic order. The length of the common prefix must be longer
		//or equal to l1. This Map is used in the arcs_nodes_evaluation method.
		Object2ObjectOpenHashMap<String, L2_element> hM_l2 = new Object2ObjectOpenHashMap<String, L2_element>();

		//Linked list of Arc objects. arcs_nodes_evaluation creates an Arc
		//object only for the couples of index_terms that share a common prefix
		//with length greater or equal to l1.
		Object2ObjectOpenHashMap<String, Arc> arcs = new Object2ObjectOpenHashMap<String, Arc>();

		//LinkedList of the index terms that belong at least to an Arc object.
		LinkedList<String> nodes = new LinkedList<String>();

		//Its entries are made by an index term and all the edges that have
		//that index term as one of the extremities of the Arc object.
		Object2ObjectOpenHashMap<String, ObjectArrayList<DefaultWeightedEdge>> arc_hash_sets = new Object2ObjectOpenHashMap<String, ObjectArrayList<DefaultWeightedEdge>>(
				expected_elements);

		
		long startReadingTime = System.currentTimeMillis();

		//Reading from the lexicon.txt and inverted.txt files. 
		//The expected number of elements of the map is equal to expected_elements.
		readAll(map, lexicon, inverted, expected_elements);

		long endReadingTime = System.currentTimeMillis();

		time.print("Time for the reading from files \t = \t" + (endReadingTime - startReadingTime) / 1000.0 + " seconds\n");
		System.out.println("Reading is finished");
		
		
		
		long startEvalTime = System.currentTimeMillis();

		arcs_nodes_evalutation(map, hM_l2, arcs, nodes);

		long endEvalTime = System.currentTimeMillis();

		time.print("Computation of the cooccurrence measure\t = \t" + (endEvalTime - startEvalTime) / 1000.0 + " seconds\n");

		System.out.println("Computation of the cooccurrence is finished");
		
		

		long startGraphTime = System.currentTimeMillis();

		SimpleWeightedGraph<String, DefaultWeightedEdge> graph = new SimpleWeightedGraph<String, DefaultWeightedEdge>(DefaultWeightedEdge.class);
		
		creationGraph(graph, arcs);

		long endGraphTime = System.currentTimeMillis();

		time.print("Graph building\t = \t" + (endGraphTime - startGraphTime) / 1000.0 + " seconds\n");
		System.out.println("End of the construction of the graph");

		
		
		long startReWTime = System.currentTimeMillis();

		reWeighting(arc_hash_sets, graph, expected_elements, nodes, arcs);

		long endReWTime = System.currentTimeMillis();

		time.print("ReWeighting of the graph \t = \t" + (endReWTime - startReWTime) / 1000.0 + " seconds\n");
		System.out.println("Reweighted graph");
		
		

		long startStrongTime = System.currentTimeMillis();

		addStrongEdges(arc_hash_sets, graph, nodes, arcs);

		long endStrongTime = System.currentTimeMillis();

		time.print("Removal of some edges\t = \t" + (endStrongTime - startStrongTime) / 1000.0 + " seconds\n");
		System.out.println("Creation of the graph with only Strong edges");
		
		

		long startLTTime = System.currentTimeMillis();

		ConnectivityInspector<String, DefaultWeightedEdge> connected_components = new ConnectivityInspector<String, DefaultWeightedEdge>(graph);
		
		List<Set<String>> list_of_components = connected_components.connectedSets();

		TreeSet<String[]> orderedLookupTable = new TreeSet<String[]>(new LookupComparator());

		findConnectedComponents(list_of_components, orderedLookupTable, connected_components);
		
		long endTime = System.currentTimeMillis();

		time.print("LT creation\t = \t" + (endTime - startLTTime) / 1000.0 + " seconds\n");
		time.print("Total time spent\t = \t" + (endTime - startTime) / 1000.0 + " seconds\n");
		time.close();

	}
	
	/**
	 * It stores a set of index terms and their term-frequencies in an open HashMap.
	 * It considers two text files containing a set of index terms and their
	 * term frequencies and for each of them it verifies if its first character
	 * is actually a digit or if its length is less than l1. If it is the case
	 * the index term is not stored in the HashMap and it won't be considered in
	 * the following phases of the construction of the SNS stemmer. If the
	 * length of an index term is less than l1 it can't have a common prefix of
	 * length greater than (or equal to) l1 with any of the other index terms, 
	 * so it would be discarded in the successive methods. If the first character 
	 * of the index term is a digit, the corresponding index term results from a
	 * typing error, so it isn't considered anymore.
	 * 
	 * @param map HashMap using open addressing strategy.
	 * @param lexicon filename of the lexicon.txt file.
	 * @param inverted filename of the inverted.txt file.
	 * @throws FileNotFoundException
	 * @throws IOException
	 */

	private void readAll(Int2ObjectOpenHashMap<IndexElement> map, String lexicon, String inverted, int expected_elements) throws FileNotFoundException, IOException 
	{
		
		//Key values for the input map
		int key_index = 0;

		int elements_div = expected_elements / 10;
		int status = expected_elements / elements_div;
		
		//Text files are read as streams of characters by means of FileReader
		//and BufferedReader objects.
		FileReader reader_lex = new FileReader("text_files/" + lexicon);
		FileReader reader_inv = new FileReader("text_files/" + inverted);

		BufferedReader buffer_reader_lex = new BufferedReader(reader_lex);
		BufferedReader buffer_reader_inv = new BufferedReader(reader_inv);

		//It will store single lines from lexicon.txt
		String line_lex = "";
		
		//It will store single lines from inverted.txt
		String line_inv = "";
		
		//It will store an index term
		String word = "";

		//The first row is not useful.
		buffer_reader_lex.readLine();
		buffer_reader_inv.readLine();
		buffer_reader_lex.readLine();
		buffer_reader_inv.readLine();

		while (true) 
		{

			line_lex = buffer_reader_lex.readLine();
			line_inv = buffer_reader_inv.readLine();

			//This if-statement allows to avoid a
			//java.lang.NullPointerException
			if (line_lex == null) 
			{
				break;
			}
			else 
			{
				Scanner scanner_line_lex = new Scanner(line_lex);

				//scanner_line_object will use "," to identify tokens in the
				//considered line.
				scanner_line_lex.useDelimiter(",");

				//We are only interested in the first word.
				word = scanner_line_lex.next();

				if (word.length() < 3) 
				{
				} 
				else 
				{
					try 
					{
						//If the first character of the index term is a numeric digit,
						//the index term is not useful in the further phases of the construction
						//of the SnS stemmer. 
						Integer.parseInt(word.substring(0, 1));
						
						continue;
					} 
					catch (NumberFormatException e) 
					{
						Scanner scanner_line_inv = new Scanner(line_inv);
						
						
						//It is not useful, it is the index of the index term
						//in the inverted.txt file
						//scanner_line_inv.nextInt();
						scanner_line_inv.nextInt();
						
						//From now to the end of the line we consider as valid
						//only integer values
						scanner_line_inv.useDelimiter("[^0-9]+");
						
						LinkedList<WordStat> postingList = new LinkedList<WordStat>();

						while (scanner_line_inv.hasNextInt()) 
						{
							//At each iteration a single couple of id document
							//and term frequency is considered.
							int doc = scanner_line_inv.nextInt();
							int tf = scanner_line_inv.nextInt();
							
							WordStat ws = new WordStat(doc, tf);
							postingList.add(ws);
						}

						/*
						* There is an IndexElement object only for the index
						* terms that at this stage are considered as
						* potentially useful in the definition of the stems.
						*/
						IndexElement indexElement = new IndexElement(word, postingList);

						map.put(key_index++, indexElement);

						if (key_index % elements_div == 0) 
						{
							System.out.println("Reading: " + (int)(((key_index / elements_div) / (double) status) * 100) + "%");
						}

						scanner_line_inv.close();
					}

				}

				scanner_line_lex.close();

			}
		}

		buffer_reader_lex.close();
		reader_lex.close();
		buffer_reader_inv.close();
		reader_inv.close();

	}
	

	/**
	 * It receives two String objects and it returns their common prefix. If it
	 * does not exist it returns null.
	 * 
	 * @param w1 a String object
	 * @param w2 a String object
	 * @return the longest common prefix
	 */

	private String commonPrefix(String w1, String w2) 
	{
		int minLength = Math.min(w1.length(), w2.length());
		
		for (int i = 0; i < minLength; i++) 
		{
			if (w1.charAt(i) != w2.charAt(i)) 
			{
				return w1.substring(0, i);
			}
		}
		return w1.substring(0, minLength);
	}

	/**
	 * It returns the suffix of a String object, given the longest common prefix
	 * evaluated in relation to an other String object.
	 * 
	 * @param word a String object.
	 * @param prefix a prefix of word.
	 * @return suffix a suffix of word.
	 */

	private String mySuffix(String word, String prefix) 
	{
		int prefixLength = prefix.length();
		int wordLength = word.length();

		String suffix = word.substring(prefixLength, wordLength);
		return suffix;

	}

	/**
	 * It returns the length of a String object if it isn't null, otherwise it
	 * returns 0.
	 * 
	 * @param commonPrefix a String object
	 * @return the length of commonPrefix if it isn't null, 0 otherwise.
	 */

	private int getLengthPrefix(String commonPrefix) 
	{
		if (commonPrefix == null) 
		{
			return 0;
		}
		
		return commonPrefix.length();
	}

	/**
	 * It takes into account 2 String objects and it concatenates them according
	 * to the lexicographic order. This String object will be used as key in a
	 * HashMap.
	 * 
	 * @param string1 a String object.
	 * @param string2 a String object.
	 * @return key a String object, concatenation of the 2 provided String objects
	 */
	
	private String getKeyL2(String string1, String string2) 
	{
		String key = "";
		
		if (string1.compareTo(string2) <= 0) 
		{
			key = string1 + "###" + string2;
		}
		else 
		{
			key = string2 + "###" + string1;
		}

		return key;
	}	
	
	/**
	 * This method receives 2 LinkedLists associated with two index terms and it
	 * returns the corresponding co-occurrence measure. This value is equal to
	 * the sum of the minimum of the term frequencies of the selected index
	 * terms evaluated for each document of the collection. If an index term
	 * doesn't appear in a document, its term frequency for that document is
	 * equal to zero and in this case the contribution of the two index terms to
	 * the co-occurrence value for that document also is equal to zero.
	 * 
	 * @param l1 LinkedList of WordStat objects.
	 * @param l2 LinkedList of WordStat objects.
	 * @return coOccurrenceValue value of co-occurrence for two index terms.
	 */
		
	private int cooccurrence2terms(LinkedList<WordStat> linklist1, LinkedList<WordStat> linklist2) 
	{
		//Co-occurrence value
		int coOccurrenceValue = 0;

		//LinkedLists of WordStat objects associated with two index terms
		ListIterator<WordStat> list1 = linklist1.listIterator();
		ListIterator<WordStat> list2 = linklist2.listIterator();

		//First couples of WordStat objects
		WordStat w1 = list1.next();
		WordStat w2 = list2.next();

		while (true) 
		{
			if (w1.getDoc() == w2.getDoc()) 
			{
				//Each of the index terms is in the document.
				coOccurrenceValue += Math.min(w1.getFrequency(), w2.getFrequency());

				if (list1.hasNext() && list2.hasNext()) 
				{
					w1 = list1.next();
					w2 = list2.next();
				} 
				else 
				{
					//If list1 or list2 has no elements left, the
					//computation of the co-occurrence is finished.
					break;
				}

			} 
			else if (w1.getDoc() < w2.getDoc()) 
			{
				/*
				 * If the first index term is in a document but the latter one
				 * is not, the WordStat objects associated with the first one
				 * are ignored until there is one with an id that is equal to
				 * the id of the following WordStat object in the LinkedList
				 * associated with the latter index term.
				 */

				if (list1.hasNext()) 
				{
					w1 = list1.next();
				} 
				else 
				{
					//list1 has no elements left, the computation is finished.
					break;
				}

			} 
			else 
			{
				if (list2.hasNext())
				{
					w2 = list2.next();
				} 
				else 
				{
					//list2 has no elements left, the computation is finished.
					break;
				}
			}

		}

		return coOccurrenceValue;

	}	
	
	/**
	 * Calculates the co-occurence between word pairs. Stores the information
	 * concerning the length of the common prefix in the HashMap hM_l2, checking
	 * if the length is greater than or equal to l2, or only greater than or
	 * equal to l1. These information are used to select nodes and edges to be
	 * added to the graph using the method save_nodes_arcs.
	 * 
	 * @param map HashMap using open addressing strategy.
	 * @param hM_l2 HashMap of L2_element using String as index.
	 * @param arcs HashMap of Arcs using String as index.
	 * @param nodes LinkedList of nodes.
	 */

	private void arcs_nodes_evalutation(Int2ObjectOpenHashMap<IndexElement> map, Object2ObjectOpenHashMap<String, L2_element> hM_l2, Object2ObjectOpenHashMap<String, Arc> arcs, LinkedList<String> nodes) 
	{
		ObjectArrayList<String> arcs_retrieval = new ObjectArrayList<String>();
 
 		int numColumns = map.size() - 1;
		int numColumns_div = numColumns / 25;
		int status = numColumns / numColumns_div;
		
		for (int indexCols = 0; indexCols <= numColumns - 1; indexCols++) 
		{
			if (indexCols % numColumns_div == 0) 
			{
				//approximate status print
				System.out.println("Cooccurence calculation: " + (int)(((indexCols / numColumns_div) / (double) status) * 100) + "%");
			}
			for (int indexRows = 0; indexRows <= indexCols; indexRows++) 
			{

				//First index term
				String index_term1 = (map.get(indexCols + 1)).getIEString();
				
				//Latter index term
				String index_term2 = (map.get(indexRows)).getIEString();

				//Longest common prefix.
				String prefix = commonPrefix(index_term1, index_term2);

				//Length of the common prefix
				int prefix_length = getLengthPrefix(prefix);
				
				//Check if the prefix length is greater than l1
				//if it is not the case, we can skip this couple of word
				//because are considered not sematically related
				
				if (prefix_length >= l1)
				{
					
					int co = cooccurrence2terms((map.get(indexCols + 1)).getIEPostingList(), (map.get(indexRows)).getIEPostingList());
					
					/*
					 * This statement verifies if the co-occurrence of the 2
					 * index_terms is greater than 0. If it is not the case, we
					 * won't consider again this couple of index_terms in the
					 * construction of the arcs because with high probability 
					 * they aren't semantically related. 
					 */
					
					if (co != 0) 
					{
					
						String suffix_index_term1 = mySuffix(index_term1, prefix);
						
						//Suffix of the latter index term after the elimination of
						//the common prefix.
						String suffix_index_term2 = mySuffix(index_term2, prefix);

						Arc potential_arc = new Arc(index_term1, index_term2, co);

						/*
						 * Construction of a single String object from the two
						 * suffixes. This string does not depend on the order of
						 * consideration of the two String objects. This is
						 * important because we will use this String as key in a
						 * Map, therefore 2 couples of Strings that contains the
						 * same Strings but in different order will point to the
						 * same bucket.
						 */

						String key_l2 = getKeyL2(suffix_index_term1, suffix_index_term2);
						L2_element element_l2 = hM_l2.get(key_l2);

						if (prefix_length > l2) 
						{
							if (element_l2 == null) 
							{
								
							 /*
								* There isn't an L2 element with the same key of that
								* associated with the selected couple of index terms, so
								* a new one is created and its counter is increased by a unit.
								*/
								
								L2_element element_new_l2 = new L2_element();
								element_new_l2.addCounter();
								element_new_l2.addArc(potential_arc);
								hM_l2.put(key_l2, element_new_l2);
								
							} 
							else 
							{
							 
								//An L2_element with the same key already exists so we only have 
								//to add a unit to its counter.  
								element_l2.addCounter();
								element_l2.addArc(potential_arc);
								arcs_retrieval.add(key_l2);
							}
						} 
						else 
						{
							if (element_l2 == null) 
							{
								/*
								 * In this case we have to create a new L2 element and to add
								 * the Arc object, obtained from the two index terms, to its
								 * LinkedList. The selected descriptors haven't a common prefix
								 * of length greater of equal to l2 so the counter must be kept at 0.
								 */

								L2_element element_new_l2 = new L2_element();
								element_new_l2.addArc(potential_arc);
								hM_l2.put(key_l2, element_new_l2);
							} 
							else 
							{
								//We have only to add the arc to the 
								//LinkedList of element_l2 object.
								element_l2.addArc(potential_arc);
							}

						}

					}
				}
			}

		}

		save_nodes_arcs(hM_l2, arcs_retrieval, arcs, nodes);

	}
	
	/**
	 * Stores all edges and nodes to be included in the graph respectively in
	 * the HashMap arcs and in the LinkedList nodes. It checks for each key of
	 * HashMap hM_l2 if the associated counter has a value greater than or equal
	 * to 2. If it is the case every Arc object in the the list of L2_element 
	 * is added to HashMap arcs. Nodes connected by this edges are stored in the
	 * LinkedList nodes (one time only).
	 *
	 * @param hM_l2 HashMap of L2_element.
	 * @param set_keys ArrayList containing all hM_l2's keys.
	 * @param arcs HashMap of Arcs using String as index.
	 * @param nodes LinkedList of nodes.
	 */

	private void save_nodes_arcs(Object2ObjectOpenHashMap<String, L2_element> hM_l2, ObjectArrayList<String> set_keys, Object2ObjectOpenHashMap<String, Arc> arcs, LinkedList<String> nodes) 
	{
		//We use an HashSet to insert an index term in the HashMap nodes only one time. 
    HashSet<String> unique_nodes = new HashSet<String>();
		for (String key : set_keys) 
		{
			L2_element element_l2 = hM_l2.get(key);
			
			//We consider only the entries of hM_l2 with counter greater or equal to 2.
			if (element_l2.getCounter() >= 2) 
			{
				for (Arc arc : element_l2.getArcs())
				{

					String row_word = arc.getWordRow();
					String column_word = arc.getWordColumn();
					
					//The index terms are used to form the keys of the HashMap arcs.
					arcs.put(getKeyL2(row_word, column_word), arc);
					
					//If row_not_inserted is false the index term has already been inserted in the LinkedList nodes.
					boolean row_not_inserted = unique_nodes.add(row_word);
					
				  //If column_not_inserted is false the index term has already been inserted in the LinkedList nodes.
					boolean column_not_inserted = unique_nodes.add(column_word);

					if (row_not_inserted) 
					{
						nodes.add(row_word);
					}
					if (column_not_inserted)
					{
						nodes.add(column_word);
					}
				}
			}
		}
	}

	/**
	 * This method builds a simple weighted graph from an empty SimpleWeightedGraph object and the entries of the HashMap arcs. 
	 * In particular each element of that HashMap corresponds to an edge of the simple weighted graph. 
	 * 
	 * @param graph a SimpleWeightedGraph object.
	 * @param arcs an HashMap with entries made by a String object and an Arc object.
	 */
	
	private void creationGraph(SimpleWeightedGraph<String, DefaultWeightedEdge> graph, Object2ObjectOpenHashMap<String, Arc> arcs) 
	{
		for (Arc arc : arcs.values()) 
		{
			String word1 = arc.getWordRow();
			String word2 = arc.getWordColumn();

			graph.addVertex(word1);
			graph.addVertex(word2);
			
			
			DefaultWeightedEdge e1 = graph.addEdge(word1, word2);
		
			//The weight of the edge is equal to the co-occurence measure of the index terms that make the edge.
			graph.setEdgeWeight(e1, arc.getCooccurrence());


		}
	}		
	
	/**
	 * Given an edge and one of its nodes this method returns the other node of
	 * the edge.
	 *
	 * @param edge a DefaultWeightedEdge object, an arc of the graph.
	 * @param graph a SimpleWeightedGraph object.
	 * @param node a String object.
	 * @return a String object representing the node of the DefaultWeightedEdge "edge" that is different from the String "node".
	 */

	private String otherNode(DefaultWeightedEdge edge, SimpleWeightedGraph<String, DefaultWeightedEdge> graph, String node) 
	{
		String source = graph.getEdgeSource(edge);
		String target = graph.getEdgeTarget(edge);

		if (source.equals(node)) 
		{
			return target;
		}

		return source;

	}	
	
	/**
	 * Updates the weights of the edges of the graph. Given an Edge (a,b) and
	 * found the set of common neighbors of a and b, for each node w belonging
	 * to this set, the minimum value of cooccurence between the edge (a,w) and
	 * the edge (w,b) is added to the weight of (a,b), multiplied by 0,5.
	 *
	 *
	 * @param arc_hash_sets an HashMap of ArrayList of edges using String as index.
	 * @param graph a SimpleWeightedGraph object, the graph.
	 * @param number_nodes an Integer, the number of nodes of the graph.
	 * @param nodes a LinkedList of nodes.
	 * @param arcs an HashMap of Arcs using String as index.
	 */

	private void reWeighting(Object2ObjectOpenHashMap<String, ObjectArrayList<DefaultWeightedEdge>> arc_hash_sets, SimpleWeightedGraph<String, DefaultWeightedEdge> graph, int number_nodes, LinkedList<String> nodes, Object2ObjectOpenHashMap<String, Arc> arcs)
	{
  	for (String node : nodes) 
  	{
  		//Set of edges that have node as one of the extremities
			ObjectArrayList<DefaultWeightedEdge> edges_node = new ObjectArrayList<DefaultWeightedEdge>();

			for (DefaultWeightedEdge edge : graph.edgesOf(node)) 
			{
				edges_node.add(edge);
			}

			//We have an entry for each node of the SimpleWeightedGraph graph
			arc_hash_sets.put(node, edges_node);
		}

		for (Arc arc : arcs.values()) 
		{
			String a = arc.getWordRow();
			String b = arc.getWordColumn();

			ObjectOpenHashSet<String> nodi = new ObjectOpenHashSet<String>();

			for (DefaultWeightedEdge edge_a : arc_hash_sets.get(a)) 
			{
				//Set of nodes of graph that share an edge with the node a.
				nodi.add(otherNode(edge_a, graph, a));
			}

			double partial_rco = 0;

			for (DefaultWeightedEdge edge_b : arc_hash_sets.get(b))
			{
				//The extremity of the edge other than b
				String other_b = otherNode(edge_b, graph, b);
				
				/*
				 * If flag is false means that other_b is a common neighbor of a and b. 
				 * The weights of [a,other_b] and [other_b, b] will be used to update the weight
				 * of the edge [a,b]
				 */

				boolean flag = nodi.add(other_b);
				if (flag == false)
				{
					partial_rco += Math.min(arcs.get(getKeyL2(a, other_b)).getCooccurrence(), arcs.get(getKeyL2(b, other_b)).getCooccurrence());
				}

			}
      
			//The value 0.5 was chosen arbitrarily.
			double rco = arc.getCooccurrence() + partial_rco * 0.5;
			
			//The weight of the edge [a,b] has been modified.
			graph.setEdgeWeight(graph.getEdge(a, b), rco);
			
			//The new value rco is stored in the Arc object associated with a and b. 
			arcs.get(getKeyL2(a, b)).setRco(rco);
		}

	}	
	
	/**
	 * Removes from the graph all the edges that are not StrongEdges. An edge
	 * (a,b) is a StrongEdge if the weight of the edge is the maximum between
	 * all the weights of the edges (a,w), where w is a neighbor of a OR it is
	 * the maximum between all the weights of the edges (b,v), where v is a
	 * neighbor of b. An edge (a,b) can be removed if its weight is less than
	 * the maximum weight of edges (a,w), where w is a neighbor of a AND its
	 * weight is less than the maximum weight of edges (b,v), where v is a
	 * neighbor of b.
	 *
	 * @param arc_hash_sets an HashMap of ArrayList of edges using String as index.
	 * @param graph a SimpleWeightedGraph object, the graph.
	 * @param nodes a LinkedList of nodes.
	 * @param arcs an HashMap of Arcs using String as index.
	 * @throws ConcurrentModificationException
	 */
	
	private void addStrongEdges(Object2ObjectOpenHashMap<String, ObjectArrayList<DefaultWeightedEdge>> arc_hash_sets, SimpleWeightedGraph<String, DefaultWeightedEdge> graph, LinkedList<String> nodes, Object2ObjectOpenHashMap<String, Arc> arcs) throws java.util.ConcurrentModificationException
	{
		//Each entry stores the maximum weight of the edges incident to a vertex.
		Object2DoubleOpenHashMap<String> map_max_rco = new Object2DoubleOpenHashMap<String>(nodes.size());

		for (String vertex : nodes) 
		{
			double weight = 0;
			double e_weight = 0;

			for (DefaultWeightedEdge e : arc_hash_sets.get(vertex)) 
			{
				String a = graph.getEdgeSource(e);
				String b = graph.getEdgeTarget(e);

				e_weight = arcs.get(getKeyL2(a, b)).getRco();
				weight = Math.max(weight, e_weight);
			}

			map_max_rco.put(vertex, weight);
		}

		//ArrayList that contains the edged which will be removed from the graph. 
		ObjectArrayList<DefaultWeightedEdge> edges_to_remove = new ObjectArrayList<DefaultWeightedEdge>();

		for (DefaultWeightedEdge edge : graph.edgeSet()) 
		{

			String a = graph.getEdgeSource(edge);
			String b = graph.getEdgeTarget(edge);

			double rco_edge = graph.getEdgeWeight(edge);
			
			//The weight of each edge is compared to the map_map_rco value of each of the two vertices. 
			if ((rco_edge < map_max_rco.getDouble(a)) && (rco_edge < map_max_rco.getDouble(b))) 
			{
				edges_to_remove.add(edge);
			}

		}

		//The edges that won't be removed from the graph are StrongEdges.
		for (DefaultWeightedEdge edge : edges_to_remove)
		{
			graph.removeEdge(edge);
		}

	}
	
	/**
	 * This method creates the output file of the stemming process. It associates each index 
	 * term with the stem that represents the elements of the connected component to which it
	 * belongs to. 
	 * 
	 * @param list_of_components
	 * @param orderedLookupTable
	 * @param connected_components
	 * @throws FileNotFoundException
	 * @throws UnsupportedEncodingException
	 */
	
	private void findConnectedComponents(List<Set<String>> list_of_components, TreeSet<String[]> orderedLookupTable, ConnectivityInspector<String, DefaultWeightedEdge> connected_components) throws FileNotFoundException, UnsupportedEncodingException 
	{
		String word1 = null;
		String word2 = null;

		for (Set<String> group : list_of_components)
		{
			try 
			{
				Iterator<String> iter = group.iterator();

				word1 = iter.next();
				word2 = iter.next();
				
				//Common prefix of word1 and word2
				String min_prefix = commonPrefix(word1, word2);

				/*
				 * For each connected component we search for the stem that represents it.
				 * In order to achieve this goal we search for the common prefix of minimum length between all the String objects 
				 * which belong to a connected component.  
				 */

				for (String word : group) 
				{
					String temp_prefix = commonPrefix(word1, word);
					if (temp_prefix.length() < min_prefix.length()) 
					{
						min_prefix = temp_prefix;
					}

				}
				
				
				for (String word : group) 
				{
					//Couples of a String belonging to a connected component and the stem that represents it. 
					String[] temp = { word, min_prefix };
					orderedLookupTable.add(temp);
				}
			} 
			catch (NoSuchElementException e)
			{

			}

		}

		//lookup_table.txt is the output of the stemming process. 
		PrintWriter writer = new PrintWriter("lookup_table.txt", "UTF-8");

		for (String[] lookupElement : orderedLookupTable) 
		{
			writer.print(lookupElement[0] + "\t" + lookupElement[1] + "\n");
		}

		writer.close();
	}

	/**
	 * Class used to implement the Comparator Interface for String[]. The class
	 * is used to generate a lookup table in lexicographical order.
	 */

	public static class LookupComparator implements java.util.Comparator<String[]>
	{

		/**
		 * Compares the first String of array1 with the first String of array2.
		 * 
		 * @param array1 an Array of String.
		 * @param array2 an Array of String.
		 */
		
		public int compare(String[] array1, String[] array2) 
		{
			return array1[0].compareTo(array2[0]);
		}
		
	}

}
