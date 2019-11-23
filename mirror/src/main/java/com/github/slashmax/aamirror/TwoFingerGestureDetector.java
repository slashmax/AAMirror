package com.github.slashmax.aamirror;

import android.content.Context;
import android.util.ArrayMap;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

class TwoFingerGestureDetector
{
    private static final String TAG = "TwoFingerGestureDetector";

    public interface OnTwoFingerGestureListener {
        void onTwoFingerTapUp();
    }

    private final OnTwoFingerGestureListener                m_Listener;
    private ArrayMap<Integer, MotionEvent.PointerCoords>    m_EventMap;
    private int                                             m_TouchSlopSquare;

    TwoFingerGestureDetector(Context context, OnTwoFingerGestureListener listener)
    {
        m_Listener = listener;
        m_EventMap = new ArrayMap<>();
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        int touchSlop = configuration.getScaledTouchSlop();
        m_TouchSlopSquare = touchSlop * touchSlop;
    }

    boolean onTouchEvent(MotionEvent event)
    {
        int action = event.getActionMasked();
        for (int i = 0; i < event.getPointerCount(); i++)
        {
            int id = event.getPointerId(i);

            switch (action)
            {
                case ACTION_DOWN:
                case ACTION_POINTER_DOWN:
                    {
                        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
                        event.getPointerCoords(i, coords);
                        m_EventMap.put(id, coords);
                    }
                    break;
                case ACTION_MOVE:
                    if (m_EventMap.containsKey(id))
                    {
                        MotionEvent.PointerCoords coords = new MotionEvent.PointerCoords();
                        event.getPointerCoords(i, coords);
                        MotionEvent.PointerCoords start = m_EventMap.get(id);
                        if (start != null)
                        {
                            double dist = ComputeSquaredDistance(start, coords);
                            if (dist > m_TouchSlopSquare)
                                m_EventMap.remove(id);
                        }
                    }
                    break;
                case ACTION_UP:
                    if (m_EventMap.size() == 2)
                    {
                        if (m_Listener != null)
                            m_Listener.onTwoFingerTapUp();
                    }
                    m_EventMap.clear();
                    break;
                case ACTION_POINTER_UP:
                    break;
                case ACTION_CANCEL:
                    m_EventMap.remove(id);
                    break;
            }
        }
        return false;
    }

    private double ComputeSquaredDistance(MotionEvent.PointerCoords p1, MotionEvent.PointerCoords p2)
    {
        double dx = (p1.x - p2.x);
        double dy = (p1.y - p2.y);
        return (dx * dx + dy * dy);
    }
}
