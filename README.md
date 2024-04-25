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

## Use
In both PLAN and APPLY mode it will interpret all yaml files in state config file and directory.
It will attempt to reconcile them with the current state of the cluster configured in the kafkaConfig.

There is also a reporting functionality. 
It will check the ACL's against some rules. 
More rules can be added with simple coding.

## Configurations

| CLI Parameter          | Shorthand | Env var                     |
|------------------------|-----------|-----------------------------|
| kafkaConfig            | config    | KAFKA_CONFIG_FILE           | 
| stateFileLocation      | file      | STATE_FILE_LOCATION         |
| stateDirectoryLocation | dir       | STATE_DIRECTORY_LOCATION    |
| plan/apply/import      | -p/-a/-i  | MODE (expects long version) |

## Development

Functions like any maven project. 
Build and develop locally.
```mvn clean install```

To build a container simply build the dockerfile.
It is a multistage docker build.
```docker build .```

## Example
There is an example configuration of CI with Google Cloud Build (GCP version of tekton pipelines)
Its easily portable to other container based CI solutions.
