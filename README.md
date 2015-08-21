# VAPP
The VAPP! mobile payment SDK for Android

# Getting Started
VAPP! is provided as a standalone [aar file](http://tools.android.com/tech-docs/new-build-system/aar-format).
You should copy this file to **/MyProject/app/aars/vapp.aar**. Your build.gradle file should then be modified 
so that it matches the code snippet below:

```
apply plugin: 'com.android.application'

android {
  
  ...
  
    repositories { // make gradle search the app/aars directory for source sets
        flatDir {
            dirs 'aars'
        }
    }
}

dependencies {
    compile(name:'vapp', ext:'aar') // add VAPP! SDK as a dependency
}
```

After syncing your gradle files, your project should be ready to initialise VAPP! with a list of products.
See the [sample application](https://github.com/vasilitate/VAPP-Store) if you are having trouble with this step.

### Initialising VAPP!
Vapp.initialise() must be called before any SMS payments are made. Initialisation requires several pieces of information:

- Your app id, which identifies your payments
- The number range which you have been assigned
- A list of products within your app, which can be paid for using SMS

It is recommended that you initialise VAPP! within a custom [Application class](http://developer.android.com/reference/android/app/Application.html),
using something similar to the snippet below:

```
private static final String VAPP_APP_ID = "MyAppId"; // TODO change app id

List<VappProduct> productList; // TODO add products
VappNumberRange vappNumberRange = new VappNumberRange("+447458730000", "+447458730010"); // TODO alter number range

...

Vapp.initialise(this, VAPP_APP_ID, productList, vappNumberRange, false);
```

### Defining Products
