#!/bin/sh

lpwd=`dirname $0`
if [ "$lpwd" = "" -o "$lpwd" = "." -o "$lpwd" = "./" ];then
  lpwd=`pwd`
fi

####### constants #########
groupId=com.alx.appserver
version=1.0.0

############# methods ##################
validateResult() {
  if [ $? -ne 0 ]; then
    echo -e "error: install $1 failed!"
    exit 1;
  else
    echo -e "info: install $1 success."
  fi
}

###### install springboot dependencies #########
libDir="${lpwd}/lib"
if [ ! -d "$libDir" ]; then
  echo "Directory $libDir does not exist, please put jars into lib directory at first!"
  exit 1
fi

mvn install:install-file -Dfile="$libDir/alxas-core-$version.jar" -DgroupId=$groupId -DartifactId=alxas-core -Dversion=$version -Dpackaging=jar >> install.log
validateResult $groupId:alxas-core:$version

mvn install:install-file -Dfile="$libDir/alxas-websocket-$version.jar" -DgroupId=$groupId -DartifactId=alxas-websocket -Dversion=$version -Dpackaging=jar  >> install.log
validateResult $groupId:alxas-websocket:$version

mvn install:install-file -Dfile="$libDir/alxas-jasper-$version.jar" -DgroupId=$groupId -DartifactId=alxas-jasper -Dversion=$version -Dpackaging=jar  >> install.log
validateResult  $groupId:alxas-jasper:$version

mvn install:install-file -Dfile="$libDir/alxas-el-$version.jar" -DgroupId=$groupId -DartifactId=alxas-el -Dversion=$version -Dpackaging=jar  >> install.log
validateResult $groupId:alxas-el:$version



