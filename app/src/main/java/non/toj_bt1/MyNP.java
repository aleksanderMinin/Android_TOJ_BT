package non.toj_bt1;

import android.widget.NumberPicker;
/**
 * Created by aminin on 26.07.2017.
 */

class MyNP {
    NumberPicker NP;

    MyNP() {
        NP.setMaxValue(115);
        NP.setMinValue(25);
        NP.setWrapSelectorWheel(false);
        NP.setEnabled(false);
    }

    void setValue(int val) {
        NP.setValue(val);
        NP.setEnabled(true);
    }

    void setEnabled (boolean val) {
        NP.setEnabled(val);
    }

    int getValue () {
        return NP.getValue();
    }

    boolean isEnabled() {
        return NP.isEnabled();
    }

    String getValueString () {
        String tmp = "";
        int value = NP.getValue();
        try {
            int xxx = value / 100;
            int xx = value / 10 - xxx * 10;
            int x = value % 10;
            tmp += Integer.toString(xxx);
            tmp += Integer.toString(xx);
            tmp += Integer.toString(x);
        } catch (NumberFormatException e) {
            System.err.println("MyNP.getValueString какой то пиздец!");
        }
        return tmp;
    }
}
