

# 1.Configure Textmate
Textmate-core module uses the features of the higher JDK version. You must desugar to run on the lower version device

``` groovy

android {
    //desugar
    compileOptions {
        coreLibraryDesugaringEnabled true
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

	//Avoid conflicting files
    packagingOptions{
        pickFirst 'license/README.dom.txt'
        pickFirst 'license/LICENSE.dom-documentation.txt'
        pickFirst 'license/NOTICE'
        pickFirst 'license/LICENSE.dom-software.txt'
        pickFirst 'license/LICENSE'
    }
	
	//...
}

dependencies {
	//desugar
    coreLibraryDesugaring 'com.android.tools:desugar_jdk_libs:1.1.5'
    //...
}

```

# 2.Libraries used
To avoid unexpected errors, the version is the same as `tm4e 0.4.2`

``` groovy
    implementation "com.google.code.gson:gson:${Versions.gsonVersion}"
    implementation "org.jruby.jcodings:jcodings:${Versions.jcodingsVersion}"
    implementation "org.jruby.joni:joni:${Versions.joniVersion}"
    implementation "org.apache.xmlgraphics:batik-css:${Versions.batikCssVersion}"
    implementation "org.apache.xmlgraphics:batik-util:${Versions.batikUtilVersion}"
    implementation "xerces:xercesImpl:${Versions.xercesImplVersion}"
```

# 3.License
`tm4e` is a community open-source project licensed under the Eclipse Public License 1.0.This module is also.

