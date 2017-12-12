/**
 * @authors Andrea Langeli, Giacomo Rocco
 * @date 02-12-2016
 * 
 * An element of this class is a String representing a word in the index and 
 * its associated posting list.
 */

import java.util.LinkedList;

public class IndexElement
{
	//a String representing the word in the index
	private String indexElement;
	//Linked list of WordStat (docID, Term_Frequency) representing 
	//the posting list
	private LinkedList<WordStat> postingList;
	
	
	/**
	 * It simply initializes the variables. 
	 * 
	 * @param index
	 * @param postList
	 */
	public IndexElement(String index, LinkedList<WordStat> postList)
	{
		indexElement = index;
		postingList = postList;
	}

	/**
	 * It gives back the index element representing a Word in the index table. 
	 *  
	 * @return indexElement
	 */

	public String getIEString()
	{
		return indexElement;
	}

	/**
	 * It gives back the Posting List associated with the index element. 
	 *  
	 * @return postingList
	 */
		
	public LinkedList<WordStat> getIEPostingList()
	{
		return postingList;
	}

	/**
	 * It returns the description of the object
	 */
	
	public String toString()
	{
		String temp = indexElement + ": ";
		for(WordStat wordStat : postingList)
		{
			temp = temp + " " + wordStat.toString();
		}
		
		return temp;
	}
   
}
