# Kafka Admin Util

The Kafka Admin Util is a CLI to manage Kafka ACL's and Topics.
Its lightweight in implementation and declarative in configuration.
Main goals was to create a tool thats extendable and integrates well with CI/CD.

ACL's are supported. Support for Topics is under development.

## Modes

There are 3 modes
- PLAN: Will parse the desired state & current state and report what needs changing.
- APPLY: Will execute what plan reports.
- IMPORT: Will read the current cluster state and print it as output. This mode will also report topics.

## Development

Functions like any maven project. 
Build and develop locally.
```mvn clean install```

To build a container simply build the dockerfile.
It is a multistage docker build.
```docker build .```


