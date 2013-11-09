// Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

/**
 * Renders map images for car navigation using the Google Static Maps API.
 * This maintains current map rendering settings (center location, zoom level, etc.),
 * and supplies maps rendered according to those set
 */
public class MapClient {
  // View parameters:
  private GeographicPoint centerLocation = new GeographicPoint(0, 0);
  private int zoomLevel = 14;
  private int width = 640, height = 640;
  private boolean centerOnVehicle = true;
  
  // Content parameters:
  private GeographicPoint vehicleLocation;
  
  /** The most recently rendered map image. */
  private Map lastMap;

  /** Gets the location at which map images will be centered. */
  public GeographicPoint getCenterLocation() {
    return centerLocation;
  }
  
  /** 
   * Sets the location at which map images will be centered.
   * Also turns off the mode in which the map is automatically
   * centered on the vehicle.
   */
  public void setCenterLocation(GeographicPoint centerLocation) {
    this.centerLocation = centerLocation;
    this.setCenterOnVehicle(false);
  }

  /** Gets the zoom level. */
  public int getZoomLevel() {
    return zoomLevel;
  }
  
  /** Sets the zoom level. */
  public void setZoomLevel(int zoomLevel) {
    this.zoomLevel = Math.max(2, Math.min(20, zoomLevel));
  }
  
  /** Sets the size of rendered map images, in pixels. */
  public void setSize(int width, int height) {
    this.width = width;
    this.height = height;
  }

  /** Gets whether vehicle auto-centering mode is on. */
  public boolean isCenterOnVehicle() {
    return centerOnVehicle;
  }
  
  /** Sets vehicle auto-centering mode on or off. */
  public void setCenterOnVehicle(boolean centerOnVehicle) {
    this.centerOnVehicle = centerOnVehicle;
  }
  
  /**
   * Sets the location at which the vehicle is displayed. If
   * vehicle auto-centering mode is on, this also adjusts the
   * location of the map center.
   */
  public void setVehicleLocation(GeographicPoint vehicleLocation) {
    this.vehicleLocation = vehicleLocation;
    if (centerOnVehicle)
      this.centerLocation = vehicleLocation;
  }

  /** Gets a map rendering according to current settings. */
  public Map getMap() {
    Map map = new Map(centerLocation, zoomLevel, width, height, vehicleLocation);
    if (!map.equals(lastMap)) {  // otherwise reuse the last map
      map.render();
      lastMap = map;
    }
    return lastMap;
  }
  
  /** Unit test. Retrieves and displays a map. */
  public static void main(String[] args) throws Exception {
    MapClient client = new MapClient();
    client.setVehicleLocation(new GeographicPoint(37.429167, -122.138056));
    Map map = client.getMap();
    JLabel label = new JLabel(new ImageIcon(map.getImage()));
    JFrame frame = new JFrame("MapClient Test");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(label);
    frame.pack();
    frame.setVisible(true);
  }
}
