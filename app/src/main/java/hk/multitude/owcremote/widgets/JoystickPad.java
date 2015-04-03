package hk.multitude.owcremote.widgets;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by jason on 14/3/15.
 */
public class JoystickPad extends View {
    private static final int MARGIN_DP = 10;
    private static final float SELECTOR_RADIUS_RATIO = 0.3f;
    private float mPadRadius;
    private float mPointerRadius;
    private float mMargin;
    private float mCentre;

    private Context mContext;

    private Paint mCirclePaint, mCircumPaint, mPointerPaint, mAxisPaint;
    private Path mCircleClip;
    
    // These are relative to the centre
    private boolean mPointerVisible = false;
    private float mPointerX = 0;
    private float mPointerY = 0;

    private JoystickListener mListener;

    public JoystickPad(Context context) {
        super(context); init(context);
    }
    public JoystickPad(Context context, AttributeSet attrs) {
        super(context, attrs); init(context);
    }
    public JoystickPad(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr); init(context);
    }
    @TargetApi(21)
    public JoystickPad(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes); init(context);
    }

    void init(Context context) {
        mContext = context;

        mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCirclePaint.setColor(Color.LTGRAY);

        mCircumPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCircumPaint.setColor(Color.LTGRAY);
        mCircumPaint.setStrokeWidth(2);
        mCircumPaint.setStyle(Paint.Style.STROKE);

        mPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointerPaint.setColor(Color.GRAY);

        mAxisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mAxisPaint.setColor(Color.BLACK);

        mMargin = Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MARGIN_DP,
                getResources().getDisplayMetrics()));
    }

    @Override
    public void setBackground(Drawable background) {
        super.setBackground(background);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mPadRadius = (w-2*mMargin)/2;
        mPointerRadius = (float) SELECTOR_RADIUS_RATIO*mPadRadius;
        mCentre = mMargin+mPadRadius;
        mCircleClip = new Path();
        mCircleClip.addCircle(mCentre, mCentre, mPadRadius, Path.Direction.CW);
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
        canvas.drawCircle(mCentre, mCentre, mPadRadius, mCirclePaint);

        canvas.save();
        canvas.clipPath(mCircleClip, Region.Op.REPLACE);
        canvas.drawCircle(mCentre+mPointerX, mCentre+mPointerY, mPointerRadius, mPointerPaint);
        canvas.restore();
        // Workaround for antialiasing
        canvas.drawCircle(mCentre, mCentre, mPadRadius, mCircumPaint);

        canvas.drawLine(mMargin, mCentre, mCentre+mPadRadius, mCentre, mAxisPaint);
        canvas.drawLine(mCentre, mMargin, mCentre, mCentre+mPadRadius, mAxisPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //Log.d("JoystickPad", "motion: "+event.getAction()+" x: "+event.getX()+" y: "+event.getY());
        int action = MotionEventCompat.getActionMasked(event);
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mPointerVisible = true;
            case MotionEvent.ACTION_MOVE:
                float x = MotionEventCompat.getX(event, 0)-mCentre;
                float y = MotionEventCompat.getY(event, 0)-mCentre;
                double len = Math.sqrt(x*x+y*y);
                if (len > mPadRadius) {
                    len = mPadRadius;
                    double angle = Math.atan2(y, x);
                    x = (float) (len*Math.cos(angle));
                    y = (float) (len*Math.sin(angle));
                }
                mPointerX = x; mPointerY = y;
                break;
            case MotionEvent.ACTION_UP:
                mPointerVisible = false;
                mPointerX = 0; mPointerY = 0;
                break;
            default:
                return true;
        }
        invalidate();
        if (mListener != null)
            mListener.onJoystickMoved(Math.round(mPointerX*127/mPadRadius),
                                      Math.round(mPointerY*127/mPadRadius));
        return true;
    }

    public int getPointerX() {
        return Math.round(mPointerX*127/mPadRadius);
    }

    public int getPointerY() {
        return Math.round(mPointerY*127/mPadRadius);
    }

    public void setJoystickListener(JoystickListener listener) {
        mListener = listener;
    }

    public JoystickListener getJoystickListener() {
        return mListener;
    }

    public static interface JoystickListener {
        public void onJoystickMoved(int x, int y); // Reports range from -127 to 127
    }
}
