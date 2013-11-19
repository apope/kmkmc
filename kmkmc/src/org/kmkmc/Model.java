// Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/** Models and estimates the state of current driving conditions. */
public class Model {

  /** Length of history kept for each car measurement. */
  private static final int SAMPLE_COUNT = 5;  // 5 seconds

  /** Categories of driving conditions. */
  public static enum State { STARTUP, STOPPED, CRUISING, ACTIVE, DEMANDING };
  
  /** Most recent estimate of driving conditions. */
  private State state;
  
  /** Supplies data about the car. */
  private VehicleDataClient vehicleDataClient;
  
  // The history and statistics of each of several car measurements.
  private ArrayBlockingQueue<Double> acceleratorHistory = new ArrayBlockingQueue<Double>(SAMPLE_COUNT);
  private Statistics acceleratorStatistics = new Statistics();
  private ArrayBlockingQueue<Double> brakeHistory = new ArrayBlockingQueue<Double>(SAMPLE_COUNT);
  private Statistics brakeStatistics = new Statistics();
  private ArrayBlockingQueue<Double> lateralAccelerationHistory = new ArrayBlockingQueue<Double>(SAMPLE_COUNT);
  private Statistics lateralAccelerationStatistics = new Statistics();
  private ArrayBlockingQueue<Double> longitudinalAccelerationHistory = new ArrayBlockingQueue<Double>(SAMPLE_COUNT);
  private Statistics longitudinalAccelerationStatistics = new Statistics();
  private ArrayBlockingQueue<Double> speedHistory = new ArrayBlockingQueue<Double>(SAMPLE_COUNT);
  private Statistics speedStatistics = new Statistics();
  private ArrayBlockingQueue<Double> yawRateHistory = new ArrayBlockingQueue<Double>(SAMPLE_COUNT);
  private Statistics yawRateStatistics = new Statistics();

  /** Statistics of some car measurement. */
  private static class Statistics {
    public int count;
    public double mean, min, max, sd;
    public String toString() {
      return "count=" + count + ", mean=" + mean + ", min=" + min + ", max=" + max + ", sd=" + sd;
    }
  }
  
  /** Constructs a model that will obtain car data from a specified VehicleDataClient. */
  public Model(VehicleDataClient vehicleDataClient) {
    this.vehicleDataClient = vehicleDataClient;
  }
  
  /** Gets the most recent state estimate. */
  public State getState() {
    return state;
  }
  
  /** Resets the state estimator, clearing all history. */
  public void reset() {
    state = State.STARTUP;
    acceleratorHistory.clear();
    brakeHistory.clear();
    lateralAccelerationHistory.clear();
    longitudinalAccelerationHistory.clear();
    speedHistory.clear();
    yawRateHistory.clear();
  }
  
  /** Updates the estimated state using the most recent car measurements. */
  public void update() {
    update(acceleratorHistory, vehicleDataClient.getAcceleratorPedalRatio(), acceleratorStatistics);
    update(brakeHistory, vehicleDataClient.isBrakeOn() ? 1 : 0, brakeStatistics);
    update(lateralAccelerationHistory, vehicleDataClient.getLateralAcceledation(), lateralAccelerationStatistics);
    update(longitudinalAccelerationHistory, vehicleDataClient.getLongitudinalAcceleration(), longitudinalAccelerationStatistics);
    update(speedHistory, vehicleDataClient.getSpeed(), speedStatistics);
    update(yawRateHistory, Math.abs(vehicleDataClient.getYawRate()), yawRateStatistics);
    
    if (speedHistory.size() < SAMPLE_COUNT)
      state = State.STARTUP;
    else if (speedStatistics.max == 0 && brakeStatistics.min == 1)
      state = State.STOPPED;
    else if (speedStatistics.sd < 10 && speedStatistics.mean > 40)
      state = State.CRUISING;
    else if (yawRateStatistics.mean > 5 && speedStatistics.mean > 30)
      state = State.DEMANDING;
    else {
      state = State.ACTIVE;
    }
    // System.out.println(state + "  speed: " + speedStatistics);
    // System.out.println(state + "  yawRate: " + yawRateStatistics);
  }

  /** Updates the history and statistics of a particular car measurement. */
  private void update(Queue<Double> samples, double sample, Statistics statistics) {
    if (samples.size() == SAMPLE_COUNT)
      samples.remove();
    samples.add(sample);
    computeStatistics(samples, statistics);
  }
  
  /** Updates the statistics of a particular car measurement. */
  private void computeStatistics(Collection<Double> samples, Statistics statistics) {
    statistics.count = samples.size();
    statistics.min = Double.MAX_VALUE;
    statistics.max = 0;
    double sum = 0;
    for (double sample : samples) {
      sum += sample;
      if (sample < statistics.min)
        statistics.min = sample;
      if (sample > statistics.max)
        statistics.max = sample;
    }
    if (samples.size() >= 1)
      statistics.mean = sum / samples.size();
    if (samples.size() >= 2) {
      sum = 0;
      for (double sample : samples) {
        double t = (sample - statistics.mean);
        sum += t * t;
      }
      statistics.sd = Math.sqrt(sum / (samples.size() - 1));
    }
  }
}