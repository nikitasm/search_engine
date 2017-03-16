import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.core.LowerCaseTokenizer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import java.util.Stack;
import javax.swing.JOptionPane;

import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import org.apache.lucene.document.FieldType;

import java.io.*;
import java.util.ArrayList;


public class Indexer
{
	private String[] messages = null;
	private int numIndexed = 0;
	Stack<String> stack = new Stack<String>();
	private FileReader fr;
	private ArrayList<File> queue = new ArrayList<File>();
	private ArrayList<File> skipped = new ArrayList<File>();
	private String defaultIndexDir = ".";

	/**
	 * Constructor
	 * @param inputIndexDir the name of a text file or a folder we wish to add to the index
	 * @throws java.io.IOException when exception creating index.
	 */
	Indexer(String inputIndexDir) throws Exception {
		PorterStemAnalyzer analyzer = new PorterStemAnalyzer();

		File defaultIndexDir = createIndexDir();

		try {
			dirsAreValid(inputIndexDir);
		} catch (Exception ex) {
			stack.push(new String("Cannot create index..." + ex.getMessage()));
			throw new Exception(getErrorMessage());	
		}

		FSDirectory outputDir = FSDirectory.open(defaultIndexDir);
		IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);
		IndexWriter writer = new IndexWriter(outputDir, config);

		long start = System.currentTimeMillis();
		try {
			numIndexed = indexFileOrDirectory(new File(inputIndexDir), writer);
		} catch (Exception ex) {
			throw new Exception(getErrorMessage());	
		}

		long end = System.currentTimeMillis();

		messages = new String[numIndexed];

		JOptionPane.showMessageDialog (null, "Indexing " + numIndexed + " files took " + (end - start) + " milliseconds." + '\n' + skipped.size() + " documents skipped.");
	}

	/**
	 * Indexes a file or directory
	 * @param file the file or folder we wish to add to the index
	 * @throws java.io.IOException when exception
	 */
	public int indexFileOrDirectory(File file, IndexWriter writer) throws Exception {
		int originalNumDocs = writer.numDocs();
		try {
			String fileName = file.getName();
			addFiles(file);
		} catch (Exception ex) {
			stack.push(new String("Cannot create index..." + ex.getMessage()));
			throw new Exception(getErrorMessage());
		}

		for (File f : queue) {
			if (!f.isHidden() &&
				f.exists() &&
				f.canRead()) {
				try {
					Document doc = new Document();

					FieldType type = new FieldType();
					type.setStoreTermVectors(true);
					type.setStoreTermVectorOffsets(true);
					type.setStoreTermVectorPositions(true);
					type.setIndexed(true);
					type.setTokenized(true);

					fr = new FileReader(f);
					doc.add(new Field("contents", readFile(f.getAbsolutePath(), Charset.defaultCharset()), type));
					doc.add(new StringField("path", f.getPath(), Field.Store.YES));
					doc.add(new StringField("filename", f.getName(), Field.Store.YES));

					writer.addDocument(doc);
					stack.push(new String("Indexing " + f));
				} catch (Exception ex) {
					ex.printStackTrace();
					stack.push(new String("Cannot create index..." + ex.getMessage()));
					throw new Exception(getErrorMessage());
				} finally {
					fr.close();
				}
			}
		}

		int finalNumDocs = writer.numDocs();

		numIndexed = finalNumDocs - originalNumDocs;
		queue.clear();
		closeIndex(writer);

		return numIndexed;
	}

	private void addFiles(File file) {
		if (!file.exists()) {
			stack.push(new String(file + " does not exist."));
		}

		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				addFiles(f);
			}
		} else {
			String filename = file.getName().toLowerCase();
			if (filename.endsWith(".htm") || filename.endsWith(".html") || 
					filename.endsWith(".xml") || filename.endsWith(".txt")) {
				queue.add(file);
			} else {
				skipped.add(file);
				stack.push(new String("Skipped " + filename));
			}
		}
	}

	/**
	 * Close the index.
	 * @throws java.io.IOException when exception closing
	 */
	public void closeIndex(IndexWriter writer) throws IOException {
		writer.close();
	}

	public String[] getMessages() {
		int i = numIndexed - 1;
		while(!stack.isEmpty() && i >= 0) {
			String message = stack.pop();
			messages[i] = message;
			i--;
		}
		return messages;
	}

	public String getErrorMessage() {
		return stack.pop();
	}

	private File createIndexDir() throws IOException {
		File outputIndexDir = new File("index");

		if (!outputIndexDir.exists()) {
			boolean result = false;
			try {
				outputIndexDir.mkdir();
				result = true;
			} catch(SecurityException se) {
				stack.push(new String("Directory index/ cannot be created "));
				getErrorMessage();
			}
		}
		return outputIndexDir;
	}

	private Boolean dirsAreValid(String inputIndexDir) throws IOException {
		File inputDir = new File(inputIndexDir);

		if (!inputDir.exists() || !inputDir.isDirectory()) {
			throw new IOException(inputIndexDir + " does not exist or is not a directory");
		}

		File outDir = new File("index");

		if (!outDir.exists() || !outDir.isDirectory()) {
			throw new IOException(inputIndexDir + " does not exist or is not a directory");
		}

		return true;
	}

	static String readFile(String path, Charset encoding) 
		throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}
}