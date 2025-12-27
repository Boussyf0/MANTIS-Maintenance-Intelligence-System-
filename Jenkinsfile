pipeline {
    agent any

    environment {
        SONAR_TOKEN = credentials('sonar-token')
        JAVA_HOME = tool name: 'jdk-17', type: 'jdk'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test Java Services') {
            parallel {
                stage('Ingestion Service') {
                    steps {
                        dir('services/ingestion-iiot') {
                            sh 'mvn -B clean verify'
                        }
                    }
                }
                stage('Dashboard API') {
                    steps {
                        dir('services/dashboard-api') {
                            sh 'mvn -B clean verify'
                        }
                    }
                }
                stage('Orchestrator') {
                    steps {
                        dir('services/orchestrator') {
                            sh 'mvn -B clean verify'
                        }
                    }
                }
            }
        }

        stage('Test Python Services') {
            steps {
                script {
                    def services = ['feature-extraction', 'anomaly-detection', 'rul-prediction']
                    services.each { service ->
                        dir("services/${service}") {
                            sh 'pip install -r requirements.txt'
                            sh 'python -m pytest tests/'
                        }
                    }
                }
            }
        }

        stage('Code Quality (SonarQube)') {
            steps {
                withSonarQubeEnv('SonarCloud') {
                    sh '''
                    mvn -B verify sonar:sonar \
                        -Dsonar.projectKey=mantis-pr_mantis-project \
                        -Dsonar.organization=mantis-pr \
                        -Dsonar.host.url=https://sonarcloud.io \
                        -Dsonar.login=$SONAR_TOKEN
                    '''
                }
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker-compose -f docker-compose.services.yml build'
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
            archiveArtifacts artifacts: '**/target/*.jar', allowEmptyArchive: true
        }
        failure {
            echo "Pipeline failed. Check logs."
        }
    }
}
