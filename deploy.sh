echo "<<Pulling the latest changes from 'origin/main'...>>"
git pull origin main &&
echo "<<Cleaning the project...>>"
sbt clean &&
echo "<<Building the project...>>"
sbt clean assembly &&
echo "<<Starting the application...>>"
ps -ef | grep java | grep -v grep | awk '{print $2}' | xargs kill &&
nohup java -jar target/scala-3.2.2/agar-winner-assembly-0.1.0.jar > output.log 2>&1 &
echo "<<Application started successfully!>>"
