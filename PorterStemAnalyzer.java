import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import java.io.*;


class PorterStemAnalyzer extends Analyzer {
	private static CharArraySet _stopTable;
	public static final String[] STOP_WORDS =
	{"a", "about", "above", "accordingly", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", 
		"along", "already", "also", "although", "always", "am", "among", "amongst", "an", "and", "another", "any", "anybody", "anyhow", "anyone", "anything", 
		"anywhere", "apart", "are", "around", "as", "aside", "at", "away", "awfully", "b", "be", "became", "because", "become", "becomes", "becoming", "been", 
		"before", "beforehand", "behind", "being", "below", "beside", "besides", "best", "better", "between", "beyond", "both", "brief", "but", "by", "c", "can", 
		"cannot", "cant", "certain", "co", "consequently", "could", "d", "did", "do", "does", "doing", "done", "down", "downwards", "during", "e", "each", "eg", 
		"eight", "either", "else", "elsewhere", "enough", "et", "etc", "even", "ever", "every", "everybody", "everyone", "everything", "everywhere", "ex", "except", 
		"f", "far", "few", "fifth", "first", "five", "for", "former", "formerly", "forth", "four", "from", "further", "furthermore", "g", "get", "gets", "go", 
		"gone", "got", "h", "had", "hardly", "has", "have", "having", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", 
		"him", "himself", "his", "hither", "how", "howbeit", "however", "i", "ie", "if", "immediate", "in", "inasmuch", "inc", "indeed", "inner", "insofar", "instead", 
		"into", "inward", "is", "it", "its", "itself", "j", "just", "k", "keep", "kept", "l", "last", "latter", "latterly", "least", "less", "lest", "like", "little", 
		"ltd", "m", "many", "may", "me", "meanwhile", "might", "more", "moreover", "most", "mostly", "much", "must", "my", "myself", "n", "namely", "near", "neither", 
		"never", "nevertheless", "new", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "novel", "now", "nowhere", "o", "of", "off", "often", 
		"oh", "old", "on", "once", "one", "ones", "only", "onto", "or", "other", "others", "otherwise", "ought", "our", "ours", "ourselves", "out", "outside", "over", 
		"overall", "own", "p", "particular", "particularly", "per", "perhaps", "please", "plus", "probably", "q", "que", "quite", "r", "rather", "really", "relatively", 
		"respectively", "right", "s", "said", "same", "second", "secondly", "see", "seem", "seemed", "seeming", "seems", "self", "selves", "sensible", "serious", "seven", 
		"several", "shall", "she", "should", "since", "six", "so", "some", "somebody", "somehow", "someone", "something", "sometime", "sometimes", "somewhat", "somewhere", 
		"still", "sub", "such", "sup", "t", "than", "that", "the", "their", "theirs", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", 
		"therein", "thereupon", "these", "they", "third", "this", "thorough", "thoroughly", "those", "though", "three", "through", "throughout", "thru", "thus", "to", 
		"together", "too", "toward", "towards", "twice", "two", "u", "under", "until", "unto", "up", "upon", "us", "v", "various", "very", "via", "vs", "viz", "w", 
		"was", "we", "well", "went", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", 
		"wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "x", 
		"y", "yet", "you", "your", "yours", "yourself", "yourselves", "z", "zero", "/*", "manual", "unix", "programmer's", "file", "files", "used", "name", 
		"specified", "value", "given", "return", "use", "following", "current", "using", "normally", "returns", "returned", "causes", "described", "contains", 
		"example", "possible", "useful", "available", "associated", "would", "cause", "provides", "taken", "unless", "sent", "followed", "indicates", "currently", 
		"necessary", "specify", "contain", "indicate", "appear", "different", "indicated", "containing", "gives", "placed", "uses", "appropriate", "automatically", 
		"ignored", "changes", "way", "usually", "allows", "corresponding", "specifying"
	};

	PorterStemAnalyzer()
	{
		this(STOP_WORDS);
	}

	/**
	 * Build a Porter Stem Analyzer with the given stop words.
	 *
	 * @param stopWords a String array of stop words
	 */
	public PorterStemAnalyzer(String[] stopWords)
	{
		_stopTable = StopFilter.makeStopSet(stopWords, false);
	}

	protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
		Tokenizer tokenizer = new LowerCaseTokenizer(reader);
		TokenStream filteredStream = new PorterStemFilter(
			new StopFilter(tokenizer, _stopTable));

		return new TokenStreamComponents(tokenizer, filteredStream);
	}

}