package ide.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class ConsolePanel extends JPanel {
	private static final long serialVersionUID = 1L;
	private JTextPane textPane;
	private StyledDocument doc;
	
	private Style defaultStyle;
	private Style errorStyle;

	public ConsolePanel() {
		setLayout(new BorderLayout());
		
		textPane = new JTextPane();
		textPane.setEditable(false);
		textPane.setBackground(new Color(43, 43, 43)); // Dark background
		
		doc = textPane.getStyledDocument();
		
		defaultStyle = textPane.addStyle("Default", null);
		StyleConstants.setForeground(defaultStyle, new Color(169, 183, 198)); // Darcula foreground
		StyleConstants.setFontFamily(defaultStyle, "Consolas");
		
		errorStyle = textPane.addStyle("Error", null);
		StyleConstants.setForeground(errorStyle, new Color(255, 107, 104)); // Error red
		StyleConstants.setFontFamily(errorStyle, "Consolas");

		JScrollPane scrollPane = new JScrollPane(textPane);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		add(scrollPane, BorderLayout.CENTER);
		
		redirectSystemStreams();
	}

	private void redirectSystemStreams() {
		OutputStream out = new OutputStream() {
			@Override
			public void write(int b) {
				appendString(String.valueOf((char) b), defaultStyle);
			}
			@Override
			public void write(byte[] b, int off, int len) {
				appendString(new String(b, off, len), defaultStyle);
			}
		};

		OutputStream err = new OutputStream() {
			@Override
			public void write(int b) {
				appendString(String.valueOf((char) b), errorStyle);
			}
			@Override
			public void write(byte[] b, int off, int len) {
				appendString(new String(b, off, len), errorStyle);
			}
		};

		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(err, true));
	}

	private void appendString(String text, Style style) {
		SwingUtilities.invokeLater(() -> {
			try {
				doc.insertString(doc.getLength(), text, style);
				textPane.setCaretPosition(doc.getLength()); // Auto-scroll
			} catch (BadLocationException e) {
				e.printStackTrace();
			}
		});
	}
}
