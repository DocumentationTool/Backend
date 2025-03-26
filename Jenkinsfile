pipeline {
	agent any

    environment {
		WAR_NAME = "DocumentationTool-0.8.9.war"
        STAGING_PATH = "/opt/staging"
    }

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
                    sh "mv ./build/libs/${WAR_NAME} ${STAGING_PATH}"
                }
            }
        }

        stage('Stop Running App') {
			steps {
				script {
					sh '''
                        PID=$(pgrep -f "${WAR_NAME}")
                        if [ ! -z "$PID" ]; then
                            echo "Stopping running app (PID=$PID)..."
                            kill $PID
                            while kill -0 $PID 2>/dev/null; do
                                echo "Waiting for process to stop..."
                                sleep 1
                            done
                        else
                            echo "No running instance found."
                        fi
                    '''
                }
            }
        }

        stage('Start WAR File') {
			steps {
				script {
					sh '''
                        echo "Starting Spring Boot WAR..."
                        nohup java -jar /opt/staging/DocumentationTool-0.8.9-plain.war > /opt/staging/app.log 2>&1 &
                    '''
                }
            }
        }
    }

    post {
		always {
			echo 'Pipeline execution finished.'
        }

        success {
			echo '✅ Deployment was successful.'
        }

        failure {
			echo '❌ Deployment failed.'
        }
    }
}
