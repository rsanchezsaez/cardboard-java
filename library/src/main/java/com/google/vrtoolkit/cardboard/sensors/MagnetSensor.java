package com.google.vrtoolkit.cardboard.sensors;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

public class MagnetSensor {
    private static final String HTC_ONE_MODEL = "HTC One";
    private TriggerDetector mDetector;
    private Thread mDetectorThread;
    
    public MagnetSensor(final Context context) {
        super();
        if (HTC_ONE_MODEL.equals(Build.MODEL)) {
            mDetector = new VectorTriggerDetector(context);
        } else {
            mDetector = new ThresholdTriggerDetector(context);
        }
    }
    
    public void start() {
        mDetectorThread = new Thread(mDetector);
        mDetectorThread.start();
    }
    
    public void stop() {
        if (mDetectorThread != null) {
            mDetectorThread.interrupt();
            mDetector.stop();
        }
    }
    
    public void setOnCardboardTriggerListener(final OnCardboardTriggerListener listener) {
        mDetector.setOnCardboardTriggerListener(listener, new Handler());
    }
    
    private abstract static class TriggerDetector implements Runnable, SensorEventListener {
        protected static final String TAG = "TriggerDetector";
        protected SensorManager mSensorManager;
        protected Sensor mMagnetometer;
        protected WeakReference<OnCardboardTriggerListener> mListenerRef;
        protected Handler mHandler;
        
        public TriggerDetector(final Context context) {
            super();
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        }
        
        public synchronized void setOnCardboardTriggerListener(
                final OnCardboardTriggerListener listener, final Handler handler) {
            mListenerRef = new WeakReference<>(listener);
            mHandler = handler;
        }
        
