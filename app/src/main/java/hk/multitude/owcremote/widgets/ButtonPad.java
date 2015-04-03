package hk.multitude.owcremote.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by jason on 14/3/15.
 */
public class ButtonPad extends View {
    private static final int PAD_SIZE = 3;
    private static final int MARGIN_DP = 5;
    private float mPadLength;
    private float mButtonLength;
    private float mMargin;

    private Context mContext;

    private Paint mButtonPaint;
    private Paint mSelectorPaint;

    private int[] mStates = new int[PAD_SIZE*PAD_SIZE];

    private ButtonStateListener mListener;

    //private ArrayList<Pair<Float, Float>> mPointerLocations = new ArrayList();

    public ButtonPad(Context context) {
        super(context); init(context);
    }
    public ButtonPad(Context context, AttributeSet attrs) {
        super(context, attrs); init(context);
    }
    public ButtonPad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init(context);
    }
    @TargetApi(21)
    public ButtonPad(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes); init(context);
    }

    void init(Context context) {
        mContext = context;

        mButtonPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mButtonPaint.setColor(Color.LTGRAY);

        mSelectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mSelectorPaint.setColor(Color.GRAY);

        mMargin = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MARGIN_DP,
                getResources().getDisplayMetrics()));

        for (int i = 0; i < mStates.length; i++) mStates[i] = 1;
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPadLength = w;
        mButtonLength = (mPadLength-mMargin*(PAD_SIZE-1))/PAD_SIZE;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int width = 100000, height = 100000;
        if (widthMode != MeasureSpec.UNSPECIFIED) width = widthSize;
        if (heightMode != MeasureSpec.UNSPECIFIED) height = heightSize;
        int length = Math.min(width, height);
        if (length == 100000) length = 300;
        //Log.d("onMeasure", "Length as: "+length);
        setMeasuredDimension(length, length);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float len = (mMargin+mButtonLength);
        for (int i = 0; i < PAD_SIZE; i++) {
            for (int j = 0; j < PAD_SIZE; j++) {
                if (mStates[i*PAD_SIZE+j] == 1)
                    canvas.drawRect(len*j, len*i, len*j+mButtonLength, len*i+mButtonLength, mButtonPaint);
                else
                    canvas.drawRect(len*j, len*i, len*j+mButtonLength, len*i+mButtonLength, mSelectorPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Log.d("Control Pad", "motion: "+event.getActionIndex()+" x: "+event.getX()+" y: "+event.getY());
        int action = MotionEventCompat.getActionMasked(event);
        int[] newStates = new int[PAD_SIZE*PAD_SIZE];
        for (int i = 0; i < newStates.length; i++) newStates[i] = 1;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_POINTER_UP:
                int size = MotionEventCompat.getPointerCount(event);
                for (int i = 0; i < size; i++) {
                    float x = MotionEventCompat.getX(event, i);
                    float y = MotionEventCompat.getY(event, i);
                    if (x < 0 || y < 0 || x > mPadLength || y > mPadLength)
                        continue;
                    if (x % (mButtonLength + mMargin) > mButtonLength)
                        continue;
                    if (y % (mButtonLength + mMargin) > mButtonLength)
                        continue;
                    if (action == MotionEvent.ACTION_POINTER_UP &&
                            i == MotionEventCompat.getActionIndex(event))
                        continue;
                    int xx = (int) (x / (mButtonLength + mMargin));
                    int yy = (int) (y / (mButtonLength + mMargin));
                    newStates[yy*PAD_SIZE+xx] = 0;
                }
                /*mPointerLocations.ensureCapacity(size);
                while (mPointerLocations.size() > size)
                    mPointerLocations.remove(mPointerLocations.size()-1);
                for (int i = 0; i < size; i++) {
                    if ( i >= mPointerLocations.size())
                        mPointerLocations.add(i, new Pair<>(MotionEventCompat.getX(event, i),
                                                            MotionEventCompat.getY(event, i)));
                    else
                        mPointerLocations.set(i, new Pair<>(MotionEventCompat.getX(event, i),
                                MotionEventCompat.getY(event, i)));
                }*/
                break;
            case MotionEvent.ACTION_UP:
                break;
            default:
                return true;
        }
        /*for (Pair<Float, Float> i : mPointerLocations) {
            if (i.first < 0 || i.second < 0 || i.first > mPadLength || i.second > mPadLength)
                continue;
            if (i.first%(mButtonLength+mMargin) > mButtonLength)
                continue;
            if (i.second%(mButtonLength+mMargin) > mButtonLength)
                continue;
            int x = (int) (i.first/(mButtonLength+mMargin));
            int y = (int) (i.second/(mButtonLength+mMargin));
            newStates[y*PAD_SIZE+x] = 0;
        }*/
        boolean changed = false;
        for (int i = 0; i < newStates.length; i++) {
            if (mStates[i] != newStates[i]) {
                changed = true;
                //Log.d("ControlPad", "Button "+i+" changed to "+newStates[i]);
                if (mListener != null) {
                    mListener.onButtonStateChanged(i%PAD_SIZE, i/PAD_SIZE, newStates[i]);
                }
            }
        }
        if (changed) {
            mStates = newStates;
            invalidate();
        }
        return true;
    }
    
    public int getState(int x, int y) {
        return mStates[y*PAD_SIZE+x];
    }

    public void setButtonStateListener(ButtonStateListener listener) {
        mListener = listener;
    }

    public ButtonStateListener getButtonStateListener() {
        return mListener;
    }

    public static interface ButtonStateListener {
        public void onButtonStateChanged(int x, int y, int state);
    }
}
