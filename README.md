# motion-detector

Gradle:
```groovy
implementation 'com.efraespada:motiondetector:0.0.4'
```
Manifest:
```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
 
<application>
 
    <service
        android:name="com.efraespada.motiondetector.MotionJob"
        android:permission="android.permission.BIND_JOB_SERVICE"
        android:exported="true"/>
        
</application>
```
Initialize:
```java
MotionDetector.initialize(getApplicationContext());

// debug logs
MotionDetector.debug(true);

// 10 meters accuracy
MotionDetector.minAccuracy(10);
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
    public void locatedStep() {

    }

    /**
    * step detected
    */
    @Override
    public void notLocatedStep() {

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
License
-------
    Copyright 2018 Efraín Espada

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.