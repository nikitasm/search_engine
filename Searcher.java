import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;

import org.apache.lucene.index.Terms;
import org.apache.lucene.index.DocsEnum;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.index.DocsAndPositionsEnum;
import org.apache.lucene.search.spans.Spans;
import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.SlowCompositeReaderWrapper;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermContext;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.ArrayList;

import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Paths;

/**
 * Searches inside the index folder for the query string given from the User.
 * 
 */
public class Searcher {
	private static PorterStemAnalyzer analyzer = new PorterStemAnalyzer();
	private static String indexDir;
	private static FSDirectory fsDir;
	private static IndexReader reader;
	private static IndexSearcher searcher;
	private static TopScoreDocCollector collector;
	private static QueryParser parser;
	private static Query q;
	private String[] filenames = null;
	private int maxHits = 1000;
	Map<String, String[]> docTerms = new HashMap<String, String[]>();
	Map<String, String[]> termDocOccurences = new HashMap<String, String[]>();

	/**
	 * Constructor
	 * @param inputIndexDir the name of a text file or a folder we wish to add to the index
	 * @throws java.io.IOException when exception creating index.
	 */
	Searcher(String queryString, Boolean shouldSearch, String[] selectedDocuments) throws Exception {
		String inputReadDir = "index/";

		fsDir = FSDirectory.open(new File(inputReadDir)); // open index dir
		reader = DirectoryReader.open(fsDir); // open dir in reader
		searcher = new IndexSearcher(reader); 
		collector = TopScoreDocCollector.create(maxHits, true);
		parser = new QueryParser("contents", analyzer);

	/**
	 * if user has selected Documents, foreach term, foreach document two "arrays", Maps are setup.
	 * FIRST MAP : [document1=>["word1, word2, ..."], document2=>["word1, word2, word3,..."]]
	 * SECOND MAP : [queryTerm1=>["doc1"=>occurences, doc2=>occurences"], queryTerm2 => ["doc1=>occurences", ....]]
	 * if no documents are selected, query continues correctly. 
	 */
		if (shouldSearch == true) {
			String[] queryStringParts = queryString.split(" "); // split the query string to get each term

			for (int i=0; i<queryStringParts.length; ++i) {
				String query_term = queryStringParts[i];
				Map<String,String[]> termDocOccurences = getOccurences(selectedDocuments, query_term, reader, searcher); // foreach term, setup the second map.

				for (String mostValuableDoc : termDocOccurences.get(query_term)) { // foreach most valuable doc of the term
					if (mostValuableDoc != null) {
						String[] mostValuableDocParts = mostValuableDoc.split("-");
						String mostValuableDocID = mostValuableDocParts[0];
						for (String extraQueryTerm : docTerms.get(mostValuableDocID)) { // foreach term that is inside the most valuable doc
							if (extraQueryTerm != null) {
								queryString += " " + extraQueryTerm; // add the term in the query string
							}
						}
					}
				}
			}
		}
		q = parser.parse(queryString);

		long start = System.currentTimeMillis(); // searching start
		searcher.search(q, collector); // search
		ScoreDoc[] hits = collector.topDocs().scoreDocs; // set up tophits
		long end = System.currentTimeMillis(); //searching stop

		filenames = new String[hits.length+1];

		for(int i=0; i<hits.length; ++i) {// foreach document in the hits
			int docId = hits[i].doc;
			Document d = searcher.doc(docId); // get the document from the IndexSearcher

			filenames[i+1] = (i+1) + ". " + d.get("path") + " docID=" + docId + " score=" + hits[i].score; //set the output on filenames[] variable
		}
		filenames[0] = "Found " + hits.length + 
			" document(s) (in " + (end - start) + " milliseconds) that matched query '" + queryString + "':\n";
	}

	public String[] getFileNames()
	{
		return filenames; // filenames getter
	}

