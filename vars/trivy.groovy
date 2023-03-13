def call(String imageName) {
    node('jdk17') {
        sh "docker run -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image ${artifactId}:${version}"
    }
}
