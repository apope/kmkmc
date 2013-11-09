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

  public void play() {
    command("play");
  }
  
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

  public static void main(String[] args) throws Exception {
    AudioSystem audio = new AudioSystem();
    audio.play();
    Thread.sleep(10000);
    audio.pause();
  }
}
