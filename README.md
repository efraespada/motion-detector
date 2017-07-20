# motion-detector

```java
MotionDetector.initialize(getApplicationContext());
MotionDetector.debug(true);

MotionDetector.start(new Listener() {
    @Override
    public void locationChanged(Location location) {
    
    }
    
    @Override
    public void step() {
        
    }
    
    @Override
    public void car() {

    }
});
```