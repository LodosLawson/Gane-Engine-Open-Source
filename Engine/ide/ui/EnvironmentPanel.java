package ide.ui;

import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.JColorChooser;
import javax.swing.JButton;
import java.awt.Color;
import org.lwjgl.util.vector.Vector3f;

import ide.ViewportCanvas;

public class EnvironmentPanel extends JPanel {

	private static final long serialVersionUID = 1L;
	private ViewportCanvas viewport;

	private JCheckBox chkTerrain;
	private JCheckBox chkOcean;
	private JCheckBox chkSky;
	private JCheckBox chkDayNight;
	private JSlider sliderTime;
	
	// Advanced Settings
	private JButton btnWaterColor;
	private JSlider sliderWaveHeight;

	public EnvironmentPanel(ViewportCanvas viewport) {
		this.viewport = viewport;
		
		setLayout(new GridLayout(12, 1, 5, 5));
		setBorder(BorderFactory.createTitledBorder("Environment Settings"));

		chkTerrain = new JCheckBox("Enable Terrain");
		chkTerrain.addActionListener(e -> viewport.setTerrainEnabled(chkTerrain.isSelected()));
		
		JButton btnCustomTerrain = new JButton("Use Custom Terrain Script");
		btnCustomTerrain.addActionListener(e -> {
		    TerrainScriptEditorDialog dialog = new TerrainScriptEditorDialog((javax.swing.JFrame) javax.swing.SwingUtilities.getWindowAncestor(this), viewport);
		    dialog.setVisible(true);
		});
		
		chkOcean = new JCheckBox("Enable Ocean (Water)");
		chkOcean.addActionListener(e -> {
			viewport.setWaterEnabled(chkOcean.isSelected());
			btnWaterColor.setEnabled(chkOcean.isSelected());
			sliderWaveHeight.setEnabled(chkOcean.isSelected());
		});
		
		btnWaterColor = new JButton("Change Water Color");
		btnWaterColor.setEnabled(false);
		btnWaterColor.addActionListener(e -> {
			Color initialColor = Color.BLUE;
			if (viewport.getWater() != null) {
				Vector3f c = viewport.getWater().getBaseColor();
				initialColor = new Color(c.x, c.y, c.z);
			}
			Color newColor = JColorChooser.showDialog(this, "Choose Water Color", initialColor);
			if (newColor != null && viewport.getWater() != null) {
				viewport.getWater().setBaseColor(newColor.getRed()/255f, newColor.getGreen()/255f, newColor.getBlue()/255f);
			}
		});
		
		sliderWaveHeight = new JSlider(0, 100, 4); // 0.0000 to 0.0100 (4 = 0.0004)
		sliderWaveHeight.setEnabled(false);
		sliderWaveHeight.addChangeListener(e -> {
			if (viewport.getWater() != null) {
				viewport.getWater().setWaveHeight(sliderWaveHeight.getValue() / 10000.0f);
			}
		});
		
		chkSky = new JCheckBox("Enable Atmosphere (Sky)");
		chkSky.addActionListener(e -> viewport.setAtmosphereEnabled(chkSky.isSelected()));
		
		chkDayNight = new JCheckBox("Enable Day/Night Cycle");
		chkDayNight.addActionListener(e -> {
			viewport.setDayNightEnabled(chkDayNight.isSelected());
			sliderTime.setEnabled(chkDayNight.isSelected());
		});

		sliderTime = new JSlider(0, 240, 120); // 0.0 ile 24.0 arasini 10 katlatiyoruz
		sliderTime.setMajorTickSpacing(60);
		sliderTime.setMinorTickSpacing(10);
		sliderTime.setPaintTicks(true);
		sliderTime.setPaintLabels(true);
		sliderTime.setEnabled(false);
		
		// Etiketleri gosterirken 10'a bolerek (0, 6, 12, 18, 24) gosterelim
		java.util.Hashtable<Integer, JLabel> labelTable = new java.util.Hashtable<>();
		labelTable.put(0, new JLabel("00:00"));
		labelTable.put(60, new JLabel("06:00"));
		labelTable.put(120, new JLabel("12:00"));
		labelTable.put(180, new JLabel("18:00"));
		labelTable.put(240, new JLabel("24:00"));
		sliderTime.setLabelTable(labelTable);

		sliderTime.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (viewport.getDayNightManager() != null) {
					viewport.getDayNightManager().setTimeOfDay(sliderTime.getValue() / 10.0f);
				}
			}
		});

		add(chkTerrain);
		add(btnCustomTerrain);
		add(chkOcean);
		add(btnWaterColor);
		add(sliderWaveHeight);
		add(chkSky);
		add(new JLabel(" ")); // Ayrac
		add(chkDayNight);
		add(new JLabel("Time of Day:"));
		add(sliderTime);
	}
	
	public void updateUIFromState() {
		chkTerrain.setSelected(viewport.getTerrain() != null);
		chkOcean.setSelected(viewport.getWater() != null);
		btnWaterColor.setEnabled(chkOcean.isSelected());
		sliderWaveHeight.setEnabled(chkOcean.isSelected());
		
		chkSky.setSelected(viewport.getAtmosphereSky() != null);
	}
	
	public void setTimeSlider(float time) {
		chkDayNight.setSelected(true);
		sliderTime.setEnabled(true);
		sliderTime.setValue((int)(time * 10));
		viewport.setDayNightEnabled(true);
		if (viewport.getDayNightManager() != null) {
			viewport.getDayNightManager().setTimeOfDay(time);
		}
	}
}
