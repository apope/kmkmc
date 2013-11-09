// Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

/**
 *  Represents the geographic latitude and longitude of a point.
 */
public class GeographicPoint {
  
  /** Geodetic latitude, in degrees. */
  private double latitude;
  
  /** Longitude in degrees. */
  private double longitude;
  
  /**
   * Constructs a GeographicPoint with specified coordinates.
   * 
   * @param latitude the geodetic latitude in degrees
   * @param longitude the longitude in degrees
   */
  public GeographicPoint(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }
  
  /** Gets the geodetic latitude in degrees. */
  public double getLatitude() {
    return latitude;
  }
  
  /** Gets the longitude in degrees. */
  public double getLongitude() {
    return longitude;
  }
  
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(latitude);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(longitude);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    GeographicPoint other = (GeographicPoint) obj;
    if (Double.doubleToLongBits(latitude) != Double.doubleToLongBits(other.latitude))
      return false;
    if (Double.doubleToLongBits(longitude) != Double.doubleToLongBits(other.longitude))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return latitude + "," + longitude;
  }
}
