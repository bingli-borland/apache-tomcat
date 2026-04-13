#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# 设置版本号
VERSION="1.0.0"
TAR_NAME="ALX-APPSERVER-SPRINGBOOT-${VERSION}"
cd output/embed
# 创建临时目录结构
mkdir -p temp_dir
mkdir -p temp_dir/lib

# 复制所有脚本文件到根目录（.sh和.bat都要保留）
# 复制shell脚本
if [ -f "deploylib.sh" ]; then
    cp deploylib.sh temp_dir/
fi
if [ -f "installlib.sh" ]; then
    cp installlib.sh temp_dir/
fi

# 复制bat脚本
if [ -f "deploylib.bat" ]; then
    cp deploylib.bat temp_dir/
fi
if [ -f "installlib.bat" ]; then
    cp installlib.bat temp_dir/
fi

# 复制README.md到根目录
if [ -f "README.md" ]; then
    # 可选：处理README.md中的版本号信息
    sed "s/tomcat-embed/alxas/gI" README.md > temp_dir/README.md
else
    # 创建默认README
    cat > temp_dir/README.md << EOF
# ALX AppServer SpringBoot Libraries

Version: ${VERSION}

## Files
- deploylib.sh - Deployment script (Unix/Linux)
- deploylib.bat - Deployment script (Windows)
- installlib.sh - Installation script (Unix/Linux)
- installlib.bat - Installation script (Windows)
- lib/ - Directory containing embed libraries
EOF
fi

# 复制并重命名jar文件，添加版本号
cp tomcat-embed-jasper.jar temp_dir/lib/alxas-jasper-${VERSION}.jar
cp tomcat-embed-core.jar temp_dir/lib/alxas-core-${VERSION}.jar
cp tomcat-embed-websocket.jar temp_dir/lib/alxas-websocket-${VERSION}.jar
cp tomcat-embed-el.jar temp_dir/lib/alxas-el-${VERSION}.jar

# 进入临时目录打包
cd temp_dir
tar -czf ../${TAR_NAME}.tar.gz *

# 返回并生成MD5
cd ..
md5sum ${TAR_NAME}.tar.gz > ${TAR_NAME}.tar.gz.md5

# 清理临时目录
rm -rf temp_dir

echo "打包完成！"
echo "生成的文件："
echo "  - ${TAR_NAME}.tar.gz"
echo "  - ${TAR_NAME}.tar.gz.md5"
echo ""
echo "压缩包目录结构："
tar -tzf ${TAR_NAME}.tar.gz | sort