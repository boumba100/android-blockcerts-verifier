# android-blockcerts-verifier

Android library for verifying blockchain certificates according to the blockCerts open standard(http://www.blockcerts.org/).


# Include the project into your application

In Android Studio 

Click ``` File → New → Import Module``` and select the blockcertVerifier module.

The step above will add ```':blockcertVerifier'```  to the settings.gradle file as shown below.

```gradle
include ':app', ':blockcertVerifier'
```
To compile the library in your application simply add this line to the project's build.gradle file.

```gradle
dependencies {
	...
compile project(':blockcertVerifier')
	...
}
```

# Example code

```
final Verifier verifier = new Verifier() {
            @Override
            public void onResult(boolean result, String message) {
                Log.d("certificate valid", String.valueOf(result));
                Log.d("message", message);
            }
        };
        // If you want to use the blockchain test network
        verifier.setTestNet();
        
        verifier.verify(certificateJsonObject)
        ```
