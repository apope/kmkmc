package org.kmkmc;

import java.util.Locale;

import javax.speech.EngineCreate;
import javax.speech.EngineList;
import javax.speech.EngineStateError;
import javax.speech.synthesis.JSMLException;
import javax.speech.synthesis.SpeakableEvent;
import javax.speech.synthesis.SpeakableListener;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerModeDesc;
import javax.speech.synthesis.Voice;

import com.sun.speech.freetts.jsapi.FreeTTSEngineCentral;

public class Speaker implements SpeakableListener {

  public void createSynthesizer() {
    try {
      SynthesizerModeDesc desc = new SynthesizerModeDesc(null, "general",  // use "time" or "general"
          Locale.US, Boolean.FALSE, null);

      FreeTTSEngineCentral central = new FreeTTSEngineCentral();
      EngineList list = central.createEngineList(desc);

      if (list.size() > 0) {
        EngineCreate creator = (EngineCreate) list.get(0);
        synthesizer = (Synthesizer) creator.createEngine();
      }
      if (synthesizer == null) {
        System.err.println("Cannot create synthesizer");
        System.exit(1);
      }
      synthesizer.allocate();
      synthesizer.resume();
      

      /* Choose the voice.
       */
      String voiceName = "kevin";
      desc = (SynthesizerModeDesc) synthesizer.getEngineModeDesc();
      Voice[] voices = desc.getVoices();
      Voice voice = null;
      for (int i = 0; i < voices.length; i++) {
          if (voices[i].getName().equals(voiceName)) {
              voice = voices[i];
              break;
          }
      }
      if (voice == null) {
          System.err.println(
              "Synthesizer does not have a voice named "
              + voiceName + ".");
          System.exit(1);
      }
      synthesizer.getSynthesizerProperties().setVoice(voice);

      /* The the synthesizer to speak and wait for it to
       * complete.
       */
      synthesizer.speakPlainText("Hello world!", null);
      synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);
      
      /* Clean up and leave.
       */
      synthesizer.deallocate();
      

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  
  public void say(String text) {
    try {
      synthesizer.speak(text, this);
    } catch (JSMLException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (EngineStateError e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  
  @Override
  public void wordStarted(SpeakableEvent arg0) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void topOfQueue(SpeakableEvent arg0) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void speakableStarted(SpeakableEvent arg0) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void speakableResumed(SpeakableEvent arg0) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void speakablePaused(SpeakableEvent arg0) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void speakableEnded(SpeakableEvent arg0) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void speakableCancelled(SpeakableEvent arg0) {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public void markerReached(SpeakableEvent arg0) {
    // TODO Auto-generated method stub
  }
  
  private Synthesizer synthesizer;

  public static void main(String[] args) throws Exception {
      Speaker speeker = new Speaker();
      speeker.createSynthesizer();
      speeker.say("Hello world");
  }
}
