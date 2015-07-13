# CassTimeSeries
## Time Series DataBase using C* and Spark.

### Time Series Pattern 1 - Single Device per Row
The simplest model for storing time series data is creating a wide row of data for each source. 
In this first example, we will use the weather station ID as the row key. 
The timestamp of the reading will be the column name and the temperature the column value (figure 1). 
Since each column is dynamic, our row will grow as needed to accommodate the data. We will also get the built-in sorting of 
Cassandra to keep everything in order.

![alt](https://github.com/letslego/CassTimeSeries/blob/master/images/TimeSeries1.png)

```
CREATE KEYSPACE test WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1 };
CREATE TABLE test.temperature (
weatherstation_id text,
event_time timestamp,
temperature text,
PRIMARY KEY (weatherstation_id,event_time)
);
```
And insert some dummy data

```
INSERT INTO test.temperature(weatherstation_id,event_time,temperature) VALUES ('1234ABCD','2013-04-03 07:01:00','72F');
INSERT INTO test.temperature(weatherstation_id,event_time,temperature) VALUES ('1234ABCD','2013-04-03 07:02:00','73F');
INSERT INTO test.temperature(weatherstation_id,event_time,temperature) VALUES ('1234ABCD','2013-04-03 07:03:00','73F');
INSERT INTO test.temperature(weatherstation_id,event_time,temperature) VALUES ('1234ABCD','2013-04-03 07:04:00','74F');
```

Simple query:

```
SELECT event_time,temperature
FROM test.temperature
WHERE weatherstation_id='1234ABCD';
```

Range Query :

```
SELECT temperature
FROM test.temperature
WHERE weatherstation_id='1234ABCD'
AND event_time > '2013-04-03 07:01:00'
AND event_time < '2013-04-03 07:04:00';
```

### Time Series Pattern 2 - Partitioning to Limit Row Size
In some cases, the amount of data gathered for a single device isn't practical to fit onto a single row. 
Cassandra can store up to 2 billion columns per row, but if we're storing data every millisecond you wouldn't even 
get a month's worth of data. The solution is to use a pattern called row partitioning by adding data to the row key 
to limit the amount of columns you get per device. Using data already available in the event, we can use the date portion 
of the timestamp and add that to the weather station id. This will give us a row per day, per weather station, and an easy 
way to find the data. (figure 2)  

![alt](https://github.com/letslego/CassTimeSeries/blob/master/images/TimeSeries2.png)

```
CREATE TABLE test.temperature_by_day (
weatherstation_id text,
date text,
event_time timestamp,
temperature text,
PRIMARY KEY ((weatherstation_id,date),event_time)
);
```
Here (weatherstation_id,date) is the PRIMARY KEY compounded with two elements, and event_time is the clustering column.

```
INSERT INTO test.temperature_by_day(weatherstation_id,date,event_time,temperature) VALUES ('1234ABCD','2013-04-03','2013-04-03 07:01:00','72F');
INSERT INTO test.temperature_by_day(weatherstation_id,date,event_time,temperature) VALUES ('1234ABCD','2013-04-03','2013-04-03 07:02:00','73F');
INSERT INTO test.temperature_by_day(weatherstation_id,date,event_time,temperature) VALUES ('1234ABCD','2013-04-04','2013-04-04 07:01:00','73F');
INSERT INTO test.temperature_by_day(weatherstation_id,date,event_time,temperature) VALUES ('1234ABCD','2013-04-04','2013-04-04 07:02:00','74F');
```
For faster retrieval of data for a single day, we need to query on both the columns of the PK.

```
SELECT *
FROM test.temperature_by_day
WHERE weatherstation_id='1234ABCD'
AND date='2013-04-03';
```

### Time Series Pattern 3 - Reverse Order Timeseries with Expiring Columns

Another common pattern with time series data is rolling storage. 
Imagine we are using this data for a dashboard application and we only want to show the last 10 temperature readings. 
Older data is no longer useful, so can be purged eventually. With many other databases, you would have to setup a background 
job to clean out older data. With Cassandra, we can take advantage of a feature called expiring columns to have our data quietly 
disappear after a set amount of seconds. (figure 3)

![alt](https://github.com/letslego/CassTimeSeries/blob/master/images/TimeSeries3.png)

```
CREATE TABLE test.latest_temperatures (
weatherstation_id text,
event_time timestamp,
temperature text,
PRIMARY KEY (weatherstation_id,event_time),
) WITH CLUSTERING ORDER BY (event_time DESC);
```

Now when we insert data. Note the TTL of 20 which means the data will expire in 20 seconds:

```
 INSERT INTO test.latest_temperatures(weatherstation_id,event_time,temperature) VALUES ('1234ABCD','2013-04-03 07:03:00','72F') USING TTL 20;
 INSERT INTO test.latest_temperatures(weatherstation_id,event_time,temperature) VALUES ('1234ABCD','2013-04-03 07:02:00','73F') USING TTL 20;
 INSERT INTO test.latest_temperatures(weatherstation_id,event_time,temperature) VALUES ('1234ABCD','2013-04-03 07:01:00','73F') USING TTL 20;
 INSERT INTO test.latest_temperatures(weatherstation_id,event_time,temperature) VALUES ('1234ABCD','2013-04-03 07:04:00','74F') USING TTL 20;
```

I encourage you to use the public apis to gather weather data, e.g. 
[Open Weather Map](http://openweathermap.org/current)