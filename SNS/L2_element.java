/**
 * @author Andrea Langeli, Giacomo Rocco
 * @version 14.12.16
 * 
 * An element of this class is associated with couples of index terms with a common prefix >= l1 which 
 * have the same key. A key is made by the concatenation of the two index terms after the removal of the common prefix. 
 * The two suffixes are concatenated according to the lexicographic ordering. 
 */

import java.util.LinkedList;
public class L2_element 
{
	 //Number of couples with common prefix >= l1 and with the same key. Each couple is associated with an Arc object.
   int ssuffix; 
   
   //LinkedList that contains Arc objects. 
   LinkedList<Arc> element_arcs;
   
  /**
   * ssuffix is set to 0 and element_arcs refers to an empty LinkedList.
   */
   
   public L2_element()
   {
   	this.ssuffix = 0;
   	this.element_arcs = new LinkedList<Arc>();
   }
   
  /**
   * It returns the variable ssuffix
   * 
   * @return ssuffix
   */
   
   public int getCounter()
   {
   	return ssuffix;
   }
   
  /**
   * It returns element_arcs
   * 
   * @return element_arcs
   */
   
   public LinkedList<Arc> getArcs()
   {
   	return element_arcs;
   }
   
  /**
   * It adds an Arc object to the LinkedList element_arcs
   * 
   * @param element
   */
   public void addArc(Arc element)
   {
   	element_arcs.add(element);
   }
   
  /**
   * It increases by a unit the variable ssuffix
   */
   
   public void addCounter()
   {
   	ssuffix++;
   }
   
  /**
   * It gives back the description of an object of the class. 
   */
   
   public String toString()
   {
  	 String description = "Number of elements: " + ssuffix + "\n";
  	 description = description + element_arcs.toString();
  	 return description;
   }
}
