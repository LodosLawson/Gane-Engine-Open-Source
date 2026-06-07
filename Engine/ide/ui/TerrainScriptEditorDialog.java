package ide.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ide.ViewportCanvas;

public class TerrainScriptEditorDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JTextArea codeArea;
	private JTextField classNameField;

	public TerrainScriptEditorDialog(JFrame parent, ViewportCanvas viewport) {
		super(parent, "Custom Terrain Script Editor", true);
		setSize(650, 550);
		setLocationRelativeTo(parent);
		setLayout(new BorderLayout(5, 5));
		
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(new JLabel("Class Adı:"));
		classNameField = new JTextField("MyCustomTerrain", 20);
		topPanel.add(classNameField);
		add(topPanel, BorderLayout.NORTH);
		
		codeArea = new JTextArea();
		codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
		
		String defaultCode = 
			"package scripts;\n\n" +
			"import terrain.flat.FlatTerrain;\n\n" +
			"public class MyCustomTerrain extends FlatTerrain {\n" +
			"    public MyCustomTerrain() {\n" +
			"        super(2000, 2000);\n" +
			"        // Kendinize ozel parametrelerle arazi uretin:\n" +
			"        this.generateProceduralTerrainV2(120.0f, 0.6f, 6, 200.0f, 9999L);\n" +
			"    }\n" +
			"}\n";
			
		// Var olan script kontrolu
		if (viewport.getCustomTerrainClassName() != null) {
		    String scriptName = viewport.getCustomTerrainClassName().replace("scripts.", "");
		    classNameField.setText(scriptName);
		    File sourceFile = new File(System.getProperty("user.dir") + "/src/scripts/" + scriptName + ".java");
		    if (sourceFile.exists()) {
		        try {
		            defaultCode = new String(java.nio.file.Files.readAllBytes(sourceFile.toPath()));
		        } catch (Exception ex) {
		            ex.printStackTrace();
		        }
		    }
		}

		codeArea.setText(defaultCode);
		add(new JScrollPane(codeArea), BorderLayout.CENTER);
		
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton compileBtn = new JButton("Compile & Set Custom Terrain");
		compileBtn.addActionListener(e -> {
			String className = classNameField.getText().trim();
			String source = codeArea.getText();
			
			if (className.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Sinif adi bos olamaz!", "Hata", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try {
			    // RuntimeCompiler'i kullanarak derle (Terrain FlatTerrain dondurmeli)
				Object obj = ide.scripting.RuntimeCompiler.compileAndLoad(className, source, terrain.flat.FlatTerrain.class);
				if (obj instanceof terrain.flat.FlatTerrain) {
				    viewport.setCustomTerrain((terrain.flat.FlatTerrain) obj, "scripts." + className);
				    JOptionPane.showMessageDialog(this, "Custom Terrain basariyla derlendi ve eklendi!");
				    dispose();
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, "Derleme Hatasi:\n" + ex.getMessage(), "Compiler Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		
		bottomPanel.add(compileBtn);
		add(bottomPanel, BorderLayout.SOUTH);
	}
}
