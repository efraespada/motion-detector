# motion-detector

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

```java
MotionDetector.initialize(getApplicationContext());
```

```java
MotionDetector.debug(true);
```

```java

MotionDetector.start(new Listener() {
    @Override
    public void locationChanged(Location location) {
    
    }
    
    /**
    * acceleration changed 
    */
    @Override
    public void accelerationChanged(float acceleration) {
    
    }
    
    /**
    * step detected
    */
    @Override
    public void step() {
        
    }
    
    /**
    * 
    */
    @Override
    public void type(String type) {
    
    }
});
```

```java
MotionDetector.end();
```
```java
@Override
protected void onResume() {
    super.onResume();
    if (MotionDetector.isServiceReady()) {
        // service is ready
    }
}
```
```java
@Override
protected void onDestroy() {
    MotionDetector.end();
    super.onDestroy();
}
```

```xml
<service
    android:name="com.efraespada.motiondetector.MotionService"
    android:enabled="true"
    android:exported="false" />
```