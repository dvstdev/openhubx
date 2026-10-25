package com.darktile.openhubx.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.appcompat.widget.AppCompatButton;
import android.util.AttributeSet;
import android.util.TypedValue;

import com.darktile.openhubx.R;

/**
 * Lightweight replacement for the unmaintained com.unstoppable:submitbutton (no AndroidX build).
 * A standard button honoring buttonColor / buttonText / buttonTextSize. doResult()/reset() are
 * retained as no-ops so existing call sites keep compiling; the original submit animation is dropped.
 */
public class SubmitButton extends AppCompatButton {

    public SubmitButton(Context context) {
        this(context, null);
    }

    public SubmitButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SubmitButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setAllCaps(false);
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SubmitButton);
            CharSequence text = a.getText(R.styleable.SubmitButton_buttonText);
            if (text != null) {
                setText(text);
            }
            int color = a.getColor(R.styleable.SubmitButton_buttonColor, 0);
            if (color != 0) {
                Drawable bg = getBackground();
                if (bg != null) {
                    bg.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
                }
                setTextColor(0xFFFFFFFF);
            }
            float size = a.getDimension(R.styleable.SubmitButton_buttonTextSize, 0f);
            if (size > 0f) {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
            }
            a.recycle();
        }
    }

    /** No-op: the original success/failure submit animation is not reproduced. */
    public void doResult(boolean success) {
    }

    /** No-op: kept for call-site compatibility. */
    public void reset() {
    }
}
