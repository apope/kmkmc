// Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

import java.awt.Image;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * Encapsulates a rendered map image, plus the parameters controlling how it was
 * rendered.
 * 
 * After constructing an instance, invoke render() to render an image, then
 * getImage() to get that image. The render() call should be made off the event
 * thread as it involves a web service call.
 */
public class Map {
  public static final String API_KEY = System.getProperty("kmkmc.mapApiKey", "<missing kmkmc.mapApiKey value");
  public static final String HOST = "maps.googleapis.com";
  private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  private GeographicPoint vehicleLocation;
  private int zoomLevel, width, height;
  private GeographicPoint centerLocation;
  private String request;
  private Image image;

  /** 
   * Constructs a Map with specified parameters. This notes those parameter values but
   * does not yet render the map (call render() for that). 
   */
  public Map(GeographicPoint centerLocation, int zoomLevel, int width, int height, GeographicPoint vehicleLocation) {
    this.centerLocation = centerLocation;
    this.zoomLevel = zoomLevel;
    this.width = width;
    this.height = height;
    this.vehicleLocation = vehicleLocation;

    request = String.format(
      	"//maps/api/staticmap?key=%s&sensor=true&center=%g,%g&zoom=%d&size=%dx%d&markers=label:V|%g,%g&visual_refresh=true", 
      	API_KEY, 
      	centerLocation.getLatitude(), centerLocation.getLongitude(),
      	zoomLevel, width, height,
      	vehicleLocation.getLatitude(), vehicleLocation.getLongitude());
  }

  /** Renders the map. Best done in a background thread. */
  public void render() {
    if (image != null)
      return;  // already done
    try {
      URL url = new URL("https", HOST, request);
      if (logger.isLoggable(Level.FINE))
        logger.fine("Query: " + url);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      image = ImageIO.read(connection.getInputStream());
      if (logger.isLoggable(Level.FINE))
        logger.fine("Response: " + image);
    } catch (Exception e) {
      logger.severe("Error contacting web service: " + e.getMessage());
      image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_BGR);
    }
  }
  
  /** Gets the rendered map image. */
  public Image getImage() {
    if (image == null) {
      logger.warning("Map wasn't already rendered.");
      render();
    }
    return image;
  }
  
  // COORDINATE MAPPING

  // The following is based on
  // https://developers.google.com/maps/documentation/javascript/examples/map-coordinates?csw=1

  private static final float TILE_SIZE = 256;
  private static final Point2D.Double pixelOrigin = new Point2D.Double(TILE_SIZE / 2, TILE_SIZE / 2);
  private static final double pixelsPerLonDegree = TILE_SIZE / 360.0;
  private static final double pixelsPerLonRadian = TILE_SIZE / (2 * Math.PI);

  /** Maps a point from geographic coordinates to world coordinates. */
  public Point2D.Double mapGeographicToWorld(GeographicPoint geographicPoint) {
    // Truncating to 0.9999 effectively limits latitude to 89.189. This is
    // about a third of a tile past the edge of the world tile.
    double x = TILE_SIZE / 2 + centerLocation.getLongitude() * (256 / 360.0);
    double siny = Math.min(Math.max(Math.sin(Math.toRadians(geographicPoint.getLatitude())), -.9999), .9999);
    double y = TILE_SIZE / 2 + 0.5 * Math.log((1 + siny) / (1 - siny)) * -TILE_SIZE / (2 * Math.PI);
    return new Point2D.Double(x, y);
  }

  /** Maps a point from world coordinate to geographic ones. */
  public GeographicPoint mapWorldToGeographic(Point2D worldPoint) {
    double lng = (worldPoint.getX() - pixelOrigin.x) / pixelsPerLonDegree;
    double latRadians = (worldPoint.getY() - pixelOrigin.y) / -pixelsPerLonRadian;
    double lat = Math.toDegrees(2 * Math.atan(Math.exp(latRadians)) - Math.PI / 2);
    return new GeographicPoint(lat, lng);
  }

  /** Maps a point from image coordinates to geographic ones. */
  public GeographicPoint mapImageToGeographic(Point2D imagePoint) {
    Point2D.Double centerWorldPoint = mapGeographicToWorld(centerLocation);
    double px = imagePoint.getX() - width / 2.0, py = imagePoint.getY() - height / 2.0;
    double numTiles = 1 << zoomLevel;
    double x = centerWorldPoint.getX() + px / numTiles, y = centerWorldPoint.getY() + py / numTiles;
    return mapWorldToGeographic(new Point2D.Double(x, y));
  }
  
  // IDENTITY

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((centerLocation == null) ? 0 : centerLocation.hashCode());
    result = prime * result + height;
    result = prime * result + ((request == null) ? 0 : request.hashCode());
    result = prime * result + ((vehicleLocation == null) ? 0 : vehicleLocation.hashCode());
    result = prime * result + width;
    result = prime * result + zoomLevel;
    return result;
  }
  
  /** 
   * Tests whether this object is equal to another. This does not compare
   * map images, only the parameters used to render map images. Thus it can
   * be used before invoking render() (see {@link MapClient#getMap()}).
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Map other = (Map) obj;
    if (centerLocation == null) {
      if (other.centerLocation != null)
        return false;
    } else if (!centerLocation.equals(other.centerLocation))
      return false;
    if (height != other.height)
      return false;
    if (request == null) {
      if (other.request != null)
        return false;
    } else if (!request.equals(other.request))
      return false;
    if (vehicleLocation == null) {
      if (other.vehicleLocation != null)
        return false;
    } else if (!vehicleLocation.equals(other.vehicleLocation))
      return false;
    if (width != other.width)
      return false;
    if (zoomLevel != other.zoomLevel)
      return false;
    return true;
  }
}