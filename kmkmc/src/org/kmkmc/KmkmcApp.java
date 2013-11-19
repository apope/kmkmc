// Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 * Main class of the "Know Me, Know My Car" demo app.
 * This includes the background thread that polls vehicle state,
 * principal UI components, state transitions, and keyboard shortcuts
 * for demoing. 
 */
@SuppressWarnings("serial")
public class KmkmcApp extends JPanel {
  private static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
  
  /** Whether the display is showing a map, info pane, or both. */
  public enum Mode { MAP, INFO, BOTH };

  private MapClient mapClient = new MapClient();
  private Model model;
  private Model.State previousState = Model.State.STARTUP;
  private JLabel mapLabel = new JLabel();
  private JLabel infoLabel = new JLabel("INFO LABEL");
  private JTextField commandField = new JTextField();
  private JLabel timeLabel = new JLabel();
  private JLabel statusLabel = new JLabel();
  
  private VehicleDataClient vehicleDataClient;
  private Map map;
  private AudioSystem audioSystem = new AudioSystem();
  private VoiceSynthesizer voiceSynthesizer = new VoiceSynthesizer();
  private InstrumentCluster instrumentCluster;
  private boolean parkingBrakeWasOn;
  
  private int lastPlaylistChange;
  private double startingOdometer;
  private int startingTime;
  private double startingFuel;
  
