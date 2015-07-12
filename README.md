# CassTimeSeries
Time Series DataBase using C* and Spark.

## Time Series Pattern 1- Single Device per Row
The simplest model for storing time series data is creating a wide row of data for each source. 
In this first example, we will use the weather station ID as the row key. 
The timestamp of the reading will be the column name and the temperature the column value (figure 1). 
Since each column is dynamic, our row will grow as needed to accommodate the data. We will also get the built-in sorting of 
Cassandra to keep everything in order.

![alt](https://github.com/letslego/CassTimeSeries/blob/master/images/TimeSeries1.png)
