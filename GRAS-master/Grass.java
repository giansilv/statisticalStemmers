/*
 * 	Author: Giulio Busato
 * 		1111268
 * 		Master Student
 * 		University of Padua
 *
 *	Date:	April 2017
 *
 * */

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class Grass {
	
	static boolean debug=false;
	
	// compute the GRASS stemming of words and given parameters
	public static String[] stemming(String[] words, int l, int alpha, double delta)
	{
		long startTime = System.currentTimeMillis();
		
		// partition the words in a set of classes such that any two words in the same class have the same LCP>=l
		List<Integer> classes = getPartitions(words,l);	
		long partitionsTime = System.currentTimeMillis();
		
		// find the alpha-frequent suffix pairs <s1,s2> such that: w1=rs1, w2=rs2, r=LCP>= l
		HashMap<String,Integer> frequentSuffixPairs = getFrequentSuffixPairs(words, classes, l, alpha); 	
		long freqPairsTime = System.currentTimeMillis();
		
		// identify the class with the pivot word
		String[] stems = identifyClass(words, classes, frequentSuffixPairs, l, delta);
		long stopTime = System.currentTimeMillis();
		
		System.out.println("\n - Stemming:");
		System.out.println("\t- partition the words:\t\t"+(partitionsTime-startTime)+"ms");
		System.out.println("\t- find frequent suffix pairs:\t"+(freqPairsTime-partitionsTime)+"ms");
		System.out.println("\t- identify classes:\t\t"+(stopTime-freqPairsTime)+"ms");
		System.out.println("\t- total stemming time:\t\t"+(stopTime-startTime)+"ms");
		return stems;
	}
	
	// this method return a list of first and last positions of each class with at least two words
	public static List<Integer> getPartitions(String[] words, int l)
	{
		List<Integer> classes = new ArrayList<Integer>();
		
		for (int i=0; i<words.length; i++)
			if (words[i].length()>=l)
			{
				String prefix = words[i].substring(0,l);
				int count = 0;

				// search the words with the same prefix l
				for (int j=i+1; j<words.length; j++)
				{	
					// if the word is shorter than l or the prefixes are different, stop the search
					if (words[j].length()<l) break;
					if (!prefix.equals(words[j].substring(0,l))) break;
					count++;
				}
				// a class must contain at least two words
				if (count>0)
				{
					classes.add(i);
					classes.add(i+count);
				}
				i=i+count;	
			}
		
		if (debug) System.out.println("Number of Classes:\t"+classes.size()/2);
		return classes;
	}
	
	// this method compute the suffix pairs and his frequency for each pair of words in all the classes
	public static HashMap<String, Integer> getFrequentSuffixPairs(String[] words, List<Integer> classes, int l, int alpha)
	{			
		HashMap<String, Integer> alphaFrequent = new HashMap<String, Integer>();
		HashMap<String, Integer> nonFrequent = new HashMap<String, Integer>();
		
		// for each class of words
		for (int i=0; i<classes.size()-1; i=i+2) 
		
			// find all the pairs in the class
			for (int j=classes.get(i); j<=classes.get(i+1); j++)
				for (int k=j+1; k<=classes.get(i+1); k++)
				{
					// obtain the suffix pair <s1,s2> of the words <wj,wk>
					String suffixPair = getSuffixPair(words[j],words[k],l);
					
					// update the frequency of the suffix pair
					Integer frequency = alphaFrequent.get(suffixPair);
					if (frequency!=null)
						alphaFrequent.put(suffixPair,frequency+1);
					else
					{
						frequency = nonFrequent.get(suffixPair);
						if (frequency==null) frequency=0;
						frequency++;
					
						// if the frequency is alpha the suffix pair become alpha-frequent
						if (frequency==alpha)
						{
							nonFrequent.remove(suffixPair);
							alphaFrequent.put(suffixPair,frequency);
						}
						else
							nonFrequent.put(suffixPair,frequency); 
					}
				}
		
		if (debug) System.out.println("Freqent Pairs:\t\t"+alphaFrequent.size());
		if (debug) System.out.println("Non Frequent:\t\t"+nonFrequent.size());
		nonFrequent.clear();
		return alphaFrequent;
	}
	
	// method that return the suffix pair <s1,s2> of the words <w1,w2> with the same prefix l
	public static String getSuffixPair(String w1,String w2,int l) 
	{
		// calculate the LCP of the two words 
		int r = longestCommonPrefix(w1, w2, l);
		
		// build the suffix pair <s1,s2> of <w1,w2>
		String suffixPair;
		
		if (w1.length()==r)
			suffixPair="NULL";
		else
			suffixPair=w1.substring(r, w1.length());
		
		suffixPair=suffixPair.concat(",");
		
		if (w2.length()==r)
			suffixPair=suffixPair.concat("NULL");
		else
			suffixPair=suffixPair.concat(w2.substring(r,w2.length()));
		
		return suffixPair;
	}
	
	// method that return the LCP of two words that have a common prefix l
	public static int longestCommonPrefix(String w1,String w2,int l)
	{
		int i=l;
		while (i<Math.min(w1.length(), w2.length()))
			if (w1.charAt(i)==w2.charAt(i))
				i++;
			else
				break;
		return i;
	}
	
	// method that identify the real class of word (class of stem)
	public static String[] identifyClass(String[] words, List<Integer> classes, HashMap<String,Integer> frequentSuffixPairs, int l, double delta)
	{
		String[] stems =  new String[words.length];
		
		// for each class of words
		for (int i=0; i<classes.size()-1; i=i+2) 
		{
			int first = classes.get(i);
			int last = classes.get(i+1);		

			// build the graph of the class			
			Graph G = buildGraph(first,last, words, frequentSuffixPairs, l);
			
			// if there isn't any node G.getNodeWithMaxDegree returns -1
			while ((G.getNodeWithMaxDegree())>=0)
			{	
				// let p be the pivotal node with maximum degree
				int p = G.getNodeWithMaxDegree();
				int[] adjacent_p = G.getAdjacentList(p);
				
				// S in the class of pivotal node
				List<Integer> S=new ArrayList<Integer>();
				S.add(p);	
				
				// visit all the adjacent nodes of the pivot, in decreasing order of edge weight 
				for(int v=0;v<adjacent_p.length;v++)
				{
					int[] adj_p=G.getAdjacentList(p);
					int[] adj_v=G.getAdjacentList(adjacent_p[v]);
				
					// compute the cohesion between the pivot and the visited node v
					if (getCohesion(adj_p,adj_v)>=delta)
						S.add(adjacent_p[v]);
					else
						G.removeEdge(p, adjacent_p[v]);
				}
				
				// find the stem word for the class S
				String stem = getStem(S, words, first, l);
				
				// remove from G all the vertices in S and their incident edges
				for (int j:S)
				{			
					stems[first+j]=stem;
					G.removeNode(j);
				}
			}
		}
		return stems;
	}
	
	// method that returns a Graph G=(V,E) :V=words in the class, E={(u,v): w(u,v)>=alpha}, where w(u,v) is the frequency of the suffix pair <s1,s2> of <u,v>
	public static Graph buildGraph(int first, int last, String[] words, HashMap<String,Integer> frequentSuffixPairs, int l) 
	{
		
		// build a graph G with a node for each word in the class
		Graph G = new Graph(last-first+1);	
		
		// for each couple of words (nodes)
		for (int j=first; j<=last; j++)
			for (int k=j+1; k<=last; k++)
			{							
				// obtain the suffix pair of the two words
				String suffixPair = getSuffixPair(words[j],words[k],l);
				
				// if the frequency of the suffix pair is >= alpha, create a weighted edge w(u,v)
				Integer frequency = frequentSuffixPairs.get(suffixPair);
				if(frequency!=null){
					G.addEdge(j-first, k-first, frequency);
				}
			}	
		return G;
	}
		
	// method that compute the cohesion between two nodes (p,v), given their adjacent lists
	public static double getCohesion(int[] adj_p, int[] adj_v)
	{
		double cohesion;
		int intersection=0, j=0, i=0;
		Arrays.sort(adj_p);
		Arrays.sort(adj_v);
		
		// compute the cardinality of intersection between adj_p and adj_v
		while(i<adj_p.length && j<adj_v.length)
			if(adj_p[i]==adj_v[j]) {
				intersection++;
				i++;j++;
			}else
				if(adj_p[i]<adj_v[j])
					i++;
				else
					j++;
		
		// cohesion formula
		cohesion = (1.+intersection)/adj_v.length;
		return cohesion;
	}
	
	// method that return the stem word of the class S
	public static String getStem(List<Integer> S, String[] words, int first, int l)
	{
		// assume the first word of the class like a candidate stem
		String stem = words[S.get(0)+first];
		int leng = stem.length();
		
		// check the candidate for all the words in the class
		for (int i=1; i<S.size(); i++)
		{
			String word = words[S.get(i)+first];
			
			// if the words is shorter or not compatible with the candidate stem, compute the LCP 
			if ((word.length()<leng) || !(word.substring(0,leng).equals(stem)))
			{
				// the LCP is the new candidate stem
				leng = longestCommonPrefix(stem,word,l);
				stem = stem.substring(0,leng);
			}
		}
		return stem;
	}
}
