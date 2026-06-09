package ide.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import ide.scripting.RuntimeCompiler;
import scene.Component;
import scene.GameObject;

public class ScriptEditorDialog extends JDialog {

	private static final long serialVersionUID = 1L;
	private JTextArea codeArea;
	private JTextField classNameField;

	public ScriptEditorDialog(JFrame parent, GameObject targetObject) {
		super(parent, "Script Editor", true);
		setSize(600, 500);
		setLocationRelativeTo(parent);
		setLayout(new BorderLayout(5, 5));
		
		// Ust panel: Sinif adi
		JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		topPanel.add(new JLabel("Class Adı:"));
		classNameField = new JTextField("MyCustomScript", 20);
		topPanel.add(classNameField);
		add(topPanel, BorderLayout.NORTH);
		
		// Orta panel: Kod alani
		codeArea = new JTextArea();
		codeArea.setFont(new Font("Consolas", Font.PLAIN, 14));
		
		String defaultCode = 
			"package scripts;\n\n" +
			"import scene.Component;\n" +
			"import scene.GameObject;\n\n" +
			"public class MyCustomScript extends Component {\n" +
			"    @Override\n" +
			"    public void update(float delta) {\n" +
			"        // Bu kod her karede (frame) calisir\n" +
			"        if (gameObject != null) {\n" +
			"            gameObject.getRotation().y += 50.0f * delta;\n" +
			"        }\n" +
			"    }\n" +
			"}\n";
			
		// Objede daha once eklenmis bir script varsa, kodunu yukle
		java.util.List<String> existingScripts = targetObject.getScriptClassNames();
		if (!existingScripts.isEmpty()) {
		    String scriptName = existingScripts.get(0).replace("scripts.", "");
		    classNameField.setText(scriptName);
		    java.io.File sourceFile = new java.io.File(System.getProperty("user.dir") + "/src/scripts/" + scriptName + ".java");
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
		
		// Alt panel: Butonlar
		JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton compileBtn = new JButton("Compile & Attach to Object");
		compileBtn.addActionListener(e -> {
			String className = classNameField.getText().trim();
			String source = codeArea.getText();
			
			if (className.isEmpty()) {
				JOptionPane.showMessageDialog(this, "Sinif adi bos olamaz!", "Hata", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			try {
				// Derle ve instance'i al
				Component comp = RuntimeCompiler.compileAndLoad(className, source);
				// Objeye ekle
				targetObject.addComponent(comp);
				JOptionPane.showMessageDialog(this, "Script basariyla derlendi ve objeye eklendi!");
				dispose(); // Kapat
			} catch (Exception ex) {
				ex.printStackTrace();
				JOptionPane.showMessageDialog(this, "Derleme Hatasi:\n" + ex.getMessage(), "Compiler Error", JOptionPane.ERROR_MESSAGE);
			}
		});
		
		bottomPanel.add(compileBtn);
		add(bottomPanel, BorderLayout.SOUTH);
	}
}
