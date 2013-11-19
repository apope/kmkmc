// Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Provides an interface to the car's audio system.
 * 
 * This implementation simulates the audio system by controlling iTunes through
 * AppleScript.
 */
public class AudioSystem {
  
  private ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("AppleScript");

  /** 
   * Starts playback of a specified playlist.
   *  
   * @param playlist the name of the playlist
   */
  public void play(String playlist) {
    command("play playlist \"" + playlist + "\"");
  }
  
  /** Resumes playback of the current playlist. */
  public void play() {
    command("play");
  }
  
  /** Pauses playback. */
  public void pause() {
    command("pause");
  }

  private void command(String verb) {
    try {
      scriptEngine.eval("tell application \"iTunes\" to " + verb);
    } catch (ScriptException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /** Unit test. */
  public static void main(String[] args) throws Exception {
    AudioSystem audio = new AudioSystem();
    audio.play();
    Thread.sleep(10000);
    audio.pause();
  }
}
