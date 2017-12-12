
/**
 * @authors Andrea Langeli, Giacomo Rocco
 * @date 02-12-2016
 * 
 * An element of this class is a couple of integers: the first one is a document id
 * while the latter one is the occurrence of a descriptor in the selected document.
 */

 
public class WordStat 
{
	//Id of a document where the word

	private int doc; 
	
	//Occurrence of a index term in the document doc. 
	private int term_freq;

	/**
	 * It simply initializes the private variables. 
	 * 
	 * @param document
	 * @param frequency
	 */

	public WordStat(int document, int frequency)
	{
		this.doc = document;
		this.term_freq = frequency;
	}
    
	/**
	 * It modifies the frequency of an index_term in the selected document
	 * 
	 * @param frequency
	 */
	
	public void changeFrequency(int frequency)
	{
		this.term_freq = frequency;		
	}
	
	/**
	 * It gives back the occurrence of an index_term in the document whose id is doc. 
	 *  
	 * @return term_freq
	 */
	 
	public int getFrequency()
	{
		return this.term_freq;
	}
	
	/**
	 * It returns the id of the document
	 * 
	 * @return doc
	 */
	 
	public int getDoc()
	{
		return this.doc;
	}
	
	/**
	 * It returns the description of the object
	 */
	 
	public String toString()
	{
		return "("+ doc + ", " + term_freq + ")";
	}    
}
