
## How the application starts:
General flow goes like this
- Docker image is build, and it points docker-entry.sh as the entrypoint.
- Docker-entry point, depending on the deployment type,
configures a bunch of things and starts the application in a separate process
and loops indefinitely to hold the docker container process.