import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.Semaphore;
import java.util.Scanner;

public class SnsStemmer implements Runnable
{

	//Boolean variable used to let the threads that execute the stemming process and the one that registers
	//the usage of memory cooperate.
	private static boolean end_execution = false;
	
	//It is used to guarantee that the access to end_execution is in mutual exclusion.
	private static Semaphore semaphore = new Semaphore(1);
	
	//Time between two registration of the use of memory
	long sleep_time = 60 * 1000;
	
	/**
	 * Implementation of the run method defined in the Runnable interface. The names of the
	 * threads that execute this method are used to select which operations each thread has to carry out. 
	 */
	public void run()
	{
		//Name of the considered thread
		String name = Thread.currentThread().getName();
		
		//Thread which records the usage of memory assigned to the JVM
		if(name.equals("memCounter"))
		{
			PrintWriter writer_memory = null;
			
			try 
			{
				//PrintWriter object used to write the occupied memory in the memory.txt file
				writer_memory = new PrintWriter("memory.txt", "UTF-8");
			} 
			catch (FileNotFoundException | UnsupportedEncodingException e1)
			{
				System.out.println("run: " + e1);
			}
			
		  	while(!inspectVal())
		  	{
		  		 Runtime rt = Runtime.getRuntime();
		  		 
		  		 //Used memory
		    	 long usedMB = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
		    	 
		    	 writer_memory.print(usedMB + "\n");
		    	 //System.out.println(usedMB);
	
		    	 try
		    	 {
		    		 //After the registration of the used memory the thread sleeps for sleep_time milliseconds.
		    		 Thread.sleep(sleep_time);
		    	 }
		    	 catch(InterruptedException e)
		    	 {
		    		 
		    	 }
		    	 
		  	 }
		  	
		  	 //closure of the stream associated with the write_memory object 
		  	 writer_memory.close();
		}
		
		else
		{
			//Expected number of elements of the lexicon file
			Scanner sc = new Scanner(System.in);
			System.out.println("Collection (choice between bg, cz, it, ru, hu or de):");
			String collection = sc.next();
			
			int n_expected_elements;
			if(collection.equals("it"))
			{
				n_expected_elements = 389771;
			}
			else if(collection.equals("ru"))
			{
				n_expected_elements = 377630;
			}
			else if(collection.equals("de"))
			{
				n_expected_elements = 1030177;
			}
			else if(collection.equals("hu"))
			{
				n_expected_elements = 542382;
			}
			else if(collection.equals("cz"))
			{
				n_expected_elements = 462039;
			}
			else if(collection.equals("bg"))
			{
				n_expected_elements = 298897;
			}
			else if(collection.equals("enTREC678"))
			{
				n_expected_elements = 767573;
			}
			else
			{
				n_expected_elements = 0;
			}
			//System.out.println("L1 parameter (recommended value 3):");
			int l1 = 3; //l1 parameter
			//System.out.println("L2 parameter (recommended value 5):"); //l2 parameter
			int l2 = 5; 

			//Initialization of a SnsTool object
			SnsTool tools = new SnsTool(l1, l2);
      
			try 
			{
				//Invocation of stemming process
				//System.out.println("Insert the name of the lexicon file (example 'lexicon.txt'):");
				String lex = "lexicon.txt";
				//System.out.println("Insert the name of the inverted_list file (example 'inverted.txt'):");
				String inv = "inverted.txt";
				tools.executeSnsStemmer(lex, inv, n_expected_elements);
				
				sc.close();
			} 
			catch (IOException e1) 
			{
				System.out.println("run: " + e1);
			}
      
			try
			{
				//Acquisition of the lock
				semaphore.acquire();
  			
				//Update of the variable end_execution. This modification will stop the execution of the memCounter thread. 
				end_execution = true;
  		  
				//Release of the lock
				semaphore.release(); 
  		  
				System.out.println("End");
			}
			catch(InterruptedException e)
			{
  			
			}
		}
	}
	
	/**
	 * This method inspects the state of the variable end_execution and returns its value.
	 * It accesses it in mutual exclusion. 
	 * 
	 * @return the state of the variable end_execution
	 */
	public boolean inspectVal() 
	{
		boolean temporary_value = true;
		try
		{
			//Acquisition of the lock
			semaphore.acquire();
   	   
			//Inspection of the variable end_execution.
			temporary_value = end_execution;
       
			//Release of the lock  
   		 	semaphore.release();
		}
		catch(InterruptedException e)
		{
			System.out.println("inspectVal: " + e);
		}
 		
		return temporary_value;
 	}

	public static void main(String[] args) throws FileNotFoundException, IOException 
	{
		SnsStemmer s1 = new SnsStemmer();
		
		//Initialization of the thread that will keep track of the use of the memory
		Thread memoryCheck = new Thread(s1, "memCounter");
		memoryCheck.start();
		
		SnsStemmer s2 = new SnsStemmer();
		
		//Initialization of the thread that will execute the stemming processing 
		Thread executionSnsStemmer = new Thread(s2, "executionSnsStemmer");
		executionSnsStemmer.start();

	}

}
