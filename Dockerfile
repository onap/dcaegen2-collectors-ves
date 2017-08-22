FROM ubuntu:16.04

MAINTAINER vv770d@att.com

WORKDIR /opt/app/manager
#WORKDIR /opt/app/VESCollector

ENV HOME /opt/app/VESCollector
ENV JAVA_HOME /usr

RUN apt-get update && apt-get install -y \
        bc \
        curl \
        telnet \
        vim \
        netcat \
        openjdk-8-jdk


COPY opt /opt

EXPOSE 9999 8080 8443

#ENTRYPOINT [ "/usr/bin/tini", "--" ]

CMD [ "/opt/app/docker-entry.sh" ]