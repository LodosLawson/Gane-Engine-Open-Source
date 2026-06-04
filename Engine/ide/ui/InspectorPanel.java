package ide.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import scene.Entity;
import scene.GameObject;
import ide.ui.ScriptEditorDialog;

public class InspectorPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;
	private Entity selectedEntity;
	
	private JTextField posX, posY, posZ;
	private JTextField rotX, rotY, rotZ;
	private JTextField scaleTxt;
	private JCheckBox chkTransparent, chkShadow, chkReflection;
	
	public InspectorPanel() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createTitledBorder("Inspector (Properties)"));
		
		// 1. Transform Panel
		JPanel transformPanel = new JPanel(new GridLayout(4, 2, 5, 5));
		transformPanel.setBorder(BorderFactory.createTitledBorder("Transform"));
		transformPanel.add(new JLabel("Pos X:")); posX = new JTextField(); transformPanel.add(posX);
		transformPanel.add(new JLabel("Pos Y:")); posY = new JTextField(); transformPanel.add(posY);
		transformPanel.add(new JLabel("Pos Z:")); posZ = new JTextField(); transformPanel.add(posZ);
		transformPanel.add(new JLabel("Rot X:")); rotX = new JTextField(); transformPanel.add(rotX);
		transformPanel.add(new JLabel("Rot Y:")); rotY = new JTextField(); transformPanel.add(rotY);
		transformPanel.add(new JLabel("Rot Z:")); rotZ = new JTextField(); transformPanel.add(rotZ);
		transformPanel.add(new JLabel("Scale:")); scaleTxt = new JTextField(); transformPanel.add(scaleTxt);
		
		// 2. Material Panel
		JPanel materialPanel = new JPanel(new GridLayout(3, 1));
		materialPanel.setBorder(BorderFactory.createTitledBorder("Material & Render"));
		chkTransparent = new JCheckBox("Is Transparent (Glass/Leaves)");
		chkShadow = new JCheckBox("Casts Shadow");
		chkReflection = new JCheckBox("Has Reflection (Water)");
		materialPanel.add(chkTransparent);
		materialPanel.add(chkShadow);
		materialPanel.add(chkReflection);
		
		// 3. Actions Panel
		JPanel actionsPanel = new JPanel(new GridLayout(2, 1, 5, 5));
		actionsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
		JButton applyBtn = new JButton("Apply Changes");
		applyBtn.addActionListener(e -> applyValues());
		
		JButton scriptBtn = new JButton("Add/Edit Script");
		scriptBtn.addActionListener(e -> {
			if (selectedEntity instanceof GameObject) {
				ScriptEditorDialog dialog = new ScriptEditorDialog((javax.swing.JFrame) SwingUtilities.getWindowAncestor(this), (GameObject) selectedEntity);
				dialog.setVisible(true);
			} else {
				javax.swing.JOptionPane.showMessageDialog(this, "Lutfen sahneden bir GameObject secin!", "Uyari", javax.swing.JOptionPane.WARNING_MESSAGE);
			}
		});
		actionsPanel.add(applyBtn);
		actionsPanel.add(scriptBtn);
		
		// Hizalamalar
		transformPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		materialPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		actionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		
		add(transformPanel);
		add(Box.createVerticalStrut(10));
		add(materialPanel);
		add(Box.createVerticalStrut(10));
		add(actionsPanel);
	}
	
	public void setSelectedEntity(Entity entity) {
		this.selectedEntity = entity;
		if (entity != null) {
			posX.setText(String.format(java.util.Locale.US, "%.2f", entity.getPosition().x));
			posY.setText(String.format(java.util.Locale.US, "%.2f", entity.getPosition().y));
			posZ.setText(String.format(java.util.Locale.US, "%.2f", entity.getPosition().z));
			
			rotX.setText(String.format(java.util.Locale.US, "%.2f", entity.getRotation().x));
			rotY.setText(String.format(java.util.Locale.US, "%.2f", entity.getRotation().y));
			rotZ.setText(String.format(java.util.Locale.US, "%.2f", entity.getRotation().z));
			
			scaleTxt.setText(String.format(java.util.Locale.US, "%.2f", entity.getScale()));
			
			if (entity.getSkin() != null) {
				chkTransparent.setSelected(entity.getSkin().isTransparent());
			} else {
				chkTransparent.setSelected(false);
			}
			chkShadow.setSelected(entity.isShadowCasting());
			chkReflection.setSelected(entity.hasReflection());
			
		} else {
			posX.setText(""); posY.setText(""); posZ.setText("");
			rotX.setText(""); rotY.setText(""); rotZ.setText("");
			scaleTxt.setText("");
			chkTransparent.setSelected(false);
			chkShadow.setSelected(false);
			chkReflection.setSelected(false);
		}
	}
	
	public void applyValues() {
		if (selectedEntity == null) return;
		try {
			selectedEntity.getPosition().x = Float.parseFloat(posX.getText());
			selectedEntity.getPosition().y = Float.parseFloat(posY.getText());
			selectedEntity.getPosition().z = Float.parseFloat(posZ.getText());
			
			selectedEntity.getRotation().x = Float.parseFloat(rotX.getText());
			selectedEntity.getRotation().y = Float.parseFloat(rotY.getText());
			selectedEntity.getRotation().z = Float.parseFloat(rotZ.getText());
			
			selectedEntity.setScale(Float.parseFloat(scaleTxt.getText()));
			
			// Skin Update
			if (selectedEntity.getSkin() != null) {
				selectedEntity.getSkin().setTransparent(chkTransparent.isSelected());
			}
			// Model attributes
			selectedEntity.setCastsShadow(chkShadow.isSelected());
			selectedEntity.setHasReflection(chkReflection.isSelected());
			
			System.out.println("Ozelikler guncellendi: " + selectedEntity.getClass().getSimpleName());
		} catch(NumberFormatException ex) {
			System.err.println("Gecersiz sayi formati. (Ondaliklar icin . kullanin)");
		}
	}
}