	/**
	 * Maps docs with terms and calculates queryterm occurence in selected docs
	 *
	 * @param selectedDocuments the selected documents
	 * @param queryTerm one of the query Terms
	 * @param reader IndexReader
	 * @param searcher IndexSearcher
	 */
	private Map<String,String[]> getOccurences(String[] selectedDocuments, String queryTerm, IndexReader reader, IndexSearcher searcher) {
		try {
		/**
	 	* Set up two biggest occurences, to handle the biggest occurences in docs. 
	 	* 
	 	*/
			int firstBiggest = 0;
			int secondBiggest = 0;
			int firstBiggestDoc = 0;
			int secondBiggestDoc = 0;

			for (int j=0; j< selectedDocuments.length; ++j) { // for each selected document
				if (selectedDocuments[j] != null) {
					String docIDUntrimmed = selectedDocuments[j];
					String[] docIDParts = docIDUntrimmed.split("=");  // split to get the part we need
					int docID = Integer.parseInt(docIDParts[1]); // get the second part
					Document d = searcher.doc(docID); // get the document on IndexSearcher
					String documentText = readFile(d.get("path"), Charset.defaultCharset()).replace("/", "").trim(); // read file contents using readFile function
					docTerms.put(Integer.toString(docID), documentText.split(" ")); // add the document terms on the DocTerms Map.
					/**
					 * Calculate term occurences in document
					 *
					 */
					int matchedCounter = 0;
					for (String docTerm : docTerms.get(Integer.toString(docID))) { // for each term of the document
						if (docTerm.toLowerCase().contains(queryTerm.toLowerCase())) { // if term contains queryterm
							++matchedCounter; // add counter +1
							continue;
						} else if (queryTerm.toLowerCase().contains(docTerm.toLowerCase())) { // or if queryterm contains docterm
							++matchedCounter; // add counter +1
							continue;
						}
					}

					/**
					 * Keep the two docs with the biggest query term occurence. 
					 *
					 */
					if (matchedCounter > firstBiggest) {
						secondBiggestDoc = firstBiggestDoc;
						firstBiggestDoc = docID;
						secondBiggest = firstBiggest;
						firstBiggest = matchedCounter;
					} else if (matchedCounter > secondBiggest) {
						secondBiggest = matchedCounter;
						secondBiggestDoc = docID;
					}
				}
			}

			String[] termsWithMostOccurences = new String[2];
			termsWithMostOccurences[0] = Integer.toString(firstBiggestDoc) + "-" + Integer.toString(firstBiggest);

			if (secondBiggest !=0) {
				termsWithMostOccurences[1] = Integer.toString(secondBiggestDoc) + "-" + Integer.toString(secondBiggest);
			}

			termDocOccurences.put(queryTerm, termsWithMostOccurences);
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		return termDocOccurences;
	}

	/**
	 * Read File and return String of File contents when given file path.
	 *
	 */
	static String readFile(String path, Charset encoding)  
		throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}


/**
 * Below, we have to 
 *
 */

	// private String[] getOccurences(int docID, String query_term, IndexReader reader) {
	// 	System.out.println(query_term);
	// 	String[] result = new String[100];
	// 	try {
	// 		SpanTermQuery fleeceQ = new SpanTermQuery(new Term("contents", query_term));
	// 		AtomicReader wrapper = SlowCompositeReaderWrapper.wrap(reader);
	// 		Map<Term, TermContext> termContexts = new HashMap<Term, TermContext>();
	// 		Spans spans = fleeceQ.getSpans(wrapper.getContext(), new Bits.MatchAllBits(reader.numDocs()), termContexts);
	// 		int window = 3;//get the words within two of the match
	// 		while (spans.next() == true) {
	// 			Map<Integer, String> entries = new TreeMap<Integer, String>();
	// 			System.out.println("Doc: " + spans.doc() + " Start: " + spans.start() + " End: " + spans.end());
	// 			int start = spans.start() - window;
	// 			int end = spans.end() + window;
	// 			Terms content = reader.getTermVector(spans.doc(), "contents");
	// 			TermsEnum termsEnum = content.iterator(null);
	// 			BytesRef term = null;
	// 			while ((term = termsEnum.next()) != null) {
	// 				//could store the BytesRef here, but String is easier for this example
	// 				String s = new String(term.bytes, term.offset, term.length);
	// 				DocsAndPositionsEnum positionsEnum = termsEnum.docsAndPositions(null, null);
	// 				if (positionsEnum.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
	// 					int i = 0;
	// 					int position = -1;
	// 					while (i < positionsEnum.freq() && (position = positionsEnum.nextPosition()) != -1) {
	// 						if (position >= start && position <= end) {
	// 							entries.put(position, s);
	// 						}
	// 						i++;
	// 					}
	// 				}
	// 			}
	// 			System.out.println("Entries:" + entries);
	// 		}
	// 	} catch (IOException ex) {
	// 		ex.printStackTrace();
	// 	}
	// 	return result;
	// }

	// private String[] getOccurences(int docID, String term, IndexReader reader) {
	// 	String[] result = new String[100];
	// 	try {
	// 		Terms terms = reader.getTermVector(docID, "contents"); //get terms vectors for one document and one field
	// 		if (terms != null && terms.size() > 0) {
	// 			TermsEnum termsEnum = terms.iterator(null); // access the terms for this field
	// 			BytesRef term = null;
	// 			int term_counter = 1;
	// 			while ((term = termsEnum.next()) != null) {
	// 				String term_name = term.utf8ToString();
	// 				DocsEnum docsEnum = termsEnum.docs(null, null); // enumerate through documents, in this case only one
	// 				int docIdEnum;
	// 				while ((docIdEnum = docsEnum.nextDoc()) != DocIdSetIterator.NO_MORE_DOCS && (term_counter < 100)) {
	// 					result[term_counter] = term_name + " " + docIdEnum + " " + docsEnum.freq(); //get the term frequency in the document
	// 					return result;
	// 				}
	// 			}
	// 		}
	// 	} catch (IOException ex) {
	// 		ex.printStackTrace();
	// 	}
	// 	return result;
	// }

}