//Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

import java.io.InputStream;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * Maintains a local copy of the state of a vehicle, which it updates by polling
 * the web service. The vehicle tracked is identified by a userID supplied to
 * the constructor.
 */
public class VehicleDataClient {
  private static final String HOST = "api-jp-t-itc.com";
  private static final String API_KEY = System.getProperty("kmkmc.carApiKey", "<missing kmkmc.carApiKey value");
  public static final String USER_ID_411 = "usSF-411";
  public static final String USER_ID_413 = "usSF-413";
  
  private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);

  /** The vehicle whose state we're tracking. */
  private String userid;
  
  // Most recent values the web service has reported for our vehicle.
  
  private String vid;
  private GeographicPoint location = new GeographicPoint(0, 0);
  private double speed, lateralAcceleration, longitudinalAcceleration, yawRate;
  private double acceleratorPedalRatio;  // [0,100]
  private boolean brakeOn;
  private double steeringAngle, engineRpm, residualFuel, engineTemperature, outsideTemperature; 

  /** Names of parameters for which we request data from the server. */
  private static final String[] parameter_names = { "MapMatching", 
    "Spd", "ALatStdByEsc", "ALgtStd", "YawRateStd",
    "AccrPedlRat", "BrkLiIntenReq", "SteerWhlAgBas",
    "EngN", "RestFu", "EngT", "OutdT" };
  
  /** URI used to request the current values of parameters from the server. */  
  private String dataRequest;
  
  private DocumentBuilder builder;
  private XPath xpath;
  
  /** Constructs a VehicleDataClient for monitoring the state of a specified vehicle. */
  public VehicleDataClient(String userID) {
    this.userid = userID;
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(false);
    try {
      builder = factory.newDocumentBuilder();
    } catch (ParserConfigurationException pce) {
      logger.severe(pce.toString());
    }
    xpath = XPathFactory.newInstance().newXPath();
    
    StringBuilder sb = new StringBuilder(generatePreamble("GetVehicleInfo"));
    sb.append("&now");
    for (String p : parameter_names)
      sb.append("&").append(p).append("=1");
    dataRequest = sb.toString();
  }
  
  /** Gets the ID of the vehicle we're monitoring. */
  public String userid() { return userid; }
  
  // Access methods for current state

  public String vid() { return vid; }
  public GeographicPoint getLocation() { return location; }
  
  public double getSpeed() { return speed; }
  public double getLateralAcceledation() { return lateralAcceleration; }
  public double getLongitudinalAcceleration() { return longitudinalAcceleration; }
  public double getYawRate() { return yawRate; }
  
  public double getAcceleratorPedalRatio() { return acceleratorPedalRatio; }
  public boolean isBrakeOn() { return brakeOn; }
  public double getSteeringAngle() { return steeringAngle; }
  public double getEngineRpm() { return engineRpm; }
  public double getResidualFuel() { return residualFuel; }
  public double getEngineTemperature() { return engineTemperature; }
  public double getOutsideTemperature() { return outsideTemperature; }
  
  /** Sends a "SearchDataReset" to the web service. */
  public void reset() {
    callServer(generatePreamble("SearchDataReset"));
  }

  /** Polls the web service for the latest values of vehicle parameters, and records those. */
  public void poll() {
    String response = null;
    response = Boolean.getBoolean("org.kmkmc.useSampleData") ? SAMPLE_DATA : callServer(dataRequest); 
    InputSource is = new InputSource();
    is.setCharacterStream(new StringReader(response));
    try {
      Document doc = builder.parse(is);
      String actualUserid = xpath.evaluate("/response/carinfo/car/userid", doc);
      if (!actualUserid.equals(userid)) {
	logger.severe("Responding userid (" + actualUserid + ") doesn't match request (" + userid + ")");
	return;
      }
      vid = xpath.evaluate("/response/carinfo/car/vid", doc);
      double latitude = extractDouble(doc, "MapMatching/lat", location.getLatitude());
      double longitude = extractDouble(doc, "MapMatching/lon", location.getLongitude());
      location = new GeographicPoint(latitude, longitude);
      speed = extractDouble(doc, "Spd", speed);
      lateralAcceleration = extractDouble(doc, "ALatStdByEsc", lateralAcceleration);
      longitudinalAcceleration = extractDouble(doc, "ALgtStd", longitudinalAcceleration);
      yawRate = extractDouble(doc, "YawRateStd", yawRate);
      acceleratorPedalRatio= extractDouble(doc, "AccrPedlRat", acceleratorPedalRatio);
      brakeOn = extractBoolean(doc, "BrkLiIntenReq", brakeOn);
      steeringAngle= extractDouble(doc, "SteerWhlAgBas", steeringAngle);
      engineRpm= extractDouble(doc, "EngN", engineRpm);
      residualFuel= extractDouble(doc, "RestFu", residualFuel);
      engineTemperature= extractDouble(doc, "EngT", engineTemperature);
      outsideTemperature = extractDouble(doc, "OutdT", outsideTemperature);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to build or query DOM document", e);
    }
  }

  private String generatePreamble(String service) {
//  return String.format("/DataSender/services/%s?apilkey=%s&vid=%s", service, API_KEY, vid);
    return String.format("/DataSender/services/%s?apilkey=%s&userid=%s", service, API_KEY, "usSF-411");
  }

  /** 
   * Sends a request to the web service and returns the response received.
   * 
   * @param request the "file" portion of the URL at which the web service is
   * contacted. It includes web service access point plus parameters.
   * @return response received from the web service
   */
  private String callServer(String request) {
    try {
      URL url = new URL("https", HOST, request);
      if (logger.isLoggable(Level.FINE))
	logger.fine("Query: " + url);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      String s = convertStreamToString(connection.getInputStream());
      if (logger.isLoggable(Level.FINE))
	logger.fine("Response: " + s);
      return s;
    } catch (Exception e) {
      logger.severe("Error contacting web service: " + e.getMessage());
      return "";
    }
  }
  
  private boolean extractBoolean(Document doc, String name, boolean dflt) {
    String s = extractString(doc, name, dflt ? "1" : "1");
    try {
      return Integer.parseInt(s) > 0;
    } catch (NumberFormatException nfe) {
      // logger.warning("Bad " + name + " value \"" + s + "\": " + nfe.getMessage());
      return dflt;
    }
  }
  
  private double extractDouble(Document doc, String name, double dflt) {
    String s = extractString(doc, name, Double.toString(dflt));
    try {
      return Double.parseDouble(s);
    } catch (NumberFormatException nfe) {
      // logger.warning("Bad " + name + " value \"" + s + "\": " + nfe.getMessage());
      return dflt;
    }
  }
  
  private String extractString(Document doc, String name, String dflt) {
    String expression = "/response/carinfo/data/" + name;
    try {
      Node node = (Node) xpath.evaluate(expression, doc, XPathConstants.NODE);
      if (node == null) {
	logger.warning("Missing " + expression + " element");
	return dflt;
      }
      return node.getTextContent(); 
    } catch (XPathExpressionException xpee) {
      logger.severe("Error extracting " + name + ": " + xpee.getMessage());
      return dflt;
    }
  }

  private static String convertStreamToString(InputStream is) {
    Scanner s = new Scanner(is).useDelimiter("\\A");
    return s.hasNext() ? s.next() : "";
  }

  /** 
   * Sample data for testing response parsing.
   * This is substituted if -Dorg.kmkmc.useSampleData=true.
   */
  private static final String SAMPLE_DATA = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n\n" +
      "<response>\n" +
      "<carinfo>\n" +
      "<car>\n" +
      "	<userid>12345678901</userid>\n" + 
      "	<vid>12345678901234561</vid>\n" +
      "</car>\n" +
      "<data>\n" +
      "	<createtime>2013-11-01T12:23:58</createtime>\n" +
      "	<MapMatching>\n" +
      "		<lat>34.659628333</lat>\n" +
      "		<lon>135.164106667</lon>\n" +
      "	</MapMatching>\n" +
      "	<Spd>0.00</Spd>\n" +
      " <ALatStdByEsc>0.287800000000001</ALatStdByEsc>\n" +
      " <ALgtStd>0.431359999999998</ALgtStd>\n" +
      " <YawRateStd>-0.316000000000003</YawRateStd>\n" +
      "	<AccrPedlRat>0</AccrPedlRat>\n" +
      "	<BrkLiIntenReq>1</BrkLiIntenReq>\n" +
      "	<SteerWhlAgBas>-22.5</SteerWhlAgBas>\n" +
      "	<EngN>0</EngN>\n" +
      " <RestFu>100</RestFu>\n" +
      " <EngT>50</EngT>\n" +
      " <OutdT>67</OutdT>\n" +
      "</data>\n" +
      "</carinfo>\n" +
      "\n" +
      "</response>\n";

  /** Unit test. Polls the web service several times and prints vehicle lat/lon after each. */
  public static void main(String[] args) throws Exception {
    VehicleDataClient client = new VehicleDataClient(VehicleDataClient.USER_ID_411);
    client.reset();
    for (int i = 0; i < 5; ++i) {
      client.poll();
      System.out.println(client.getLocation());
      Thread.sleep(1000);
    }
  }
}
