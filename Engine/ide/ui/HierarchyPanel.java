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
	
	public HierarchyPanel(ViewportCanvas viewport, InspectorPanel inspector) {
		this.viewport = viewport;
		this.inspector = inspector;
		
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Hierarchy (Scene Objects)"));
		
		listModel = new DefaultListModel<>();
		entityList = new JList<>(listModel);
		
		// Objeye tiklaninca Inspector'i guncelle
		entityList.addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting()) {
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
							listModel.addElement(obj);
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
	
	public void selectEntity(scene.Entity entity) {
		if (!listModel.contains(entity)) {
			listModel.addElement(entity);
		}
		entityList.setSelectedValue(entity, true);
	}
}
