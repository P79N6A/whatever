package mmp;

import java.util.ArrayList;
import java.util.List;

public class MyPromise {

    List<MyListener> listeners = new ArrayList<>();

    boolean success;

    Object result;

    int failCount;


    public boolean setSuccess(Object result) {
        if (success) {
            return false;
        }

        success = true;
        this.result = result;

        signalListeners();
        return true;
    }


    private void signalListeners() {
        for (MyListener l : listeners) {
            l.operationComplete(this);
        }
    }


    public boolean setFail(Exception e) {
        if (failCount > 0) {
            return false;
        }
        ++failCount;
        result = e;
        signalListeners();
        return true;
    }

    public boolean isSuccess() {
        return success;
    }


    public void addListener(MyListener myListener) {
        listeners.add(myListener);
    }


    public void removeListener(MyListener myListener) {
        listeners.remove(myListener);
    }

    public Object get() {
        return result;
    }
}
