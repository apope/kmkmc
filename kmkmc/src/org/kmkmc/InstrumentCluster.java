// Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.text.DecimalFormat;

import javax.swing.GroupLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;

/** A UI components emulating the instrument cluster. */
@SuppressWarnings("serial")
public class InstrumentCluster extends JPanel {
  private DecimalFormat speedFormat = new DecimalFormat("### mph");
  
  private JProgressBar fuelGauge = new JProgressBar(SwingConstants.VERTICAL);
  private JLabel fullLabel = new JLabel("F");
  private JLabel emptyLabel = new JLabel("E");
  private JPanel centerPanel = new JPanel();
  private CardLayout centerPanelLayout = new CardLayout();
  private JLabel speedLabel = new JLabel("0 mph");
  private JTextArea textField = new JTextArea();
  private JLabel modeLabel1 = new JLabel();
  private JLabel modeLabel2 = new JLabel();
  private JLabel modeLabel3 = new JLabel();
  private JLabel modeLabel4 = new JLabel();
  private Dimension modeLabelSize = new Dimension(60, 45);

  /** Construct an instrument cluster. */
  public InstrumentCluster() {
    setBackground(Color.black);
    GroupLayout layout = new GroupLayout(this);
    setLayout(layout);
    layout.setAutoCreateContainerGaps(true);
    layout.setHorizontalGroup(layout.createSequentialGroup()
        .addComponent(fuelGauge)
        .addGroup(layout.createParallelGroup()    
            .addComponent(fullLabel)
            .addComponent(emptyLabel))
        .addGap(60)
        .addComponent(centerPanel, 100, 600, 1000)
        .addGap(60)
        .addGroup(layout.createParallelGroup()
            .addComponent(modeLabel1)
            .addComponent(modeLabel2)
            .addComponent(modeLabel3)
            .addComponent(modeLabel4)));
    layout.setVerticalGroup(layout.createParallelGroup()
        .addComponent(fuelGauge, 40, 100, 1000)
        .addGroup(layout.createSequentialGroup()
            .addComponent(fullLabel)
            .addGap(10, 30, 1000)
            .addComponent(emptyLabel))
        .addGroup(layout.createSequentialGroup()
            .addGap(0, 20, 100)
            .addComponent(centerPanel)
            .addGap(0, 20, 100))
        .addGroup(layout.createSequentialGroup()
            .addGap(0, 0, 1000)
            .addComponent(modeLabel4)
            .addGap(2, 2, 2)
            .addComponent(modeLabel3)
            .addGap(2, 2, 2)
            .addComponent(modeLabel2)
            .addGap(2, 2, 2)
            .addComponent(modeLabel1)));
    fuelGauge.setMinimum(0);
    fuelGauge.setMaximum(255);
    fullLabel.setForeground(Color.cyan);
    fullLabel.setFont(new Font("Dialog", Font.BOLD, 24));
    emptyLabel.setForeground(Color.cyan);
    emptyLabel.setFont(new Font("Dialog", Font.BOLD, 24));
    speedLabel.setOpaque(true);
    speedLabel.setBackground(Color.black);
    speedLabel.setForeground(Color.orange);
    speedLabel.setFont(new Font("Monospaced", Font.BOLD | Font.ITALIC, 72));
    speedLabel.setHorizontalAlignment(SwingConstants.TRAILING);
    textField.setForeground(Color.green);
    textField.setBackground(Color.black);
    textField.setFont(new Font("Dialog", Font.BOLD, 48));
    textField.setEditable(false);
    centerPanel.setLayout(centerPanelLayout);
    centerPanel.add(speedLabel, "speed");
    centerPanel.add(textField, "text");
    modeLabel1.setMinimumSize(modeLabelSize);
    modeLabel1.setPreferredSize(modeLabelSize);
    modeLabel1.setMaximumSize(modeLabelSize);
    modeLabel2.setMinimumSize(modeLabelSize);
    modeLabel2.setPreferredSize(modeLabelSize);
    modeLabel2.setMaximumSize(modeLabelSize);
    modeLabel3.setMinimumSize(modeLabelSize);
    modeLabel3.setPreferredSize(modeLabelSize);
    modeLabel3.setMaximumSize(modeLabelSize);
    modeLabel4.setMinimumSize(modeLabelSize);
    modeLabel4.setPreferredSize(modeLabelSize);
    modeLabel4.setMaximumSize(modeLabelSize);
    modeLabel1.setOpaque(true);
    modeLabel2.setOpaque(true);
    modeLabel3.setOpaque(true);
    modeLabel4.setOpaque(true);
    setState(Model.State.STARTUP);
  }

  /** Sets the fuel level displayed by the instrument cluster. */
  public void setFuel(int fuel) {
    fuelGauge.setForeground(Color.cyan);
    fuelGauge.setStringPainted(true);
    fuelGauge.setValue(fuel);
  }

  /** 
   * Sets the text message displayed in the center of the instrument
   * cluster.
   */
  public void setMessage(String text) {
    textField.setText(text);
  }

  /** 
   * Sets whether the center of the instrument cluster displays
   * speed (false) or a text message (true).
   */
  public void setMessageVisible(boolean b) {
    centerPanelLayout.show(centerPanel, b ? "text" : "speed");
  }

  /** Sets the speed displayed by the instrument cluster. */
  public void setSpeed(double speed) {
    speedLabel.setText(speedFormat.format(speed));
  }
  
  public void setState(Model.State state) {
    Color color1 = Color.black, color2= Color.black , color3 = Color.black, color4 = Color.black;
    switch (state) {
    case STARTUP: break;
    case STOPPED: color1 = Color.gray; break;
    case CRUISING: color1 = color2 = Color.green; break;
    case ACTIVE: color1 = color2 = color3 = Color.yellow; break;
    case DEMANDING: color1 = color2 = color3  = color4 = Color.orange; break; 
    }
    modeLabel1.setBackground(color1);
    modeLabel2.setBackground(color2);
    modeLabel3.setBackground(color3);
    modeLabel4.setBackground(color4);
  }
  
  /** Unit test. */
  public static void main(String[] args) throws Exception {
    JFrame frame = new JFrame("Instrument Cluster");
    InstrumentCluster cluster = new InstrumentCluster();
    frame.getContentPane().add(cluster);
    frame.pack();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setVisible(true);
    cluster.setSpeed(55);
    cluster.setFuel(128);
    cluster.setState(Model.State.STARTUP);
    Thread.sleep(1000);
    cluster.setState(Model.State.STOPPED);
    Thread.sleep(1000);
    cluster.setState(Model.State.CRUISING);
    Thread.sleep(1000);
    cluster.setState(Model.State.ACTIVE);
    Thread.sleep(1000);
    cluster.setState(Model.State.DEMANDING);
    Thread.sleep(1000);
    cluster.setMessage("This is a test\nmessage.");
    cluster.setMessageVisible(true);
  }
}
