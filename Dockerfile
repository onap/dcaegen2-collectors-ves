FROM ubuntu:16.04
#FROM dcae-alpine:8-jre-tini

MAINTAINER vv770d@att.com

WORKDIR /opt/app/VESCollector

ENV HOME /opt/app/VESCollector
ENV JAVA_HOME /usr

RUN apt-get update && apt-get install -y \
        curl \
        vim \
        openjdk-8-jdk

COPY opt /opt

EXPOSE 8080

CMD [ "/opt/app/docker-entry.sh" ]
