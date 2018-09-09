jdb -classpath "..\..\..\..\libs\jtds.1.3.1.jar;..\..\..\..\libs\servlet-api.jar;c:\cygwin\home\chris\apache-tomcat-6.0.39\webapps\gheskio\WEB-INF\classes" org.gheskio.queue.UploadServlet -j "jdbc:jtds:sqlserver://WAITINGTIME:1093/christest" -u chrisnicholas -p "password@123" -t gheskio_uploaded_events -f ..\..\..\..\misc\test_records.txt


jdb -classpath "..\..\..\..\libs\jtds.1.3.1.jar;..\..\..\..\libs\servlet-api.jar;c:\cygwin\home\chris\apache-tomcat-6.0.39\webapps\gheskio\WEB-INF\classes" org.gheskio.queue.UploadServlet -j "jdbc:jtds:sqlserver://WAITINGTIME:1093/christest" -u chrisnicholas -p "password@123" -t gheskio_uploaded_events -f ..\..\..\..\misc\test_records.txt



"jdb -classpath "/usr/share/tomcat8/lib:javax   -j "jdbc:mysql://localhost:3306/gheskio?useSSL=false" -u "root" -p "gheskioag" -t anything_prob -f emptyfile.txt