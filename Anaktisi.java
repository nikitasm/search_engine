import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.text.DefaultCaret;
import java.awt.EventQueue;

import java.io.*;
import org.apache.commons.io.FileUtils;
import java.nio.file.Files;
import java.nio.charset.Charset;
import java.nio.file.Paths;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
 * Creates the indexes inside the index folder. Given the path of files fore indexing, it 
 * uses as analyzer the PorterstemAnalyzer that we created. Each word is indexed 
 * after put through a stemming to avoid common words.
 * 
 */
public class Anaktisi extends JPanel implements ListSelectionListener {
	private JList list;

	private DefaultListModel listModel;
	private static final String searchString = "Search";
	private static final String openString = "Open";
	private static final String indexString = "Index";
	private static final String clearIndexString = "Clear Index";
	private JButton searchButton;
	private JButton indexButton;
	private JButton clearIndexButton;
	private JTextField nameField;
	private JTextArea log;
	private int maxHits = 1000;
	static private String newline = "\n";
	private String fileContents = "";

	public Anaktisi() {
		super(new BorderLayout()); // Setting up the Layout.

		listModel = new DefaultListModel(); // Instantiating the default listModel, that will carry the results.
		listModel.addElement("");

		list = new JList(listModel); // The list that will output the listModel.
		list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION); // Setting up multiple interval selection.
		list.addListSelectionListener(this); // Set up listener. He reads the current values selected, on click on the list.

		JScrollPane listScrollPane = new JScrollPane(list);

		/**
		 * Setting up our buttons. For each button, a listener is instantiated. 
		 * 
		 */
		searchButton = new JButton(searchString);
		searchButton.setActionCommand(searchString);
		searchButton.addActionListener(new SearchButtonListener());
		searchButton.setToolTipText("Click to search in the location of index folder");

		indexButton = new JButton(indexString);
		indexButton.setActionCommand(indexString);
		indexButton.addActionListener(new IndexButtonListener());
		indexButton.setToolTipText("Click to select the location of the files");

		clearIndexButton = new JButton(clearIndexString);
		clearIndexButton.setActionCommand(clearIndexString);
		clearIndexButton.addActionListener(new ClearIndexButtonListener());
		clearIndexButton.setToolTipText("Click to clear the Index folder");

		JPanel indexPanel = new JPanel(new GridLayout(2, 1));
		indexPanel.add(indexButton);
		indexPanel.add(clearIndexButton);

		nameField = new JTextField(15); // Setting up the text field that will carry our queries.
		String name = "";
		nameField.setText(name);

		JPanel buttonPane = new JPanel();
		buttonPane.add(nameField);
		buttonPane.add(searchButton);
		buttonPane.add(indexPanel);

