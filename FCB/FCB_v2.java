/*
* @author <a href="mailto:alb.purpura@gmail.com">Alberto Purpura</a>
* @version 0.1
* @since 0.1
* Copyright 2017 University of Padua, Italy
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
*
* Implementation of the Fast Corpus-Based stemmer (FCB) from the paper "A Fast Corpus-Based Stemmer"
* by J. H. Paik and S. K. Parui, DOI: 10.1145/1967293.1967295
*/


import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FCB_v2 {

    /**
     * Computes all the potential suffixes, if a suffix is a substring of another one the longest one that satisfies
     * the frequency constraints is added to the list.
     * list
     *
     * @param lexicon the set of distinct strings found in a corpus
     * @return the list of frequent suffixes
     */
    private static ObjectArrayList<String> getPotentialSuffixes(ObjectArrayList<String> lexicon) {
        ObjectArrayList<String> ps = new ObjectArrayList<>();
        int maxSuffLen = Math.max(getMaxStrLen(lexicon) - 1, 1);
        //ObjectArrayList<String> revList = revList(lexicon);
        HashMap<String, Integer> suffFreqs = new HashMap<>();
        for (int suffLen = 1; suffLen < maxSuffLen; suffLen++) {
            for (String token : lexicon) {
                //the whole word can't be a suffix
                if (token.length() <= suffLen) {
                    continue;
                }
                String suff = token.substring(token.length() - suffLen, token.length());
                if (suffFreqs.containsKey(suff)) {
                    int newFreq = suffFreqs.get(suff) + 1;
                    suffFreqs.replace(suff, newFreq);
                } else {
                    suffFreqs.put(suff, 1);
                }
            }
        }
        System.out.println("total suffixes found (not filtered): " + suffFreqs.size());
        //compute now cutoff threshold alpha to select frequent suffixes
        IntArrayList freqs = new IntArrayList();
        for (Map.Entry<String, Integer> e : suffFreqs.entrySet()) {
            freqs.add(e.getValue());
        }
        int cutoffSuffFreq = computeCutOffThr(freqs);

        for (Map.Entry<String, Integer> e : suffFreqs.entrySet()) {
            if (e.getValue() >= cutoffSuffFreq) {
                ps.add(e.getKey());
            }
        }
        return ps;
    }

    /**
     * Computes the cut-off threshold for the frequent/potential suffixes according to the method described in the
     * reference paper.
     *
     * @param list the list of suffix frequencies
     * @return the cut-off threshold for frequent suffixes.
     */
    private static int computeCutOffThr(IntArrayList list) {
        list.sort(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return -o1.compareTo(o2);
            }
        });
        int windowLen = 10;
        int startL = 0;
        int endL = startL + windowLen;
        int endR = endL + windowLen;
        int startR = endL + 1;
        while (endR < list.size()) {
            List<Integer> left = list.subList(startL, endL);
            double avgL = computeAvgOfWindow(left);
            List<Integer> right = list.subList(startR, endR);
            double avgR = computeAvgOfWindow(right);

            if (avgR / avgL >= 0.99) {
                return (int) avgL;
            }
            startL++;
            endL++;
            startR++;
            endR++;
        }
        return 1;
    }

    private static double computeAvgOfWindow(List<Integer> list) {
        int sum = 0;
        for (int i : list) {
            sum += i;
        }
        return sum / (double) list.size();
    }

    private static int getMaxStrLen(ObjectArrayList<String> list) {
        int max = -1;
        for (String s : list) {
            if (s.length() > max) {
                max = s.length();
            }
        }
        return max;
    }


    private static int computeLongestPrefixLen(String s1, String s2) {
        int maxLen = Math.min(s1.length(), s2.length());
        int i = 1;
        while (i <= maxLen) {
            if (!s1.substring(0, i).equalsIgnoreCase(s2.substring(0, i))) {
                break;
            }
            i++;
        }
        return i - 1;
    }

    //////////////////////////////////////////////////////////////////

    /**
     * Computes the equivalence classes as defined in the reference paper.
     *
     * @param k   the minimum lenght of the longest common prefix of the elements in each class
     * @param lex the terms to group
     * @return the k-equivalence classes for a fixed k
     */
    private static ObjectArrayList<ObjectArrayList<String>> computeKEquivalenceClasses(int k, ObjectArrayList<String> lex) {
        ObjectArrayList<ObjectArrayList<String>> KEqClasses = new ObjectArrayList<>();
        ObjectArrayList<String> myLexicon = new ObjectArrayList<>();
        myLexicon.addAll(lex);
        myLexicon.sort(String::compareTo);

        //I cannot have words that are shorter than k in a k-equivalence class
        //therefore look now for the first word in the lexicon longer than k and remove the shorter ones
        String s = myLexicon.get(0);
        while (s.length() < k && !myLexicon.isEmpty()) {
            myLexicon.remove(0);
            s = myLexicon.get(0);
        }

        while (!myLexicon.isEmpty()) {
            int lsize = myLexicon.size();
            ObjectArrayList<String> teq = new ObjectArrayList<>();
            s = myLexicon.get(0);
            teq.add(s);
            for (int j = 1; j < myLexicon.size(); j++) {
                if (!myLexicon.get(j).startsWith(s.substring(0, 1))) {
                    break;
                }
                if (computeLongestPrefixLen(s, myLexicon.get(j)) >= k) {
                    teq.add(myLexicon.get(j));
                }
            }
            myLexicon.removeAll(teq);
            KEqClasses.add(teq);
            if (lsize == myLexicon.size()) {
                break;
            }
        }
        return KEqClasses;
    }

    private static BufferedReader loadFileInUTF(String filePath) throws FileNotFoundException, UnsupportedEncodingException {
        return new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath), "UTF-8"));
    }

    /**
     * Loads tokens from a lexicon file exported by terrier and stores them into an ObjectArrayList
     *
     * @param filePath the input file where it should load the tokens
     * @return the tokens list in an ObjectArrayList
     */
    private static ObjectArrayList<String> loadLexicon(String filePath) throws IOException {
        ObjectArrayList<String> retval = new ObjectArrayList<String>();
        BufferedReader reader = loadFileInUTF(filePath);
        String line = reader.readLine();
        while (line != null) {
            String[] ws = line.split(",");
            retval.add(ws[0].toLowerCase());
            line = reader.readLine();
        }
        reader.close();
        return retval;
    }

    /**
     * The class generator is the longest prefix of the elements in the class, see reference paper for more info.
     *
     * @param list A list of strings
     * @return returns the generator of a class according to the definition of generator in the reference paper.
     */
    private static String getClassGenerator(ObjectArrayList<String> list) {
        if (list.size() == 1) {
            //@@ here the paper is unclear on what to do
            //first option:
            return list.get(0);
        }
        //compute the length of the shortest string in the list.
        //The longest prefix can't be longer than this.
        int minLen = Integer.MAX_VALUE;
        int minInd = 0;
        for (int j = 0; j < list.size(); j++) {
            int tl = list.get(j).length();
            if (tl < minLen) {
                minLen = tl;
                minInd = j;
            }
        }

        String s = list.get(minInd);
        int maxPrefLen = -1;
        for (int i = 0; i < list.size(); i++) {
            if (i == minInd) {
                continue;
            }
            int pLen = computeLongestPrefixLen(list.get(i), s);
            if (pLen > maxPrefLen) {
                maxPrefLen = pLen;
            }
        }
        return s.substring(0, maxPrefLen);
    }

    /**
     * Returns class generator for singleton classes (new behaviour wrt the reference paper)
     *
     * @param term        the term that is in the singleton class
     * @param potSuffixes the list of potential suffixes extracted from the lexicon
     * @return Returns class generator for singleton classes
     */
    private static String getClassGeneratorForSingletonClass(String term, ObjectArrayList<String> potSuffixes) {
        String longestSuff = "";
        for (String ps : potSuffixes) {
            if (term.endsWith(ps)) {
                if (ps.length() > longestSuff.length()) {
                    longestSuff = ps;
                }
            }
        }
        return term.substring(0, longestSuff.length());
    }


    /**
     * Compute the strength (see definition in the reference paper) of a class generator according to the definition in
     * the reference paper.
     *
     * @param classGenerator the string representing a class generator to evaluate
     * @param generatedClass the class corresponding to the generator
     * @param potSuffixes    the list of potential suffixes extracted from the lexicon
     * @return the strength of the class generator in input wrt the passed class.
     */
    private static double getClassGeneratorStrength(String classGenerator, ObjectArrayList<String> generatedClass,
                                                    ObjectArrayList<String> potSuffixes) {
        return getPotentialClass(classGenerator, generatedClass, potSuffixes).size() / ((double) generatedClass.size());
    }


    /**
     * Returns the potential class associated to a generator string. Seed definition of potential class in reference
     * paper. New approach here to generate them wrt the reference paper.
     *
     * @param classGenerator the string that serves as the generator for the potential class
     * @param kEqClass       the class from which to extract the elements of the potential class
     * @param potSuffixes    the list of frequent suffixes extracted from the lexicon
     * @return the potential class associated to a generator.
     */
    private static ObjectArrayList<String> getPotentialClass(String classGenerator, ObjectArrayList<String> kEqClass,
                                                             ObjectArrayList<String> potSuffixes) {
        ObjectArrayList<String> potClass = new ObjectArrayList<>();
        //@@ here if the class size is 1, consider it as a potential class only if the term ends with a potential suffix
        if (kEqClass.size() == 1) {
            String el = kEqClass.get(0);
            for (String pSuff : potSuffixes) {
                if (el.endsWith(pSuff)) {
                    return kEqClass;
                }
            }
            return potClass; //return an empty list if the term does not end in one of the potential suffixes
        }
        //@@ simplified approach that looks directly at suffixes
        for (String s : kEqClass) {
            for (String pSuff : potSuffixes) {
                if (s.endsWith(pSuff)) {
                    potClass.add(s);
                    break;
                }
            }
        }
        return potClass;
    }

    /**
     * Returns the generated class by a generator. See definitions in the reference paper.
     *
     * @param generator the generator of the new class
     * @param terms     a list of terms from which to extract the generated class elements.
     * @return Returns the generated class by a generator. See definitions in the reference paper.
     */
    private static ObjectArrayList<String> getGeneratedClass(String generator, ObjectArrayList<String> terms) {
        ObjectArrayList<String> gc = new ObjectArrayList<>();
        for (String s : terms) {
            if (s.startsWith(generator)) {
                gc.add(s);
            }
        }
        return gc;
    }

    private static ObjectArrayList<ObjectArrayList<String>> getEquivalenceClasses(ObjectArrayList<ObjectArrayList<String>> KEqClasses,
                                                                                  double delta, ObjectArrayList<String> potSuffs) {
        ObjectArrayList<ObjectArrayList<String>> eqClasses = new ObjectArrayList<>();
        for (ObjectArrayList<String> kEC : KEqClasses) {
            String classGenerator;
            if (kEC.size() > 1) {
                classGenerator = getClassGenerator(kEC);
            } else {
                classGenerator = getClassGeneratorForSingletonClass(kEC.get(0), potSuffs);
            }
            if (delta <= getClassGeneratorStrength(classGenerator, kEC, potSuffs)) {
                eqClasses.add(kEC);
            } else {
                if (kEC.size() == 1) {
                    continue;
                }
                //refine equivalence class
                while (!kEC.isEmpty()) {
                    String c = kEC.get(0);
                    ObjectArrayList<String> cGenClass = getGeneratedClass(c, kEC);
                    if (delta <= getClassGeneratorStrength(c, cGenClass, potSuffs)) {
                        eqClasses.add(cGenClass);
                    }
                    kEC.removeAll(cGenClass);
                }
            }
        }
        return eqClasses;
    }

    private static void printEqClassesToFile(String outFilePath, ObjectArrayList<ObjectArrayList<String>> eqClasses,
                                             ObjectArrayList<String> potSuffixes)
            throws IOException {
        Writer wr = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(outFilePath), "UTF-8"));
        for (ObjectArrayList<String> eq : eqClasses) {
            if (eq.size() == 1) {
                String el = eq.get(0);
                String root = getClassGeneratorForSingletonClass(el, potSuffixes);
                wr.write(el + "\t" + root + "\n");
            } else {
                String root = getClassGenerator(eq);
                for (String el : eq) {
                    wr.write(el + "\t" + root + "\n");
                }
            }
        }
        wr.close();
    }


    public static void main(String[] args) throws IOException {
        String lexiconPath = "data/CZ_T_lexicon.txt";
        String lang = "CZ";
        String type = "T";
        System.out.println(lexiconPath);
        ObjectArrayList<String> original_lexicon = loadLexicon(lexiconPath);
        System.out.println("computing potential suffixes...");
        ObjectArrayList<String> potentialSuffixes = getPotentialSuffixes(original_lexicon);
        int k1 = 5;
        int k2 = 2;
        double delta = 0.5;
        for (double d = delta; d <= 0.8; d += 0.1) {
            System.out.println("reloading lexicon...");
            ObjectArrayList<String> lexicon = new ObjectArrayList<>();
            lexicon.addAll(original_lexicon);
            System.out.println("delta = " + d);
            String outFilePath = "processed/" + lang + "_" + d + "_" + type + ".txt";
            ObjectArrayList<ObjectArrayList<String>> allEqClasses = new ObjectArrayList<>();
            System.out.println("computing equivalence classes...");
            for (int k = k1; k >= k2; k--) {
                if (lexicon.size() == 0) {
                    break;
                }
                System.out.println("k = " + k);
                long start = System.currentTimeMillis();
                ObjectArrayList<ObjectArrayList<String>> KEqClasses = computeKEquivalenceClasses(k, lexicon);
                long end = System.currentTimeMillis();
                System.out.println("Total time to compute k-equivalence classes: " + (end - start) / 1000d + "s");
                start = System.currentTimeMillis();
                ObjectArrayList<ObjectArrayList<String>> eqClasses = getEquivalenceClasses(KEqClasses, d,
                        potentialSuffixes);
                end = System.currentTimeMillis();
                System.out.println("Total time to compute final equivalence classes (k= "
                        + k + "): " + (end - start) / 1000d + "s");
                for (ObjectArrayList<String> ec : eqClasses) {
                    lexicon.removeAll(ec);
                }
                System.out.println("remaining terms in lexicon: " + lexicon.size());
                allEqClasses.addAll(eqClasses);
            }
            printEqClassesToFile(outFilePath, allEqClasses, potentialSuffixes);
        }
    }

}