  public KmkmcApp(VehicleDataClient vehicleDataClient, InstrumentCluster instrumentCluster) {
    super(new BorderLayout());
    this.vehicleDataClient = vehicleDataClient;
    this.instrumentCluster = instrumentCluster;
    mapClient.setSize(640, 640);
    Dimension d = new Dimension(640, 640);
    infoLabel.setMinimumSize(d);
    infoLabel.setPreferredSize(d);
    infoLabel.setMaximumSize(d);
    model = new Model(vehicleDataClient);
    mapLabel.setPreferredSize(new Dimension(640, 640));
    mapLabel.addMouseListener(mapMouseListener);
    
    Box box = Box.createHorizontalBox();
    box.add(Box.createHorizontalGlue());
    box.add(mapLabel);
    box.add(infoLabel);
    box.add(Box.createHorizontalGlue());
    add(box, BorderLayout.CENTER);
    Box footer = Box.createHorizontalBox();
    footer.add(Box.createHorizontalStrut(10));
    footer.add(commandField);
    footer.add(Box.createHorizontalStrut(10));
    footer.add(timeLabel);
    footer.add(Box.createHorizontalStrut(10));
    footer.add(statusLabel);
    footer.add(Box.createHorizontalStrut(10));
    statusLabel.setMinimumSize(new Dimension(300, 10));
    add(footer, BorderLayout.SOUTH);
    KeyStroke keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, 0);
    commandField.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(keyStroke, "ZoomIn");
    commandField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyTyped(KeyEvent event) {
        switch (event.getKeyChar()) {
        
        // Map zoom and recenter:
        case '+': zoomIn(); break;
        case '-': zoomOut(); break;
        case 'c': recenter(); break;
        
        // Jump to times:
        case '0': gotoTime(0); break;
        case '1': gotoTime(470); break;
        case '2': gotoTime(800); break;
        case '3': gotoTime(1780); break;
        case '4': gotoTime(1950); break;
        case '5': gotoTime(2470); break;
        case '6': gotoTime(2650); break;
        case 's': skip5(); break;
        
        // Display modes:
        case 'm': setMode(Mode.MAP); break;
        case 'i': setMode(Mode.INFO); break;
        case 'b': setMode(Mode.BOTH); break;
        
        // Info content:
        case 'A':
          showInfo("/org/kmkmc/1a-GoingToWorkTrafficAhead.png", "Hello Jane");
          break;
        case 'B':
          showInfo("/org/kmkmc/1b-GoingToWorkTrafficAhead.png", "Via see A 101"); 
          break;
        case 'C': showInfo("/org/kmkmc/2a-TodaysSchedule.png", "Today's schedule"); break;
        case 'D': showInfo("/org/kmkmc/2b-TodaysSchedule.png"); break;
        case 'E': showInfo("/org/kmkmc/2c-TodaysSchedule.png"); break;
        case 'F': showInfo("/org/kmkmc/4a-CallingDoctor.png", "Calling doctor clean teeth"); break;
        case 'G': showInfo("/org/kmkmc/4b-TalkingDoctor.png"); break;
        case 'H': showInfo("/org/kmkmc/5-CalendarPreview.png"); break;
        case 'I': showInfo("/org/kmkmc/6-PredictLongCruiseOfferLearning.png", "Resume Your Japanese Lesson?"); break;
        case 'J': 
          showInfo("/org/kmkmc/7-LanguageLesson.png");
          play("Learning Japanese");
          break;
        case 'K': 
          audioSystem.pause();
          showInfo("/org/kmkmc/8a-TrafficAlertReschedule.png", "Traffic Alert");
          break;
        case 'L': showInfo("/org/kmkmc/8b-TrafficAlertReschedule-checking.png", "Rescheduling"); break;
        case 'M': showInfo("/org/kmkmc/8c-Rescheduled.png", "Rescheduling done"); break;
        case 'N': showInfo("/org/kmkmc/10-StoppedSendGreeting.png", "Suggestion"); break;
        case 'Z':
          showTripSummary();
          break;
          
        case 'w': play("Cruising Music"); break;
        case 'x': play("Quiet Music"); break;
        case 'y': play("Learning Japanese"); break;
        case 'z': audioSystem.pause(); break;
        }
      }
    });
    (new Worker()).execute();
  }

  /** 
   * Repeatedly polls the vehicle data API, fetches an updated map image,
   * advances state, and updates the display.
   */
  private class Worker extends SwingWorker<Object, Map> {
    @Override
    protected Object doInBackground() throws Exception {
      while (true) {
	Thread.sleep(200);
      	vehicleDataClient.poll();
      	model.update();
      	mapClient.setVehicleLocation(vehicleDataClient.getLocation());
      	publish(mapClient.getMap());
      	if (model.getState() == Model.State.DEMANDING)
      	  audioSystem.pause();
      	else if ((vehicleDataClient.getTime() - lastPlaylistChange > 20) &&
      	    previousState != Model.State.CRUISING && model.getState() == Model.State.CRUISING)
      	  play("Cruising Music");
      	else if (previousState == Model.State.CRUISING && model.getState() == Model.State.ACTIVE)
          play("Quiet Music");
      	else if (previousState != Model.State.STOPPED && model.getState() == Model.State.STOPPED)
      	  audioSystem.pause();
      	previousState = model.getState();
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
    instrumentCluster.setFuel((int) vehicleDataClient.getResidualFuel());
    instrumentCluster.setSpeed(vehicleDataClient.getSpeed());
    instrumentCluster.setState(model.getState());
    int time = vehicleDataClient.getTime();
    int seconds = time % 60, minutes = (time / 60) % 60, hours = (time / 3600);
    timeLabel.setText(MessageFormat.format("{0,number,00}:{1,number,00}:{2,number,00}", hours, minutes, seconds));
    statusLabel.setText(vehicleDataClient.getStatus());
    // TODO trigger from vehicle power state
    if (model.getState() == Model.State.STOPPED &&
        vehicleDataClient.getTime() < 500) {
      instrumentCluster.setMessage("Traffic on I-280.");
      instrumentCluster.setMessageVisible(true);
    } else {
      instrumentCluster.setMessageVisible(false);
    }
    if (parkingBrakeWasOn && !vehicleDataClient.isParkingBrakeOn()) {
      showInfo("/org/kmkmc/1a-GoingToWorkTrafficAhead.png", "Hello Jane");
      startingOdometer = vehicleDataClient.getOdometer();
      startingTime = vehicleDataClient.getTime();
      startingFuel = vehicleDataClient.getResidualFuel();
    }
    parkingBrakeWasOn = vehicleDataClient.isParkingBrakeOn();
  }
  
  /** Centers the map on the point where the mouse is clicked. */
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
  
  public void zoomIn() {
    mapClient.setZoomLevel(mapClient.getZoomLevel() + 1);
  };
  public void zoomOut() {
    mapClient.setZoomLevel(mapClient.getZoomLevel() - 1);
  };
  public void recenter() {
    mapClient.setCenterOnVehicle(true);;
  };

  public void gotoTime(int time) {
    vehicleDataClient.seek(time);
  }
  public void skip5() {
    vehicleDataClient.seek(vehicleDataClient.getTime() + 5 * 60);
  };
  
  public void play(String playlist) {
    audioSystem.play(playlist);
    lastPlaylistChange = vehicleDataClient.getTime();
  }
  
  public void setMode(Mode mode) {
    boolean infoVisible = true, mapVisible = true; 
    switch (mode) {
    case MAP: infoVisible = false; break;
    case INFO: mapVisible = false; break;
    default: break;
    }
    infoLabel.setVisible(infoVisible);
    mapLabel.setVisible(mapVisible);
  }

  public void showInfo(String resource) {
    showInfo(resource, null);
  }
  
  public void showInfo(String resource, String utterance) {
    Toolkit.getDefaultToolkit().beep();
    setMode(Mode.BOTH);
    URL url = getClass().getResource(resource);
    ImageIcon image = new ImageIcon(url);
    infoLabel.setIcon(image);
    if (utterance != null)
      voiceSynthesizer.say(utterance);
  }
  
  public void showTripSummary() {
    double elapsedMiles = vehicleDataClient.getOdometer() - startingOdometer;
    int elapsedTime = vehicleDataClient.getTime() - startingTime;
    double elapsedFuel = (startingFuel - vehicleDataClient.getResidualFuel()) / 255.0 * 45 / 3.785;
    String s = MessageFormat.format(
        "<html><hr><center><h1><font size='72'>Trip Summary</h1></center><br><blockquote><font size='40'> {0,number,#.##} miles <br><br> {1,number,#.#} hours <br><br>  {2,number,#.#} gallons </font></blockquote><br><br><hr></html>",
        elapsedMiles, elapsedTime / 3600.0, elapsedFuel
        );
    infoLabel.setIcon(null);
    infoLabel.setText(s);
    infoLabel.setHorizontalAlignment(SwingConstants.CENTER);
  }
  
  /** Entry point of the "Know Me, Know My Car" application. */
  public static void main(String[] args) throws Exception {
    String userid = (args.length == 1) ? args[0] : VehicleDataClient.USER_ID_413;
    VehicleDataClient vehicleDataClient = new VehicleDataClient(userid);
    vehicleDataClient.seek(0);
    InstrumentCluster instrumentCluster = new InstrumentCluster();
    
    // Display console:
    KmkmcApp panel = new KmkmcApp(vehicleDataClient, instrumentCluster);
    JFrame frame = new JFrame("Console Display");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.getContentPane().add(panel);
    frame.setSize(1280, 640);
    frame.setVisible(true);
    
    // Display instrument cluster:
    JFrame clusterFrame = new JFrame("Instrument Cluster");
    clusterFrame.getContentPane().add(instrumentCluster);
    clusterFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    clusterFrame.pack();
    clusterFrame.setVisible(true);
  }
}