        protected void handleButtonPressed() {
            synchronized (this) {
                final OnCardboardTriggerListener listener = mListenerRef != null ?
                        mListenerRef.get() : null;
                if (listener != null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            listener.onCardboardTrigger();
                        }
                    });
                }
            }
        }
        
        @Override
        public void run() {
            Looper.prepare();
            mSensorManager.registerListener(this, this.mMagnetometer, 0);
            Looper.loop();
        }
        
        public void stop() {
            mSensorManager.unregisterListener(this);
        }
        
        public void onSensorChanged(final SensorEvent event) {
        }
        
        public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        }
    }
    
    private static class ThresholdTriggerDetector extends TriggerDetector {
        private static final String TAG = "ThresholdTriggerDetector";
        private static final long NS_SEGMENT_SIZE = 200000000L;
        private static final long NS_WINDOW_SIZE = 400000000L;
        private static final long NS_WAIT_TIME = 350000000L;
        private long mLastFiring;
        private static int mT1;
        private static int mT2;
        private ArrayList<float[]> mSensorData;
        private ArrayList<Long> mSensorTimes;
        
        public ThresholdTriggerDetector(final Context context) {
            super(context);
            mLastFiring = 0L;
            mSensorData = new ArrayList<>();
            mSensorTimes = new ArrayList<>();
        }
        
        public ThresholdTriggerDetector(final Context context, final int t1, final int t2) {
            super(context);
            mLastFiring = 0L;
            mSensorData = new ArrayList<>();
            mSensorTimes = new ArrayList<>();
            ThresholdTriggerDetector.mT1 = t1;
            ThresholdTriggerDetector.mT2 = t2;
        }
        
        private void addData(final float[] values, final long time) {
            mSensorData.add(values);
            mSensorTimes.add(time);
            while (mSensorTimes.get(0) < time - NS_WINDOW_SIZE) {
                mSensorData.remove(0);
                mSensorTimes.remove(0);
            }
            evaluateModel(time);
        }
        
        private void evaluateModel(final long time) {
            if (time - mLastFiring < NS_WAIT_TIME || mSensorData.size() < 2) {
                return;
            }
            final float[] baseline = mSensorData.get(mSensorData.size() - 1);
            int startSecondSegment = 0;
            for (int i = 0; i < this.mSensorTimes.size(); ++i) {
                if (time - mSensorTimes.get(i) < NS_SEGMENT_SIZE) {
                    startSecondSegment = i;
                    break;
                }
            }
            final float[] offsets = new float[mSensorData.size()];
            computeOffsets(offsets, baseline);
            final float min1 = computeMinimum(Arrays.copyOfRange(offsets, 0, startSecondSegment));
            final float max2 = computeMaximum(Arrays.copyOfRange(
                    offsets, startSecondSegment, mSensorData.size()));
            if (min1 < ThresholdTriggerDetector.mT1 && max2 > ThresholdTriggerDetector.mT2) {
                mLastFiring = time;
                handleButtonPressed();
            }
        }
        
        private void computeOffsets(final float[] offsets, final float[] baseline) {
            for (int i = 0; i < mSensorData.size(); ++i) {
                final float[] point = mSensorData.get(i);
                final float[] o = {
                        point[0] - baseline[0],
                        point[1] - baseline[1],
                        point[2] - baseline[2]
                };
                final float magnitude = (float) Math.sqrt(o[0] * o[0] + o[1] * o[1] + o[2] * o[2]);
                offsets[i] = magnitude;
            }
        }
        
        private float computeMaximum(final float[] offsets) {
            float max = Float.NEGATIVE_INFINITY;
            for (final float o : offsets) {
                max = Math.max(o, max);
            }
            return max;
        }
        
        private float computeMinimum(final float[] offsets) {
            float min = Float.POSITIVE_INFINITY;
            for (final float o : offsets) {
                min = Math.min(o, min);
            }
            return min;
        }
        
        @Override
        public void onSensorChanged(final SensorEvent event) {
            if (event.sensor.equals(this.mMagnetometer)) {
                final float[] values = event.values;
                if (values[0] == 0.0f && values[1] == 0.0f && values[2] == 0.0f) {
                    return;
                }
                addData(event.values.clone(), event.timestamp);
            }
        }
        
        @Override
        public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        }
        
        static {
            ThresholdTriggerDetector.mT1 = 30;
            ThresholdTriggerDetector.mT2 = 130;
        }
    }
    
    private static class VectorTriggerDetector extends TriggerDetector {
        private static final String TAG = "ThresholdTriggerDetector";
        private static final long NS_REFRESH_TIME = 350000000L;
        private static final long NS_THROWAWAY_SIZE = 500000000L;
        private static final long NS_WAIT_SIZE = 100000000L;
        private long mLastFiring;
        private static int mXThreshold;
        private static int mYThreshold;
        private static int mZThreshold;
        private ArrayList<float[]> mSensorData;
        private ArrayList<Long> mSensorTimes;
        
        public VectorTriggerDetector(final Context context) {
            super(context);
            mLastFiring = 0L;
            mSensorData = new ArrayList<>();
            mSensorTimes = new ArrayList<>();
            VectorTriggerDetector.mXThreshold = -3;
            VectorTriggerDetector.mYThreshold = 15;
            VectorTriggerDetector.mZThreshold = 6;
        }
        
        public VectorTriggerDetector(final Context context, final int xThreshold,
                                     final int yThreshold, final int zThreshold) {
            super(context);
            mLastFiring = 0L;
            mSensorData = new ArrayList<>();
            mSensorTimes = new ArrayList<>();
            VectorTriggerDetector.mXThreshold = xThreshold;
            VectorTriggerDetector.mYThreshold = yThreshold;
            VectorTriggerDetector.mZThreshold = zThreshold;
        }
        
        private void addData(final float[] values, final long time) {
            mSensorData.add(values);
            mSensorTimes.add(time);
            while (mSensorTimes.get(0) < time - NS_THROWAWAY_SIZE) {
                mSensorData.remove(0);
                mSensorTimes.remove(0);
            }
            evaluateModel(time);
        }
        
        private void evaluateModel(final long time) {
            if (time - mLastFiring < NS_REFRESH_TIME || mSensorData.size() < 2) {
                return;
            }
            int baseIndex = 0;
            for (int i = 1; i < mSensorTimes.size(); ++i) {
                if (time - mSensorTimes.get(i) < NS_WAIT_SIZE) {
                    baseIndex = i;
                    break;
                }
            }
            final float[] oldValues = mSensorData.get(baseIndex);
            final float[] currentValues = mSensorData.get(mSensorData.size() - 1);
            if (currentValues[0] - oldValues[0] < VectorTriggerDetector.mXThreshold
                    && currentValues[1] - oldValues[1] > VectorTriggerDetector.mYThreshold
                    &&  currentValues[2] - oldValues[2] > VectorTriggerDetector.mZThreshold) {
                mLastFiring = time;
                handleButtonPressed();
            }
        }
        
        @Override
        public void onSensorChanged(final SensorEvent event) {
            if (event.sensor.equals(mMagnetometer)) {
                final float[] values = event.values;
                if (values[0] == 0.0f && values[1] == 0.0f && values[2] == 0.0f) {
                    return;
                }
                addData(event.values.clone(), event.timestamp);
            }
        }
        
        @Override
        public void onAccuracyChanged(final Sensor sensor, final int accuracy) {
        }
    }
    
    public interface OnCardboardTriggerListener {
        void onCardboardTrigger();
    }
}
