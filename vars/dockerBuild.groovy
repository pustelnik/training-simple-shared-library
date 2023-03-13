call(body) {
    pipeline {
        agent {
            label 'jdk17'
        }
        stages {
            stage('Lint') {
                steps {
                    sh 'echo "linting..."'
                }
            }
            stage('Build') {
                steps {
                    sh '''
                        #!/bin/bash
                        for dockerfile in $(find -name Dockerfile) ; do
                            tag="$(dirname "${dockerfile}")":"$(date '+%Y-%m-%d')-${BUILD_NUMBER}"
                            docker build -f ${dockerfile} -t ${tag} .
                        done
                    '''
                }
            }
            stage('Push') {
                when {
                    anyOf {
                        branch 'master'
                        branch 'main'
                    }
                }
                steps {
                    sh '''
                        #!/bin/bash
                        for dockerfile in $(find -name Dockerfile) ; do
                            tag="$(dirname "${dockerfile}")":"$(date '+%Y-%m-%d')-${BUILD_NUMBER}"
                            docker push ${tag} .
                        done
                    '''
                }
            }
        }
    }
}
