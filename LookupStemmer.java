/*
 * Copyright 2016 University of Padua, Italy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terrier.terms;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terrier.utility.ApplicationSetup;
import org.terrier.utility.Files;

/**
 * Implements lookup stemmers as a TermPipeline object. The lookup list has to
 * be loaded from the <tt>lookupstemmer.filename</tt> property.
 * 
 * <b>Properties</b><br />
 * <ul>
 * <li><tt>lookupstemmer.filename</tt> - the stem list to load. There MUST be an empty row at the end of the file.
 * The format of the file is: word \t stem \n word \t stem \n ...
 * <li><tt>lookupstemmer.encoding</tt> - encoding of the file containing the
 * stemmed words, if not set defaults to <tt>trec.encoding</tt>, and if that is
 * not set, onto the default system encoding.</li>
 * </ul>
 * 
 * @author Gianmaria Silvello <silvello@dei.unipd.it>
 */
public class LookupStemmer extends StemmerTermPipeline {
	/** The logger used */
	private static Logger logger = LoggerFactory.getLogger(Stopwords.class);

	
	/** The matrix that contains all the stems. */
	protected HashMap<String, String> lookupStems = new HashMap<String, String>(10000);

	/**
	 * Makes a new lookup stemmer termpipeline object. The lookup stemmer file
	 * is loaded from the application setup file, under the property
	 * <tt>lookupstemmer.filename</tt>.
	 * 
	 * @param _next
	 *            TermPipeline the next component in the term pipeline.
	 */
	public LookupStemmer(final TermPipeline _next) {
		this(_next, ApplicationSetup.getProperty("lookupstemmer.filename", "lookup.txt"));
	}

	/**
	 * Makes a new lookup stemmer term pipeline object. The lookup stemmer
	 * file(s) are loaded from the filename parameter. If the filename is not
	 * absolute, it is assumed to be in TERRIER_SHARE. LookupStemmerFile is
	 * split on \t .
	 * 
	 * @param _next
	 *            TermPipeline the next component in the term pipeline
	 * @param LookupStemmerFile
	 *            The filename(s) of the file to use as the lookup stemmer list.
	 *            Split on tab, and passed to the (TermPipeline,String[])
	 *            constructor.
	 */
	public LookupStemmer(final TermPipeline _next, final String LookupStemmerFile) {
		super(_next);

		loadLookupStemmerList(LookupStemmerFile);
	}

	/**
	 * Loads the specified lookupStemmer file. If a lookupStemmer filename is
	 * not absolute, then ApplicationSetup.TERRIER_SHARE is appended.
	 * 
	 * @param lookupStemmerFilename
	 *            The filename of the file to use as the lookup stemmer list.
	 */
	public void loadLookupStemmerList(String lookupStemmerFilename) {

		// get the absolute filename
		lookupStemmerFilename = ApplicationSetup.makeAbsolute(lookupStemmerFilename, ApplicationSetup.TERRIER_SHARE);

		// determine encoding to use when reading the lookupStemmer file
		String lookupStemmerEncoding = ApplicationSetup.getProperty("lookupstemmer.encoding",
				ApplicationSetup.getProperty("trec.encoding", null));
		try {
			
			// use sys default encoding if none specified
			BufferedReader br = lookupStemmerEncoding != null
					? Files.openFileReader(lookupStemmerFilename, lookupStemmerEncoding)
					: Files.openFileReader(lookupStemmerFilename);

			Scanner scanner = new Scanner(br);
			
			while (scanner.hasNext()){
				lookupStems.put(scanner.next().toLowerCase(), scanner.next().toLowerCase());
			}
			
			scanner.close();

		} catch (IOException ioe) {
			logger.error("Error: Input/Output Exception while reading lookupStemmmer list (" + lookupStemmerFilename
					+ ") :  Stack trace follows.", ioe);

		}
	
	}

	/** {@inheritDoc} */
	public boolean reset() {
		return next.reset();
	}

	@Override
	public String stem(String s) {
		String stem = lookupStems.get(s);
		
		return  stem != null ? stem : s;
	}
}
