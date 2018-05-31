FROM openjdk:8-jre-slim

MAINTAINER vv770d@att.com

WORKDIR /opt/app/VESCollector

ENV HOME /opt/app/VESCollector
ENV JAVA_HOME /usr
ENV HOSTALIASES /etc/host.aliases

COPY opt /opt

EXPOSE 8080 8443

CMD [ "/opt/app/docker-entry.sh" ]
