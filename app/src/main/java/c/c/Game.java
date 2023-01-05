package c.c;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

class Game extends View {

  private static final Random random = new Random(0);
  private static final int WIDTH = 8;
  private static final int HEIGHT = 5;
  private static final int SIZE = WIDTH * HEIGHT;

  private final Drawable[] candy;
  private final Drawable explosion;
  private final int[] candies;
  private final boolean[] exploded;
  private int cellSize;
  private int leftMargin;
  private int topMargin;
  private int startPosition;
  private Handler handler;
  private AudioTrack audioTrack;

  public Game(Context context) {
    super(context);
    handler = new Handler();
    candy = new Drawable[] {
        getResources().getDrawable(R.drawable.b, null),
        getResources().getDrawable(R.drawable.g, null),
        getResources().getDrawable(R.drawable.o, null),
        getResources().getDrawable(R.drawable.r, null),
        getResources().getDrawable(R.drawable.y, null),
        getResources().getDrawable(R.drawable.p, null)
    };
    audioTrack = new AudioTrack.Builder()
        .setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build())
        .setAudioFormat(new AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(8000)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build())
        .setBufferSizeInBytes(8000)
        .build();
    explosion = getResources().getDrawable(R.drawable.e, null);
    candies = new int[SIZE];
    exploded = new boolean[SIZE];
    fall();
    while (markExploded(candies, exploded)) {
      removeExploded();
      fall();
    }
  }

  private static boolean markExploded(int[] candies, boolean[] exploded) {
    Log.i("X", "markExploded start");
    boolean hasExploded = false;
    for (int i = 0; i < SIZE; i++) {
      int x = i % WIDTH;
      int y = i / WIDTH;
      int v = candies[i];
      if (v == 0) continue;
      if (x > 0 && x < WIDTH - 1 && candies[i-  1] == v && candies[i + 1] == v) {
        hasExploded = true;
        exploded[i-1] = true;
        exploded[i] = true;
        exploded[i+1] = true;
      }
      if (y > 0 && y < HEIGHT - 1 && candies[i - WIDTH] == v && candies[i + WIDTH] == v) {
        hasExploded = true;
        exploded[i- WIDTH] = true;
        exploded[i] = true;
        exploded[i+ WIDTH] = true;
      }
    }

    Set<Integer> res = new HashSet<>();
    for (int i = 0; i < SIZE; i++) {
      if (exploded[i]) res.add(i);
    }
    Log.i("X", "markExploded Marked: " + res.toString());

    return hasExploded;
  }

  private void removeExploded() {
    Log.i("X", "removeExploded");
    for (int i = 0; i < SIZE; i++) {
      if (exploded[i]) {
        exploded[i] = false;
        candies[i] = 0;
      }
    }
  }

  private void fall() {
    Log.i("X", "fall");
    boolean falling = true;
    while (falling) {
      falling = false;
      for (int i = SIZE - 1; i >= WIDTH; i--) {
        if (candies[i] == 0 && candies[i - WIDTH] != 0) {
          falling = true;
          candies[i] = candies[i - WIDTH];
          candies[i - WIDTH] = 0;
        }
      }
    }

    for (int i = 0; i < candies.length ; i++) {
      if (candies[i] == 0) candies[i] = random.nextInt(candy.length) + 1;
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    int width = getRight();
    int height = getBottom();
    cellSize = width /10;
    leftMargin = (width - WIDTH * cellSize) / 2;
    topMargin = (height - HEIGHT * cellSize) / 2;
    for (int i = 0; i < SIZE; i++) {
      int v = candies[i];
      if (v == 0) continue;
      int x = i % WIDTH;
      int y = i / WIDTH;
      Drawable drawable = candy[v - 1];
      drawable.setBounds(leftMargin + cellSize * x, topMargin + cellSize * y, leftMargin + cellSize * (x+1), topMargin + cellSize * (y + 1));
      drawable.draw(canvas);
      if (exploded[i]) {
        explosion.setBounds(leftMargin + cellSize * x, topMargin + cellSize * y, leftMargin + cellSize * (x+1), topMargin + cellSize * (y + 1));
        explosion.draw(canvas);
      }
    }
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int x = (((int) event.getX()) - leftMargin) / cellSize;
    int y = (((int) event.getY()) - topMargin) / cellSize;
    int touchPosition = x + y * WIDTH;
    switch(event.getAction()) {
      case MotionEvent.ACTION_DOWN:
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
          startPosition = touchPosition;
          return true;
        }
        return false;
      case MotionEvent.ACTION_MOVE:
        if (startPosition >= 0 && x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
          if (isValid(startPosition, touchPosition)) {
            swap(candies, startPosition, touchPosition);
            invalidate();
            Log.i("X", "onTouchEvent: posting call to explode");
            handler.postDelayed(this::explode, 100);
            // Make all subsequent events invalid.
            startPosition = -1;
          }
        }
    }
    return true;
  }

  private boolean isValid(int position1, int position2) {
    int distance = Math.abs(position1 - position2);
    if (distance != 1 && distance != WIDTH) return false;
    int[] newCandies = new int[SIZE];
    boolean[] newExploded = new boolean[SIZE];
    System.arraycopy(candies, 0, newCandies, 0, SIZE);
    swap(newCandies, position1, position2);
    return markExploded(newCandies, newExploded);
  }

  private void explode() {
    Log.i("X", "explode");
    if (markExploded(candies, exploded)) {
      invalidate();
      play();
      handler.postDelayed(() -> { removeExploded();invalidate(); }, 500);
      handler.postDelayed(() -> { fall();invalidate(); }, 1000);
      handler.postDelayed(this::explode, 1500);
    }
  }

  private static void swap(int[] array, int index1, int index2) {
    int tmp = array[index1];
    array[index1] = array[index2];
    array[index2] = tmp;
  }

  private void play() {
    short[] buf = new short[4000];
    for (int i = 0 ; i < 4000 ; i++) {
      buf[i] = (short) (Math.sin(440 * i * 6.28 / 8000.0) * 20000);
    }
    audioTrack.write(buf, 0, 4000);
    audioTrack.play();
  }
}
