package hk.multitude.owcremote;

/**
 * Created by jason on 14/3/15.
 */
public abstract class DeviceConnection {
    private OnReadListener mListener = null;

    public abstract void write(String message);

    public abstract void flush();

    public abstract boolean isConnected();

    public void setOnReadListener(OnReadListener listener) {
        mListener = listener;
    }

    public OnReadListener getOnReadListener() {
        return mListener;
    }

    public static interface OnReadListener {
        void onRead(String message);
    }
}
