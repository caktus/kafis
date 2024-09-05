# Kotlin AFIS CLI

CLI application based on SourceAFIS for comparing fingerprint templates.

## GraalVM installation

### 1a. With binary support
To build a native CLI binary on MacOS, you will need GraalVM >= 22 from Oracle.
Follow GraalVM [Installation on macOS Platforms](https://www.graalvm.org/latest/docs/getting-started/macos/)
instructions to install it. For example:

```shell
wget https://download.oracle.com/graalvm/22/latest/graalvm-jdk-22_macos-aarch64_bin.tar.gz
tar -xzf graalvm-jdk-22_macos-aarch64_bin.tar.gz
# Note, you may download a newer minor version; alter the commands below accordingly
sudo mv graalvm-jdk-22.0.2+9.1 /Library/Java/JavaVirtualMachines
rm graalvm-jdk-22_macos-aarch64_bin.tar.gz
```

Add to `.envrc` and run `direnv allow` to set the environment variables:
```shell
export JAVA_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-22.0.2+9.1/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export GRAALVM_HOME=$JAVA_HOME
```

### 1b. Without binary support (via `jabba`)
If you don't need to compile a native binary, you can use Jabba as a JVM manager to install GraalVM:

```shell
# use Jabba to install GraalVM
$ jabba ls-remote | grep graalvm | grep 21
$ jabba install graalvm-ce-java16@21.1.0
```

Add to `.envrc` and run `direnv allow` to set the environment variables:
```shell
source ~/.jabba/jabba.sh
export JABBA_VM=graalvm-ce-java16@21.1.0
export JAVA_HOME=$(jabba which $JABBA_VM)/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export GRAALVM_HOME=$JAVA_HOME
```

#### Install the `native-image` command
Install the `native-image` utility using the Graal updater.
```shell
$ gu available
$ gu install native-image
```

## 2. Building and using CLI

```shell
$ ./gradlew build
$ ./gradlew buildNativeImage

$ cd build/
$ ./kafis
$ ./kafis --name Leander --count 3
$ ./kafis --help
```

## Maintainer

Caktus Consulting Group, LLC

Thanks to [@lreimer](https://github.com/lreimer) for the [hands-on-clikt](https://github.com/lreimer/hands-on-clikt) template used to start this repository.

## License

This software is provided under the MIT open source license, read the `LICENSE`
file for details.
