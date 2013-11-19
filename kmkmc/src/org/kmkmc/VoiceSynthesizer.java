// Copyright (c) 2013 Art Pope. All rights reserved.

package org.kmkmc;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

/**
 * Provides an interface to the car's voice synthesizer.
 * 
 * This implementation uses AppleScript commands to emit utterances.
 */
public class VoiceSynthesizer {
  
  private ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("AppleScript");

  /** Outputs a phrase. */
  public void say(String phrase) {
    try {
      scriptEngine.eval("say \"" + phrase + "\"");
    } catch (ScriptException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /** Unit test. */
  public static void main(String[] args) throws Exception {
    VoiceSynthesizer synth = new VoiceSynthesizer();
    synth.say("Hello world");
  }
}
