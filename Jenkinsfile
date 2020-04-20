#!/usr/bin/env groovy
pipeline {
    agent any

    parameters {
        booleanParam(name: 'verifyPacts', defaultValue: false, description: 'should this job just run to verify pacts from a consumer')
    }

    options {
        disableConcurrentBuilds()
    }

    environment {
        SNAPSHOT_VERSION = readMavenPom().getVersion()
        LAST_COMMIT_MESSAGE = "${currentBuild.changeSets.size() == 0 ? 'update version to ' : currentBuild.changeSets[currentBuild.changeSets.size() - 1].items.length == 0 ? 'update version to ' : currentBuild.changeSets[currentBuild.changeSets.size() - 1].items[currentBuild.changeSets[currentBuild.changeSets.size() - 1].items.length - 1].msg}"
        PERFORM_RELEASE = "${env.SNAPSHOT_VERSION.contains('-SNAPSHOT') && env.BRANCH_NAME == 'master' && !env.LAST_COMMIT_MESSAGE.startsWith('update version to ')}"
        RELEASE_VERSION = "${env.SNAPSHOT_VERSION.contains('-SNAPSHOT') ? env.SNAPSHOT_VERSION.substring(0, env.SNAPSHOT_VERSION.lastIndexOf('-SNAPSHOT')) : SNAPSHOT_VERSION}"
        VERSION = "${env.BRANCH_NAME == 'master' && !env.LAST_COMMIT_MESSAGE.startsWith('update version to ') ? env.RELEASE_VERSION : env.SNAPSHOT_VERSION}"
        ROOT_DIRECTORY = "${params.verifyPacts == true && env.BRANCH_NAME == 'master' && env.SNAPSHOT_VERSION.endsWith("-SNAPSHOT") ? 'target/checkout' : '.'}"
        NAMESPACE = "${env.BRANCH_NAME == 'master' ? 'onlineshop' : 'onlineshop-test'}"
        PORT = "${env.BRANCH_NAME == 'master' ? '30003' : '31003'}"
        STAGE = "${env.BRANCH_NAME == 'master' ? 'prod' : 'test'}"
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
                    expression {
                        params.verifyPacts == true
                    }
                }
            }
            steps {
                echo "Building version ${env.VERSION}"
                script {
                    if (params.verifyPacts == true && env.BRANCH_NAME == 'master' && env.SNAPSHOT_VERSION.endsWith("-SNAPSHOT")) {
                        int previousRevision = Integer.parseInt(env.RELEASE_VERSION.substring(env.RELEASE_VERSION.lastIndexOf(".") + 1)) - 1
                        if (previousRevision >= 0) {
                            previousVersion = RELEASE_VERSION.substring(0, env.RELEASE_VERSION.lastIndexOf(".")) + "." + previousRevision
                            sh "mvn scm:checkout -DscmVersionType=tag -DscmVersion=${previousVersion}"
                            echo "Testing against version ${previousVersion}"
                        }
                    } else if (env.PERFORM_RELEASE.equals('true') && !env.RELEASE_VERSION.equals(env.SNAPSHOT_VERSION)) {
                        sh "mvn versions:set -DnewVersion=${env.RELEASE_VERSION} -B"
                        sh "sed -i 's/${env.SNAPSHOT_VERSION}/${env.RELEASE_VERSION}/g' helm/address-validation/Chart.yaml"
                    }
                }
                sh """
                    cd ${ROOT_DIRECTORY}
                    mvn clean test-compile -B
                """
            }
        }
        stage ('Test') {
            when {
                anyOf {
                    not {
                        branch 'master'
                    }
                    environment name: 'PERFORM_RELEASE', value: 'true'
                    expression {
                        params.verifyPacts == true
                    }
                }
            }
            steps {
                sh """
                    cd ${ROOT_DIRECTORY}
                    mvn test -DpactBroker.url=http://host.docker.internal -Dpact.tag=${params.verifyPacts ? 'pending-' + env.STAGE : env.STAGE} -B
                """
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
                expression {
                    params.verifyPacts == false
                }
            }
            steps {
                sh 'mvn package -DskipTests -B'
                sh 'docker build -t address-validation .'
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
                expression {
                    params.verifyPacts == false
                }
            }
            steps {
                sh """
                    docker tag address-validation host.docker.internal:5000/address-validation:${env.VERSION}
                    docker tag address-validation host.docker.internal:5000/address-validation:${env.BRANCH_NAME == 'master' ? 'stable' : 'latest'}
                    docker push host.docker.internal:5000/address-validation:${env.VERSION}
                    docker push host.docker.internal:5000/address-validation:${env.BRANCH_NAME == 'master' ? 'stable' : 'latest'}
                """
                sh """
                    cd ./helm 
                    helm package ./address-validation
                    helm cm-push --force ./address-validation chartmuseum
                """
                script {
                    if (env.PERFORM_RELEASE.equals('true') && !env.RELEASE_VERSION.equals(env.SNAPSHOT_VERSION)) {
                        sh 'git config --global user.name "Jenkins"'
                        sh 'git config --global user.email "ci@openknowledge.de"'
                        sh "mvn scm:checkin -Dmessage='release of version ${env.RELEASE_VERSION}' -B"
                        sh "mvn scm:tag -Dtag=${env.RELEASE_VERSION} -B"
                        int nextRevision = Integer.parseInt(env.RELEASE_VERSION.substring(env.RELEASE_VERSION.lastIndexOf(".") + 1)) + 1
                        nextVersion = RELEASE_VERSION.substring(0, env.RELEASE_VERSION.lastIndexOf(".")) + "." + nextRevision + "-SNAPSHOT"
                        sh "sed -i 's/${env.RELEASE_VERSION}/${nextVersion}/g' helm/address-validation/Chart.yaml"
                        sh "mvn versions:set scm:checkin -DnewVersion=${nextVersion} -Dmessage='update version to ${nextVersion}' -B"
                    }
                }
            }
        }
        stage ('Deploy') {
            when {
                expression {
                    params.verifyPacts == false
                }
                anyOf {
                    not {
                        branch 'master'
                    }
                    environment name: 'PERFORM_RELEASE', value: 'true'
                }
            }
            steps {
                sh """
                    helm repo update
                    helm upgrade --install address-validation --set app.imageTag=${env.VERSION} --set app.service.targetPort=${env.PORT} --namespace=${env.NAMESPACE} chartmuseum/address-validation --version=${env.VERSION}
                """
                sh "curl -H 'Content-Type: application/json' -X PUT http://host.docker.internal/pacticipants/address-validation-service/versions/${env.VERSION}/tags/${env.STAGE}"
                sh "curl -X DELETE http://host.docker.internal/pacticipants/address-validation-service/versions/${env.VERSION}/tags/pending-${env.STAGE}"
            }
        }
    }
}