		log = new JTextArea(10, 20);
		JScrollPane logScrollPane = new JScrollPane(log);

		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
				listScrollPane, logScrollPane);
		splitPane.setResizeWeight(0.5);

		add(buttonPane, BorderLayout.PAGE_START);
		add(splitPane, BorderLayout.CENTER);
	}

	/**
	 * SearchButtonListener class, set up on searchButton, handles the search. After the user inputs
	 * the query, he searches inside the index folder for the top score hits for which our query match.
	 * 
	 */
	class SearchButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String query = nameField.getText(); // get the query string from the input field
			int[] selectedIndices = list.getSelectedIndices(); // check for the selected documents
			String[] selectedDocuments = new String[100];
			Boolean shouldSearch = false; // for searching inside specific documents, this value should be false.
			
			if (selectedIndices.length > 0) {
				shouldSearch = true; // if selected documents, this value is true

				/**
				 * For each document that is selected, get the docID(that can be found in the IndexReader)
				 * 
				 */
				for(int i=0;i<(selectedIndices.length);i++) {
					if (selectedIndices[i] != 0) { 
						String item = listModel.getElementAt(selectedIndices[i]).toString();
						String[] result_parts = item.split(" ");
						String docID = result_parts[2];
						selectedDocuments[i] = docID;
					}
				}
			}

			listModel.removeAllElements(); // before each output, reset the list
			log.setText(null); // before each output, reset the output area.

			if (query.equals("")) {
				Toolkit.getDefaultToolkit().beep(); //don't do any query if query string is empty
				return;
			}

			int size = listModel.getSize();
			nameField.setText(query);

			/**
			 * Instantiate the searcher and search inside the index folder for the query string.
			 * 
			 */
			try {
				Searcher s = new Searcher(query, shouldSearch, selectedDocuments);	

				String[] filenames = s.getFileNames(); // 
				int searchLength = 0;

				if (filenames.length <= maxHits) {
					searchLength = filenames.length;
				} else {
					searchLength = maxHits+1;
				}

				for(int i=0;i<(searchLength);i++) {
					String untrimmed = filenames[i];
					String[] untrimmed_parts = untrimmed.split(" ");
					String fullFileName = untrimmed_parts[1];
					String[] fullFileNameParts = fullFileName.split("/");
					listModel.addElement(implodeArray(untrimmed_parts, " ")); // for each matched doc, append inside the listModel.
				}
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		}
	}


	/**
	 * IndexButtonListener class, handles the index button actions. On click, a popup is introduced to the user
	 * and after the user selects file folder, it starts indexing.
	 * 
	 */
	class IndexButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			String indexLocation = getDir(); // get the IO input from the user 

			if (indexLocation == null) {
				Toolkit.getDefaultToolkit().beep();
				return;
			}
			log.setText(null); // on every index, reset log area.

			try {
				Indexer indexer = new Indexer(indexLocation);
				String[] results = indexer.getMessages(); // the results of the indexing
				for (int i=0; i<results.length; i=i+1) {
					String result = results[i];
					log.append(result + "\n"); // for each document, append it's output
				}
			}
			catch (Exception e1) {
				JOptionPane.showMessageDialog (null, "Cannot create index", "Problem", JOptionPane.ERROR_MESSAGE);
			}
		}
	}


	/**
	 * Like IndexButtonListener, this listener handles only clearButton action. This
	 * action is cleawring the index folder.
	 * 
	 */
	class ClearIndexButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			clearIndexDir(); // on click, clear index folder.
		}
	}

	/**
	 * If any value on the listmodel is selected, it updates the log area with the selected document(s)' contents.
	 * 
	 */
	public void valueChanged(ListSelectionEvent e) {
		if (e.getValueIsAdjusting() == false) { // if this is the last event in the chain.
			int[] selected = list.getSelectedIndices(); // get the current selected documents
			updateLog(selected); // update the log with the selected documents's contents
		}
	}

	/**
	 * This is called on the main() function of the program. It instantiates all the GUI( The frames, etc)
	 * 
	 */
	private static void createAndShowGUI() {
		JFrame.setDefaultLookAndFeelDecorated(true);

		JFrame frame = new JFrame("Anaktisi");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		JComponent newContentPane = new Anaktisi();
		newContentPane.setOpaque(true);
		frame.setContentPane(newContentPane);

		newContentPane.setMinimumSize(new Dimension(newContentPane
				.getPreferredSize().width, 100));

		frame.pack();
		frame.setVisible(true);
	}


	/**
	 * The main function of the program, it calls the createAndShowGUI() which then loads everything.
	 * 
	 */
	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	/**
	 * Pops a window filechooser and gets the dir chosen by the user. It is set by default only for directories
	 * 
	 */
	private String getDir() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int result = fileChooser.showOpenDialog(this);

		if (result == JFileChooser.CANCEL_OPTION) {
			return null;
		}
		File fileDir = fileChooser.getSelectedFile();

		return fileDir.getPath();
	}


	/**
	 * On click of the clearIndexButton, it clears the index folder.
	 * 
	 */
	boolean clearIndexDir() {
		try {
			File outputIndexDir = new File("index");

			if (!isDirEmpty(outputIndexDir)) {
				FileUtils.cleanDirectory(outputIndexDir);
				return true;
			}
			return false;
		} catch (Exception ex) {
			return false;
		}
	}


	/**
	 * Checks if the dir given is empty
	 * 
	 */
	private static boolean isDirEmpty(final File f) {
		try {
			if(f.isDirectory() && f.exists()) {
				if (f.list().length == 0) {
					return true;
				}
				return false;
			}
			return false;
		} catch (Exception ex) {
			return false;
		}
	}


	/**
	 * Each time called, it pdates the log area with the contents of the  
	 * 
	 */
	private void updateLog(int[] selectedIndices) {
		log.setText(null);
		fileContents = "";

		for(int i=0;i<(selectedIndices.length);i++) {
			String item = listModel.getElementAt(selectedIndices[i]).toString();
			String[] result_parts = item.split(" ");
			String fileName = result_parts[1];
			try {
				fileContents += readFile(fileName, Charset.defaultCharset()) + "\n";
			} catch (IOException ex) {
			}
		}
		log.setText(fileContents.replace("/", ""));
	}


	/**
	 * It reads the file and outputs a string containing them.
	 * 
	 */
	static String readFile(String path, Charset encoding) 
		throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public static String implodeArray(String[] inputArray, String glueString) {
	/** Output variable */
	String output = "";

	if (inputArray.length > 0) {
		StringBuilder sb = new StringBuilder();
		sb.append(inputArray[0]);

		for (int i=1; i<inputArray.length; i++) {
			sb.append(glueString);
			sb.append(inputArray[i]);
		}

		output = sb.toString();
	}

	return output;
	}
}