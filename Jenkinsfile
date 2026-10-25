pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 15, unit: 'MINUTES')
    }

    triggers {
        pollSCM('H/5 * * * *')
    }

    environment {
        SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/urlshortener_ci'
        SPRING_DATASOURCE_USERNAME = 'urlshortener'
        SPRING_DATASOURCE_PASSWORD = credentials('urlshortener-ci-db-password')
    }

    stages {
        stage('Test') {
            steps {
                sh './mvnw -B test'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }
        stage('Package') {
            steps {
                sh './mvnw -B -DskipTests package'
                archiveArtifacts artifacts: 'target/*.jar', fingerprint: true
            }
        }
    }
}
