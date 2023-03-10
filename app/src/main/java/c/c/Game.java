package c.c;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;

import java.util.Random;

class Game extends View {

  private static final Random random = new Random();
  private static final int WIDTH = 8;
  private static final int HEIGHT = 5;
  private static final int SIZE = WIDTH * HEIGHT;

  private final Drawable[] items;
  private final Drawable explosion;
  private final int[] candies;
  private final boolean[] exploded;
  private int cellSize;
  private int leftMargin;
  private int topMargin;
  private int startPosition;
  private int score;
  private final Handler handler;
  private final AudioTrack audioTrack;
  private final Paint textPaint;
  private boolean fallToCome;

  public Game(Context context) {
    super(context);
    handler = new Handler();
    Resources resources = getResources();
    items = new Drawable[] {
        resources.getDrawable(R.drawable.b, null),
        resources.getDrawable(R.drawable.g, null),
        resources.getDrawable(R.drawable.o, null),
        resources.getDrawable(R.drawable.r, null),
        resources.getDrawable(R.drawable.y, null),
        resources.getDrawable(R.drawable.p, null)
    };

    audioTrack = new AudioTrack(
            new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(8000)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            8000,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
    );

    explosion = resources.getDrawable(R.drawable.e, null);
    candies = new int[SIZE];
    exploded = new boolean[SIZE];
    textPaint = new Paint();
    textPaint.setColor(Color.WHITE);
    textPaint.setTextSize(100);
    fall();
    // Clear any match-3 before the game starts.
    while (markExploded(candies, exploded) > 0) {
      removeExploded();
      fall();
    }
    score = 0;
  }

  private static int markExploded(int[] candies, boolean[] exploded) {
    int explosions = 0;
    for (int i = 0; i < SIZE; i++) {
      int x = i % WIDTH;
      int y = i / WIDTH;
      int v = candies[i];
      if (v == 0) continue;
      if (x > 0 && x < WIDTH - 1 && candies[i-  1] == v && candies[i + 1] == v) {
        explosions++;
        exploded[i - 1] = true;
        exploded[i] = true;
        exploded[i + 1] = true;
      }
      if (y > 0 && y < HEIGHT - 1 && candies[i - WIDTH] == v && candies[i + WIDTH] == v) {
        explosions++;
        exploded[i - WIDTH] = true;
        exploded[i] = true;
        exploded[i + WIDTH] = true;
      }
    }
    return explosions;
  }

  private void removeExploded() {
    for (int i = 0; i < SIZE; i++) {
      if (exploded[i]) {
        exploded[i] = false;
        candies[i] = 0;
      }
    }
  }

  private void fall() {
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
      fallToCome = false;
    }

    for (int i = 0; i < candies.length ; i++) {
      if (candies[i] == 0) candies[i] = random.nextInt(items.length) + 1;
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
      Drawable drawable = items[v - 1];
      drawable.setBounds(leftMargin + cellSize * x, topMargin + cellSize * y, leftMargin + cellSize * (x+1), topMargin + cellSize * (y + 1));
      drawable.draw(canvas);
      if (exploded[i]) {
        explosion.setBounds(leftMargin + cellSize * x, topMargin + cellSize * y, leftMargin + cellSize * (x+1), topMargin + cellSize * (y + 1));
        explosion.draw(canvas);
      }
    }
    String scoreString = String.valueOf(score);
    // Each digit of the score ir roughly 50 pixel wide.
    canvas.drawText(scoreString, (width - scoreString.length() * 50) / 2, topMargin / 2, textPaint);
    // It's ok not to have any valid move while there are still explosions.
    if (!fallToCome && !isValid()) {
      canvas.drawText("Game Over!", (width - 500) / 2, height - topMargin / 2, textPaint);
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
    return markExploded(newCandies, newExploded) > 0;
  }

  private boolean isValid() {
    for (int i = 0; i < SIZE; i++) {
      int x = i % WIDTH;
      int y = i / WIDTH;
      if (x < WIDTH - 1 && isValid(i, i + 1)) return true;
      if (y < HEIGHT - 1 && isValid(i, i + WIDTH)) return true;
    }
    return false;
  }

  private void explode() {
    int explosions = markExploded(candies, exploded);
    if (explosions > 0) {
      score += explosions;
      invalidate();
      playBeep();
      fallToCome = true;
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

  private void playBeep() {
    if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
      audioTrack.stop();
    } else {
      short[] buf = new short[4000];
      for (int i = 0; i < 4000; i++) {
        int amplitude = 20000;
        if (i < 1000) amplitude = amplitude * i / 1000;
        if (i > 3000) amplitude = amplitude * (4000 - i) / 1000;
        buf[i] = (short) (Math.sin(440 * i * 6.28 / 8000.0) * amplitude);
      }
      audioTrack.write(buf, 0, 4000);
    }
    audioTrack.play();
  }
}
