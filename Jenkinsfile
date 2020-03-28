#!/usr/bin/env groovy

pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }
    stages {
        stage('Build') {
            steps {
                script {
                    properties([pipelineTriggers([snapshotDependencies()])])
                }
                withMaven(
                    jdk: 'JDK_1.8.0_191',
                    maven: 'Maven3.1.1',
                    mavenSettingsFilePath : 'settings.xml') {
                    sh 'mvn clean compile'
                }
            }
        }
        stage('Test') {
            steps {
                withMaven(
                    jdk: 'JDK_1.8.0_191',
                    maven: 'Maven3.1.1',
                    mavenOpts: '-Dmaven.main.skip',
                    mavenSettingsFilePath : 'settings.xml') {
                    sh 'mvn test'
                }
            }
        }
        stage('Publish') { 
            when { branch 'master' }
            steps {
                withMaven(
                    jdk: 'JDK_1.8.0_191',
                    maven: 'Maven3.1.1',
                    mavenOpts: '-Dmaven.main.skip -Dmaven.test.skip=true -DskipTests',
                    mavenSettingsFilePath : 'settings.xml') {
                    sh 'mvn deploy'
                }
            }
        }
    }
    post {
        always {
            deleteDir() /* clean up our workspace */
        }
    }
}
