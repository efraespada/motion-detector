# motion-detector

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

```java
MotionDetector.initialize(getApplicationContext());
MotionDetector.debug(true);

MotionDetector.start(new Listener() {
    @Override
    public void locationChanged(Location location) {
    
    }
    
    @Override
    public void accelerationChanged(float acceleration) {
    
    }
    
    @Override
    public void step() {
        
    }
    
    @Override
    public void type(String type) {
    
    }
});
```