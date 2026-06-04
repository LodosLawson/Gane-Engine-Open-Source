package ide.ui;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import scene.GameObject;
import ide.ViewportCanvas;

public class HierarchyPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private DefaultListModel<scene.Entity> listModel;
	private JList<scene.Entity> entityList;
	private ViewportCanvas viewport;
	private InspectorPanel inspector;
	private java.util.List<scene.Entity> allEntities = new java.util.ArrayList<>();
	private javax.swing.JTextField searchField;
	
	public HierarchyPanel(ViewportCanvas viewport, InspectorPanel inspector) {
		this.viewport = viewport;
		this.inspector = inspector;
		
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Hierarchy (Scene Objects)"));
		
		listModel = new DefaultListModel<>();
		entityList = new JList<>(listModel);
		
		// Custom Renderer (Renk/Ikon Gosterimi)
		entityList.setCellRenderer(new javax.swing.DefaultListCellRenderer() {
			@Override
			public java.awt.Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof scene.GameObject) {
					setText("📦 " + ((scene.GameObject) value).getOriginalFilePath());
					setForeground(new java.awt.Color(169, 183, 198)); // Darcula Gray
				} else if (value instanceof scene.Light) {
					setText("💡 Light");
					setForeground(new java.awt.Color(255, 198, 109)); // Darcula Yellow
				} else {
					setText("⚙️ " + value.getClass().getSimpleName());
				}
				return this;
			}
		});
		
		// Search Bar
		searchField = new javax.swing.JTextField();
		searchField.putClientProperty("JTextField.placeholderText", "Search Entities...");
		searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
			public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
			public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
			public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
		});
		
		JPanel topPanel = new JPanel(new BorderLayout());
		topPanel.add(searchField, BorderLayout.CENTER);
		add(topPanel, BorderLayout.NORTH);
		
		// Objeye tiklaninca Inspector'i guncelle
		entityList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && entityList.getSelectedValue() != null) {
				inspector.setSelectedEntity(entityList.getSelectedValue());
			}
		});
		
		add(new JScrollPane(entityList), BorderLayout.CENTER);
		
		// Model Yukleme Butonu
		JButton addBtn = new JButton("Load 3D Model");
		addBtn.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
			chooser.setFileFilter(new FileNameExtensionFilter("3D Models (glb, obj)", "glb", "obj"));
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
				File selected = chooser.getSelectedFile();
				String absPath = selected.getAbsolutePath();
				String projectDir = System.getProperty("user.dir");
				String relPath = absPath;
				
				if (absPath.startsWith(projectDir)) {
					relPath = absPath.substring(projectDir.length());
					if (relPath.startsWith("\\") || relPath.startsWith("/")) relPath = relPath.substring(1);
					relPath = relPath.replace('\\', '/');
				}
				
				final String finalPath = relPath;
				
				// Model yukleme (VBO olusturma) OpenGL thread'inde olmali!
				viewport.enqueue(() -> {
					try {
						GameObject obj = new GameObject(finalPath);
						obj.getPosition().set(0, 0, 0);
						obj.setScale(1.0f);
						
						// Sahneye ekle
						viewport.getScene().addEntity(obj);
						
						// Arayuzdeki (Swing) listeyi guncelle
						javax.swing.SwingUtilities.invokeLater(() -> {
							allEntities.add(obj);
							filter();
							entityList.setSelectedValue(obj, true);
						});
						
					} catch(Exception ex) {
						ex.printStackTrace();
					}
				});
			}
		});
		add(addBtn, BorderLayout.SOUTH);
	}
	
	private void filter() {
		String text = searchField.getText().toLowerCase();
		listModel.clear();
		for (scene.Entity e : allEntities) {
			String name = e.getClass().getSimpleName();
			if (e instanceof scene.GameObject && ((scene.GameObject)e).getOriginalFilePath() != null) {
				name = ((scene.GameObject)e).getOriginalFilePath();
			}
			if (name.toLowerCase().contains(text)) {
				listModel.addElement(e);
			}
		}
	}
	
	public void selectEntity(scene.Entity entity) {
		if (!allEntities.contains(entity)) {
			allEntities.add(entity);
			filter();
		}
		entityList.setSelectedValue(entity, true);
	}
}
