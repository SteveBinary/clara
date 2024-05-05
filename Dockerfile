FROM eclipse-temurin:21-jre
EXPOSE 7878/tcp

# Make the kubeconfig available for the fabric8 Kubernetes client
COPY clara-app/src/main/resources/config /root/.kube/config

RUN mkdir /app
WORKDIR /app

COPY clara-app/build/libs /app/

# Install GraphViz
RUN apt-get update
RUN apt-get install -y graphviz
# Install syft
RUN curl -sSfL https://raw.githubusercontent.com/anchore/syft/main/install.sh | sh -s -- -b /usr/local/bin

CMD ["sh", "-c", "java -jar clara-app-*-standalone.jar"]