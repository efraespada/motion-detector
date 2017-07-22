# motion-detector

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