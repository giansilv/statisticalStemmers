/**
 * @author Andrea Langeli, Giacomo Rocco
 * @version 14.12.16
 * 
 * An element of this class is a representation of the edge between two elements of the index file at this stage of the indexing process.  
 * It stores the descriptors that it connects, their occurrence before and after the execution of the reWeighting method. 
 * The occurrence is the weight of the arc. 
 */

public class Arc
{

   String word_row;
   String word_column;
   int cooccurrence;
   double rco;
   
   /**
    * Initially the variable rco is set to 0 because it is the weight of the arc that is evaluated in the reWeighting method.
    * 
    * @param word_row
    * @param word_column
    * @param cooccurrence
    */
   public Arc(String word_row, String word_column, int cooccurrence)
   {

   	this.word_column = word_column;
   	this.word_row = word_row;
   	this.cooccurrence = cooccurrence;
   	this.rco = 0;
   }
     
   /**
    * It returns the first extremity of the arc.
    * 
    * @return word_row
    */
   public String getWordRow()
   {
   	return word_row;
   }
   
   /**
    * It returns the latter extremity of the arc.
    * 
    * @return word_column
    */
   public String getWordColumn()
   {
   	return word_column;
   }
   
   /**
    * It gives back the weight of the arc evaluated in the first part of the stemming process.
    * It is equal to the sum of the minimum frequency between the two terms over all documents that contain each of the selected descriptors. 
    *  
    * @return cooccurrence
    */
   public int getCooccurrence()
   {
   	return cooccurrence;
   }
   
   /**
    * It sets the variable rco to the desired value. For a couple of descriptors, the value of rco depends on their occurence  and 
    * on the occurence of the arcs that connect word_row or word_column to an other descriptor only if exists also the arc from that 
    * descriptor and the other index term.  This value is evaluated in the reWeighting method. 
    * 
    * @param rco
    */
   public void setRco(double rco)
   {
   	this.rco = rco;
   }
   
   /**
    * It returns the value of rco. 
    * 
    * @return rco
    */
   public double getRco()
   {
   	return rco;
   }
   
   /**
    * It gives back a description as String of an element of this class. 
    */
   public String toString()
   {
   	String description = "(" + word_row + ", " + word_column + ")";
   	return description;
   }
}
