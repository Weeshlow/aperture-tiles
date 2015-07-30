#!/bin/bash


MAIN_CLASS=com.uncharted.tile.source.server.app.ServerApp
MAIN_JAR=build/libs/server-0.7-SNAPSHOT-cdh5.4.1-assembly.jar
JOB_MASTER=yarn-client

spark-submit --num-executors 12 --executor-memory 10g --executor-cores 4 \
	--master ${JOB_MASTER} --class ${MAIN_CLASS} ${MAIN_JAR}
