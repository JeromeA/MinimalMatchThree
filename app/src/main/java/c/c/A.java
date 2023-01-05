package c.c;

import android.app.Activity;
import android.os.Bundle;

public class A extends Activity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(new Game(this));
  }
}