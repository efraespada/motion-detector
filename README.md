# motion-detector

Gradle:
```groovy
implementation 'com.efraespada:motiondetector:0.0.2'
```
Manifest:
```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 
<application>
 
    <service
        android:name="com.efraespada.motiondetector.MotionService"
        android:enabled="true"
        android:exported="false" />
        
</application>
```
Initialize:
```java
MotionDetector.initialize(getApplicationContext());
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
Kill service:
```java
MotionDetector.end();
```
Check if service is available:
```java
@Override
protected void onResume() {
    super.onResume();
    if (MotionDetector.isServiceReady()) {
        // service is ready
    }
}
 
@Override
protected void onDestroy() {
    MotionDetector.end();
    super.onDestroy();
}
```