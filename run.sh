#!/usr/bin/env sh
if [ -z ${KAFKA_CONFIG_FILE} ]; then echo "KAFKA_CONFIG_FILE env var unset"; else echo "Kafka config path '$KAFKA_CONFIG_FILE'"; fi
if [ -z ${STATE_FILE_LOCATION} ]; then echo "STATE_FILE_LOCATION env var unset"; else echo "State file location '$STATE_FILE_LOCATION'"; fi
if [ -z ${STATE_DIRECTORY_LOCATION} ]; then echo "STATE_DIRECTORY_LOCATION env var unset"; else echo "State dir location '$STATE_DIRECTORY_LOCATION'"; fi
if [ -z ${MODE} ]; then echo "MODE env var unset"; else echo "mode '$MODE'"; fi

java -jar /KafkaAdminHelper.jar --$MODE --config $KAFKA_CONFIG_FILE --file $STATE_FILE_LOCATION --dir $STATE_DIRECTORY_LOCATION
