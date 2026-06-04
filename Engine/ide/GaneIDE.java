package ide;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import ide.ui.EnvironmentPanel;
import ide.ui.HierarchyPanel;
import ide.ui.InspectorPanel;
import ide.ui.ConsolePanel;
import ide.utils.SceneSerializer;
import utils.NativeLibraryLoader;
import com.formdev.flatlaf.FlatDarculaLaf;

/**
 * Gane Game Engine IDE'sinin (Editör) ana baslangic sinifi.
 * Java Swing arayuzunu ve LWJGL OpenGL motorunu ayni pencerede birlestirir.
 */
public class GaneIDE {
	
	private JFrame frame;
	private ViewportCanvas viewport;
	private HierarchyPanel hierarchyPanel;
	private InspectorPanel inspectorPanel;
	private EnvironmentPanel environmentPanel;

	public GaneIDE() {
		try {
			UIManager.setLookAndFeel(new FlatDarculaLaf());
		} catch (Exception e) {
			System.err.println("FlatLaf yuklenemedi, sistem temasi kullaniliyor.");
		}
		
		frame = new JFrame("Gane Game Engine IDE v1.1.0");
		frame.setSize(1400, 800);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setLayout(new BorderLayout());
		
		// 1. Viewport (Oyun Ekrani / LWJGL Canvas)
		viewport = new ViewportCanvas();
		viewport.setPreferredSize(new Dimension(800, 600));
		
		// 2. Yan Paneller (UI)
		inspectorPanel = new InspectorPanel();
		environmentPanel = new EnvironmentPanel(viewport);
		
		JTabbedPane rightTabbedPane = new JTabbedPane();
		rightTabbedPane.addTab("Inspector", inspectorPanel);
		rightTabbedPane.addTab("Environment", environmentPanel);
		rightTabbedPane.setPreferredSize(new Dimension(300, 0));
		
		hierarchyPanel = new HierarchyPanel(viewport, inspectorPanel);
		hierarchyPanel.setPreferredSize(new Dimension(250, 0));
		
		// Mouse picking eventi
		viewport.setOnEntitySelected(entity -> {
			hierarchyPanel.selectEntity(entity);
		});

		// 3. Ekranlari Bolme (SplitPane)
		JSplitPane rightSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, viewport, rightTabbedPane);
		rightSplit.setResizeWeight(1.0); // Viewport esnek olsun (buyusun)
		
		JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hierarchyPanel, rightSplit);
		mainSplit.setResizeWeight(0.0);
		
		// 3.1. Konsol Panelini alta ekleme
		ConsolePanel consolePanel = new ConsolePanel();
		JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainSplit, consolePanel);
		verticalSplit.setResizeWeight(0.8); // Ust kisim %80, konsol %20
		
		frame.add(verticalSplit, BorderLayout.CENTER);
		
		// 4. Ust Menuler (MenuBar)
		setupMenuBar();
		
		// 5. Pencere kapanma eventi
		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeIDE();
			}
		});
		
		frame.setLocationRelativeTo(null); // Ekrani ortala
		frame.setVisible(true);

		// Engine başlatılıyor
		viewport.startEngine();
	}
	
	private void setupMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu fileMenu = new JMenu("File");
		
		JMenuItem saveItem = new JMenuItem("Save Scene (*.gane)");
		saveItem.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
			chooser.setFileFilter(new FileNameExtensionFilter("Gane Scene", "gane"));
			if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();
				String path = file.getAbsolutePath();
				if (!path.endsWith(".gane")) path += ".gane";
				SceneSerializer.saveScene(viewport.getScene(), path, viewport);
			}
		});
		
		JMenuItem loadItem = new JMenuItem("Load Scene (*.gane)");
		loadItem.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
			chooser.setFileFilter(new FileNameExtensionFilter("Gane Scene", "gane"));
			if (chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
				File file = chooser.getSelectedFile();
				SceneSerializer.loadScene(file.getAbsolutePath(), viewport, environmentPanel);
			}
		});
		
		JMenuItem exitItem = new JMenuItem("Exit");
		exitItem.addActionListener(e -> closeIDE());
		
		JMenuItem exportItem = new JMenuItem("Export as Playable Game (.java)");
		exportItem.addActionListener(e -> {
			JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			chooser.setDialogTitle("Select Java Output Folder (e.g. src/gane)");
			if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
				File folder = chooser.getSelectedFile();
				String tempGaneFile = System.getProperty("user.dir") + "/src/res/scane/export_temp.gane";
				File ganeDir = new File(System.getProperty("user.dir") + "/src/res/scane");
				if (!ganeDir.exists()) ganeDir.mkdirs();
				ide.utils.SceneSerializer.saveScene(viewport.getScene(), tempGaneFile, viewport);
				ide.utils.GameExporter.exportGame(tempGaneFile, folder.getAbsolutePath());
				javax.swing.JOptionPane.showMessageDialog(frame, "Oyun kodlari su klasore cikarildi:\n" + folder.getAbsolutePath() + "\nArtik MyGameLauncher.java uzerinden oyunu baslatabilirsiniz.");
			}
		});
		
		fileMenu.add(saveItem);
		fileMenu.add(loadItem);
		fileMenu.addSeparator();
		fileMenu.add(exportItem);
		fileMenu.addSeparator();
		fileMenu.add(exitItem);
		
		menuBar.add(fileMenu);
		frame.setJMenuBar(menuBar);
	}
	
	private void closeIDE() {
		viewport.stopEngine();
		frame.dispose();
		System.exit(0);
	}
	
	public static void main(String[] args) {
		// 1. LWJGL native kutuphaneleri yuklenir (DLL'ler)
		NativeLibraryLoader.loadNativeLibraries();
		
		// 2. Swing Arayüzü (Event Dispatch Thread) baslatilir
		SwingUtilities.invokeLater(() -> {
			new GaneIDE();
		});
	}
}
