package hk.multitude.owcremote;

/**
 * Created by jason on 14/3/15.
 */
public class ControllerThread extends Thread implements DeviceConnection.OnReadListener {
    private static final int[] BUTTON_MAP = {4, 3, 2, 0, 9, 8, 7, 6, 5};
    private final DeviceConnection mConn;

    private ControlState mPrev = new ControlState();
    private ControlState mCur = new ControlState();
    private ControlState mNext = null;
    private int mTick = 30;

    public ControllerThread(DeviceConnection conn) {
        mConn = conn;
        mConn.setOnReadListener(this);
    }

    @Override
    public void run() {
        while (!mConn.isConnected()) {
            try {
                sleep(300);
            } catch (InterruptedException e) {
                return;
            }
        }
        while (true) {
            if (interrupted() || !mConn.isConnected())
                break;
            mPrev = mCur;
            popState();
            for (int i = 0; i < mCur.buttons.length; i++) {
                if (mCur.buttons[i] != mPrev.buttons[i]) {
                    mConn.write("io"+BUTTON_MAP[i]+":"+mCur.buttons[i]+";");
                    mConn.flush();
                    if (!sleepTick()) return;
                }
            }
            mConn.write("x:"+(-mCur.joystickY+128)+";y:"+(mCur.joystickX+128)+";");
            mConn.flush();
            if (!sleepTick()) return;
        }
    }

    public void setTick(int tick) {
        mTick = tick;
    }

    private boolean sleepTick() {
        try {
            sleep(mTick);
        } catch (InterruptedException e) {
            return false;
        }
        return true;
    }

    @Override
    public void onRead(String message) {
        // Do nothing
    }

    public synchronized void pushState(ControlState state) {
        mNext = state;
    }

    private synchronized void popState() {
        if (mNext != null) {
            mCur = new ControlState(mNext);
        }
    }
}
