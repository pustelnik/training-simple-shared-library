def call(body) {
    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    def buildNode = pipelineParams?.build?.node
    def runTests = pipelineParams?.build?.runTests
    def buildDocker = pipelineParams?.build?.buildDocker
    def trivyScan = pipelineParams?.build?.trivyScan
    def version
    def artifactId

    pipeline {
        agent {
            label buildNode ?: 'jdk17'
        }
        tools {
            maven 'maven-3.9.0'
        }
        stages {
            stage('Setup') {
                steps {
                    script {
                        version = sh script: 'mvn help:evaluate -Dexpression=project.version -q -DforceStdout', returnStdout: true
                        artifactId = sh script: 'mvn help:evaluate -Dexpression=project.artifactId -q -DforceStdout', returnStdout: true
                    }
                }
            }
            stage('Build') {
                steps {
                    sh 'mvn install -DskipTests'
                }
            }
            stage('Test') {
                when {
                    expression { runTests }
                }
                steps {
                    sh 'mvn verify'
                    junit allowEmptyResults: true, testResults: 'target/surefire-reports/*.xml'
                }
            }
            stage('Build Docker') {
                when {
                    expression { buildDocker }
                }
                steps {
                    sh "docker build -t ${artifactId}:${version} ."
                }
            }
            stage('Trivy Scan') {
                when {
                    expression { trivyScan }
                }
                steps {
                    sh "docker run -v /var/run/docker.sock:/var/run/docker.sock aquasec/trivy image ${artifactId}:${version}"
                }
            }
            stage('Deploy Artifacts') {
                when {
                    branch 'main'
                }
                steps {
                    sh "docker push ${artifactId}:${version}"
                }
            }
        }
    }
}