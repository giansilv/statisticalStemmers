/*
 * 	Author: Giulio Busato
 * 		1111268
 * 		Master Student
 * 		University of Padua
 *
 *	Date:	April 2017
 *
 * */

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class MainStemmer {
	
	static String encoding = "UTF-8";
	static String input_file;
	static String output_file="out.txt";
	static int l;
	static int alpha;
	static double delta;
	
	// read the input file and create an array of words
	static public String[] readFile(String filename) throws IOException
	{	
		List<String> word_list = new ArrayList<String>();
		Scanner in = new Scanner(new InputStreamReader(new FileInputStream(filename), Charset.forName(encoding)));
		String line;
		while(in.hasNextLine())
		{
			line=in.nextLine();
			word_list.add(line);
		}
		in.close();
		
		// transfer the ArrayList into Array
		String[] words = word_list.toArray(new String[0]);
		word_list.clear();
		
		// return the array if words
		return words;
	}
	
			
	// write the output file of stemmed words 
	static public void writeFile(String filename, String[] words, String[] stems) throws IOException	
	{
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename,false), Charset.forName(encoding)));
		for (int i=0; i<words.length; i++)
			if (stems[i]==null)  writer.write(words[i]+"\t"+words[i]+"\n");
			else writer.write(words[i]+"\t"+stems[i]+"\n");
		writer.close();
	}
	
	// main method
	public static void main(String[] args) throws IOException
	{	
		// SINTAX: MainStemmer <inputFile> <l> <alpha> <delta>
		if (args.length!=4)
		{
			System.out.println("SINTAX INVALID!\nTipe: MainStemmer <inputFile> <l> <alpha> <delta>");
			return;
		}
		else
		{	
			System.out.println("--------------------- GRAS ---------------------");
			System.out.println("\n - Parameters ok");
			input_file = args[0];
			l = Integer.parseInt(args[1]);
			alpha = Integer.parseInt(args[2]);
			delta = Double.parseDouble(args[3]);
		}			
		
		long startTime,stopTime;
		
		// create an array of words
		String[] words=null;
		try {
			startTime = System.currentTimeMillis();
			words = readFile(input_file);
			stopTime = System.currentTimeMillis();
			System.out.println("\n - Read input file: "+(stopTime-startTime)+"ms");
		} catch (IOException e) {
			System.out.println("INPUT FILE ERROR");
			return;
		}
				
		// compute the stemming
		String[] stems = Grass.stemming(words, l, alpha, delta);

		// write the output file
		try {
			startTime = System.currentTimeMillis();
			writeFile(output_file, words, stems);
			stopTime = System.currentTimeMillis();
			System.out.println("\n - Write output file: "+(stopTime-startTime)+"ms");
			
			
		} catch (IOException e) {
			System.out.println("OUTPUT FILE ERROR");
			e.printStackTrace();
			return;
		}
		System.out.println("\n--------------------- DONE ---------------------");
	}
}
