<%--
 Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements.  See the NOTICE file distributed with
  this work for additional information regarding copyright ownership.
  The ASF licenses this file to You under the Apache License, Version 2.0
  (the "License"); you may not use this file except in compliance with
  the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
--%>
<html><body>
<%@ taglib prefix="tags11" uri="http://tomcat.apache.org/tags11" %>
<%@ taglib prefix="tags12" uri="http://tomcat.apache.org/tags12" %>
<%@ taglib prefix="tags20" uri="http://tomcat.apache.org/tags20" %>
<%@ taglib prefix="tags21" uri="http://tomcat.apache.org/tags21" %>
<%@ taglib prefix="tags30" uri="http://tomcat.apache.org/tags30" %>
<%@ taglib prefix="tags31" uri="http://tomcat.apache.org/tags31" %>
<%@ taglib prefix="tags40" uri="http://tomcat.apache.org/tags40" %>
<%@ taglib prefix="tags41" uri="http://tomcat.apache.org/tags41" %>
<tags11:Echo echo="${'00-hello world'}"/>
<tags11:Echo echo="#{'01-hello world'}"/>
<tags12:Echo echo="${'02-hello world'}"/>
<tags12:Echo echo="#{'03-hello world'}"/>
<tags20:Echo echo="${'04-hello world'}"/>
<tags20:Echo echo="#{'05-hello world'}"/>
<tags21:Echo echo="${'06-hello world'}"/>
<tags30:Echo echo="${'07-hello world'}"/>
<tags31:Echo echo="${'08-hello world'}"/>
<tags40:Echo echo="${'09-hello world'}"/>
<tags41:Echo echo="${'10-hello world'}"/>
</body></html>