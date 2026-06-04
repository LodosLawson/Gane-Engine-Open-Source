package ide.ui;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.JButton;
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
	
	public InspectorPanel() {
		setLayout(new GridLayout(8, 2, 5, 5));
		setBorder(BorderFactory.createTitledBorder("Inspector (Properties)"));
		
		add(new JLabel("Pos X:")); posX = new JTextField(); add(posX);
		add(new JLabel("Pos Y:")); posY = new JTextField(); add(posY);
		add(new JLabel("Pos Z:")); posZ = new JTextField(); add(posZ);
		
		add(new JLabel("Rot X:")); rotX = new JTextField(); add(rotX);
		add(new JLabel("Rot Y:")); rotY = new JTextField(); add(rotY);
		add(new JLabel("Rot Z:")); rotZ = new JTextField(); add(rotZ);
		
		add(new JLabel("Scale:")); scaleTxt = new JTextField(); add(scaleTxt);
		
		JButton applyBtn = new JButton("Apply Changes");
		applyBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				applyValues();
			}
		});
		
		JButton scriptBtn = new JButton("Add/Edit Script");
		scriptBtn.addActionListener(e -> {
			if (selectedEntity instanceof GameObject) {
				ScriptEditorDialog dialog = new ScriptEditorDialog((javax.swing.JFrame) SwingUtilities.getWindowAncestor(this), (GameObject) selectedEntity);
				dialog.setVisible(true);
			} else {
				javax.swing.JOptionPane.showMessageDialog(this, "Lutfen sahneden bir GameObject secin!", "Uyari", javax.swing.JOptionPane.WARNING_MESSAGE);
			}
		});
		
		add(scriptBtn);
		add(applyBtn);
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
		} else {
			posX.setText(""); posY.setText(""); posZ.setText("");
			rotX.setText(""); rotY.setText(""); rotZ.setText("");
			scaleTxt.setText("");
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
		} catch(NumberFormatException ex) {
			System.err.println("Gecersiz sayi formati. (Ondaliklar icin . kullanin)");
		}
	}
}
