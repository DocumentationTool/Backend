pipeline {
    agent any

    stages {
        stage('Pull Latest Changes') {
            steps {
                git 'https://github.com/DocumentationTool/Backend'
            }
        }

		stage('Build Backend') {
		    steps {
		        script {
		            sh 'chmod +x ./gradlew'
		            sh './gradlew build'
		        }
		    }
		}


        stage('Move WAR to Staging Folder') {
            steps {
                script {
                	sh 'find . -name "*.war"'
                    sh 'mv ./build/libs/DocumentationTool-0.8.9-plain.war /opt/staging'
                }
            }
        }

        stage('Shutdown Tomcat') {
            steps {
                script {
                    sh 'sudo /opt/tomcat/bin/shutdown.sh'
                }
            }
        }

        stage('Deploy WAR to Tomcat') {
            steps {
                script {
                    sh 'sudo chown -R jenkins:jenkins /opt/tomcat/webapps'
                    sh 'mv /opt/staging/DocumentationTool-0.8.9-plain.war /opt/tomcat/webapps'
                }
            }
        }

        stage('Start Tomcat') {
            steps {
                script {
                    sh 'sudo /opt/tomcat/bin/startup.sh'
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution finished.'
        }

        success {
            echo 'Deployment was successful.'
        }

        failure {
            echo 'Deployment failed.'
        }
    }
}
