package hk.multitude.owcremote;

/**
 * Created by jason on 14/3/15.
 */
public class ControlState {
    public int joystickX;
    public int joystickY;
    public int[] buttons;
    
    public ControlState() {
        joystickX = joystickY = 0;
        buttons = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1};
    }
    public ControlState(ControlState state) {
        joystickX = state.joystickX;
        joystickY = state.joystickY;
        buttons = new int[9];
        System.arraycopy(state.buttons, 0, buttons, 0, buttons.length);
    }
    public ControlState(int x, int y, int[] states) {
        joystickX = x;
        joystickY = y;
        buttons = states;
    }
}
