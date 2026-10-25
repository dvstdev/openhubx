package com.darktile.openhubx.ui.widget.colorChooser;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.widget.Toast;

import java.util.Locale;

/**
 * Bespoke replacement for the color-circle view that the removed material-dialogs
 * color module used to provide. Draws a filled circle of its background color and,
 * when selected, a contrasting ring. Long-press shows the color's hex value.
 */
public class CircleView extends View {

  private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final float borderWidthPx;
  private int circleColor = Color.BLACK;
  private boolean selectedState = false;

  public CircleView(Context context) {
    this(context, null);
  }

  public CircleView(Context context, AttributeSet attrs) {
    super(context, attrs);
    borderWidthPx = context.getResources().getDisplayMetrics().density * 3f;
    fillPaint.setStyle(Paint.Style.FILL);
    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setStrokeWidth(borderWidthPx);
    setWillNotDraw(false);
  }

  @Override
  public void setBackgroundColor(int color) {
    if (this.circleColor != color) {
      this.circleColor = color;
      invalidate();
    }
  }

  @Override
  public void setSelected(boolean selected) {
    if (this.selectedState != selected) {
      this.selectedState = selected;
      invalidate();
    }
  }

  private static int shiftColor(int color, float by) {
    float[] hsv = new float[3];
    Color.colorToHSV(color, hsv);
    hsv[2] *= by;
    return Color.HSVToColor(hsv);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    final int w = getWidth();
    final int h = getHeight();
    final float cx = w / 2f;
    final float cy = h / 2f;
    final float radius = Math.min(w, h) / 2f - borderWidthPx;
    fillPaint.setColor(circleColor);
    canvas.drawCircle(cx, cy, radius, fillPaint);
    if (selectedState) {
      borderPaint.setColor(shiftColor(circleColor, 0.75f));
      canvas.drawCircle(cx, cy, radius, borderPaint);
    }
  }

  public void showHint(int color) {
    final int[] pos = new int[2];
    getLocationOnScreen(pos);
    Toast toast =
        Toast.makeText(
            getContext(),
            String.format(Locale.US, "#%06X", 0xFFFFFF & color),
            Toast.LENGTH_SHORT);
    toast.setGravity(Gravity.TOP | Gravity.START, pos[0], pos[1]);
    toast.show();
  }
}
