// Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

/**
 * Displays a map showing the location of a car, plus controls for panning
 * and zooming the map.
 */
@SuppressWarnings("serial")
public class NavPanel extends JPanel {
  private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  
  private MapClient mapClient = new MapClient();
  private JLabel mapLabel = new JLabel();
  
  private AbstractAction zoomInAction= new AbstractAction("+") {
    @Override
    public void actionPerformed(ActionEvent arg0) {
      mapClient.setZoomLevel(mapClient.getZoomLevel() + 1);
    }
  };
  private AbstractAction zoomOutAction= new AbstractAction("-") {
    @Override
    public void actionPerformed(ActionEvent arg0) {
      mapClient.setZoomLevel(mapClient.getZoomLevel() - 1);
    }
  };
  private AbstractAction recenterAction= new AbstractAction("Recenter") {
    @Override
    public void actionPerformed(ActionEvent arg0) {
      mapClient.setCenterOnVehicle(true);;
    }
  };

  private JButton zoomInButton = new JButton(zoomInAction);
  private JButton zoomOutButton = new JButton(zoomOutAction);
  private JButton recenterButton = new JButton(recenterAction);
  private VehicleDataClient vehicleDataClient;
  private Map map;
  
  public NavPanel(String vid) {
    super(new BorderLayout());
    mapLabel.addComponentListener(new ComponentAdapter() {

      @Override
      public void componentResized(ComponentEvent event) {
	mapClient.setSize(
	    Math.min(mapLabel.getWidth(), 640), 
	    Math.min(mapLabel.getHeight(), 640));
      }
    });
    vehicleDataClient = new VehicleDataClient(vid);
    mapLabel.setPreferredSize(new Dimension(640, 640));
    mapLabel.addMouseListener(mapMouseListener);
    Box buttonPanel = Box.createHorizontalBox();
    buttonPanel.add(zoomInButton);
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(recenterButton);
    buttonPanel.add(Box.createHorizontalGlue());
    buttonPanel.add(zoomOutButton);
    add(mapLabel, BorderLayout.CENTER);
    add(buttonPanel, BorderLayout.SOUTH);
    (new Worker()).execute();
  }
  
  private class Worker extends SwingWorker<Object, Map> {
    @Override
    protected Object doInBackground() throws Exception {
      while (true) {
	Thread.sleep(1000);
      	vehicleDataClient.poll();
      	mapClient.setVehicleLocation(vehicleDataClient.getLocation());
      	publish(mapClient.getMap());
      }
    }

    @Override
    protected void process(List<Map> maps) {
      setMap(maps.get(maps.size() - 1));
    }
  }
  
  public void setMap(Map map) {
    this.map = map;
    mapLabel.setIcon(new ImageIcon(map.getImage()));
  }
  
  private MouseListener mapMouseListener = new MouseAdapter() {
    @Override
    public void mouseClicked(MouseEvent event) {
      if (map != null) {
	GeographicPoint location = map.mapImageToGeographic(event.getPoint());
	logger.fine("Centering on " + location);
	mapClient.setCenterLocation(location);
      }
    }
  };
  
  public static void main(String[] args) throws Exception {
    NavPanel panel = new NavPanel(VehicleDataClient.USER_ID_411);
    JFrame frame = new JFrame("NavPanel Test");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(panel);
    frame.pack();
    frame.setVisible(true);
  }
}
