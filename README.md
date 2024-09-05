# Kotlin AFIS CLI

CLI application based on SourceAFIS for comparing fingerprint templates.

## GraalVM installation

You can use Jabba as a JVM manager to install GraalVM on my machine, or download
a more recent version from Oracle. 

```
# use Jabba to install GraalVM
$ jabba ls-remote | grep graalvm | grep 21
$ jabba install graalvm@21.1.0
```

Add to `.envrc` and run `direnv allow` to set the environment variables:
```
source ~/.jabba/jabba.sh
export JABBA_VM=graalvm@21.1.0
export JAVA_HOME=$(jabba which $JABBA_VM)/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export GRAALVM_HOME=$JAVA_HOME
```

Install the `native-image` utility using the Graal updater.
```
$ gu available
$ gu install native-image
```

## Building and using CLI

```
$ ./gradlew build
$ ./gradlew buildNativeImage

$ cd build/
$ ./hands-on-clikt
$ ./hands-on-clikt --name Leander --count 3
$ ./hands-on-clikt --help
```

## Maintainer

Caktus Consulting Group, LLC

## License

This software is provided under the MIT open source license, read the `LICENSE`
file for details.
