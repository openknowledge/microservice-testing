#!/usr/bin/env groovy
pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }

    environment {
        SNAPSHOT_VERSION = readMavenPom().getVersion()
        LAST_COMMIT_MESSAGE = "${currentBuild.changeSets.size() == 0 ? 'update version to ' : currentBuild.changeSets[currentBuild.changeSets.size() - 1].items.length == 0 ? 'update version to ' : currentBuild.changeSets[currentBuild.changeSets.size() - 1].items[currentBuild.changeSets[currentBuild.changeSets.size() - 1].items.length - 1].msg}"
        PERFORM_RELEASE = "${env.SNAPSHOT_VERSION.contains('-SNAPSHOT') && env.BRANCH_NAME == 'master' && !env.LAST_COMMIT_MESSAGE.startsWith('update version to ')}"
        RELEASE_VERSION = "${env.SNAPSHOT_VERSION.contains('-SNAPSHOT') ? env.SNAPSHOT_VERSION.substring(0, env.SNAPSHOT_VERSION.lastIndexOf('-SNAPSHOT')) : SNAPSHOT_VERSION}"
        VERSION = "${env.BRANCH_NAME == 'master' && !env.LAST_COMMIT_MESSAGE.startsWith('update version to ') ? env.RELEASE_VERSION : env.SNAPSHOT_VERSION}"
        NAMESPACE = "${env.BRANCH_NAME == 'master' ? 'onlineshop' : 'onlineshop-test'}"
        PORT = "${env.BRANCH_NAME == 'master' ? '30002' : '31002'}"
        HELM_PORT = "${env.BRANCH_NAME == 'master' ? '44134' : '44135'}"
        TEST_PORT = "${env.BRANCH_NAME == 'master' ? '6002' : '6102'}"
        WILDFLY_PORT_OFFSET = "${env.BRANCH_NAME == 'master' ? '-2078' : '-1978'}"
        WILDFLY_MANAGEMENT_PORT = "${env.BRANCH_NAME == 'master' ? '7912' : '8012'}"
    }

    triggers {
        pollSCM("* * * * *")
    }

    stages {
        stage ('Compile') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    environment name: 'PERFORM_RELEASE', value: 'true'
                }
            }
            steps {
                echo "Building version ${env.VERSION}"
                script {
                    if (env.PERFORM_RELEASE.equals('true') && !env.RELEASE_VERSION.equals(env.SNAPSHOT_VERSION)) {
                        sh "mvn versions:set -DnewVersion=${env.RELEASE_VERSION} -B"
                        sh "sed -i 's/${env.SNAPSHOT_VERSION}/${env.RELEASE_VERSION}/g' helm/delivery/Chart.yaml"
                    }
                }
                sh 'mvn clean test-compile -B'
            }
        }
        stage ('Test') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    environment name: 'PERFORM_RELEASE', value: 'true'
                }
            }
            steps {
                sh "mvn test -Dtest.http.port=${env.TEST_PORT} -Dwildfly.port.offset=${env.WILDFLY_PORT_OFFSET} -Dwildfly.management.port=${env.WILDFLY_MANAGEMENT_PORT} -B"
            }
        }
        stage ('Package') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    environment name: 'PERFORM_RELEASE', value: 'true'
                }
            }
            steps {
                sh 'mvn package -DskipTests -B'
                sh 'docker build -t delivery .'
            }
        }
        stage ('Push') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    environment name: 'PERFORM_RELEASE', value: 'true'
                }
            }
            steps {
                sh """
                    docker tag delivery host.docker.internal:5000/delivery:${env.VERSION}
                    docker tag delivery host.docker.internal:5000/delivery:${env.BRANCH_NAME == 'master' ? 'stable' : 'latest'}
                    docker push host.docker.internal:5000/delivery:${env.VERSION}
                    docker push host.docker.internal:5000/delivery:${env.BRANCH_NAME == 'master' ? 'stable' : 'latest'}
                """
                sh """
                    cd ./helm 
                    export HELM_HOST=host.docker.internal:${env.HELM_PORT}
                    helm package ./delivery
                    helm push --force ./delivery chartmuseum
                """
                script {
                    if (env.PERFORM_RELEASE.equals('true') && !env.RELEASE_VERSION.equals(env.SNAPSHOT_VERSION)) {
                        sh "mvn scm:checkin -Dmessage='release of version ${env.RELEASE_VERSION}' -B"
                        sh "mvn scm:tag -Dtag=${env.RELEASE_VERSION} -B"
                        int nextRevision = Integer.parseInt(env.RELEASE_VERSION.substring(env.RELEASE_VERSION.lastIndexOf(".") + 1)) + 1
                        nextVersion = RELEASE_VERSION.substring(0, env.RELEASE_VERSION.lastIndexOf(".")) + "." + nextRevision + "-SNAPSHOT"
                        sh "sed -i 's/${env.RELEASE_VERSION}/${nextVersion}/g' helm/delivery/Chart.yaml"
                        sh "mvn versions:set scm:checkin -DnewVersion=${nextVersion} -Dmessage='update version to ${nextVersion}' -B"
                    }
                }
            }
        }
        stage ('Deploy') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    environment name: 'PERFORM_RELEASE', value: 'true'
                }
            }
            steps {
                sh """
                    set +e
                    export HELM_HOST=host.docker.internal:${env.HELM_PORT}
                    helm repo update
                    helm upgrade delivery --set app.imageTag=${env.VERSION} --set app.service.targetPort=${env.PORT} --namespace ${env.NAMESPACE} chartmuseum/delivery --version=${env.VERSION}
                    if [ \$? -ne 0 ]
                    then
                        set -e
                        helm install --name delivery --set app.imageTag=${env.VERSION} --set app.service.targetPort=${env.PORT} --namespace ${env.NAMESPACE} chartmuseum/delivery --version=${env.VERSION}
                    fi
                """
            }
        }
    }
}
