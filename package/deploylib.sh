#!/bin/sh

lpwd=`dirname $0`
if [ "$lpwd" = "" -o "$lpwd" = "." -o "$lpwd" = "./" ];then
  lpwd=`pwd`
fi

####### constants #########
groupId=com.alx.appserver
version=1.0.0
# you can specify repositoryId and url here,
# or you can specify value in command. Syntax: deploylib.sh [repositoryId] [url]
#repositoryId=
#url=

############# methods ##################
validateResult() {
  if [ $? -ne 0 ]; then
    echo -e "error: deploy $1 failed!"
    exit 1;
  else
    echo -e "info: deploy $1 success."
  fi
}

###### deploy springboot dependencies #########
libDir="${lpwd}/lib"
if [ ! -d "$libDir" ]; then
  echo "Directory $libDir does not exist, please put jars into lib directory at first!"
  exit 1
fi

if [ -n "$1" ]; then
  repositoryId=$1
fi

if [ -n "$2" ]; then
  url=$2
fi


if [ -z "$repositoryId" -o -z "$url" ]; then
  echo Please specify repositoryId and url in deploylib.sh
  echo Or you can specify by command parameter, Usage: deploylib [repositoryId] [url]
  exit 1
fi

mvn deploy:deploy-file -Dfile="$libDir/alxas-core-$version.jar" -DgroupId=$groupId -DartifactId=alxas-core -Dversion=$version -Dpackaging=jar -DrepositoryId=$repositoryId -Durl=$url >> deploy.log
validateResult $groupId:alxas-core:$version

mvn deploy:deploy-file -Dfile="$libDir/alxas-websocket-$version.jar" -DgroupId=$groupId -DartifactId=alxas-websocket -Dversion=$version -Dpackaging=jar  -DrepositoryId=$repositoryId -Durl=$url >> deploy.log
validateResult $groupId:alxas-websocket:$version

mvn deploy:deploy-file -Dfile="$libDir/alxas-jasper-$version.jar" -DgroupId=$groupId -DartifactId=alxas-jasper -Dversion=$version -Dpackaging=jar  -DrepositoryId=$repositoryId -Durl=$url >> deploy.log
validateResult  $groupId:alxas-jasper:$version

mvn deploy:deploy-file -Dfile="$libDir/alxas-el-$version.jar" -DgroupId=$groupId -DartifactId=alxas-el -Dversion=$version -Dpackaging=jar  -DrepositoryId=$repositoryId -Durl=$url >> deploy.log
validateResult $groupId:alxas-el:$version

