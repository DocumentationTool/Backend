pipeline {
	agent any

    environment {
		WAR_NAME = "MarkDoc-Backend.war"
        STAGING_PATH = "/opt/staging"
        JAVADOC_PATH = "build/docs/javadoc"
        JAVADOC_DEPLOY_PATH = "/var/www/html/javadoc" // change this as needed
    }

    stages {

		stage('Build Backend') {
			steps {
				script {
					sh 'chmod +x ./gradlew'
                    sh './gradlew build --info'
                }
            }
        }

	stage('Generate Aggregated Javadoc') {
			steps {
				script {
					sh './gradlew aggregateJavadoc'
            		sh 'ls -al build/docs/javadoc || echo "No Javadoc generated"'
        	}
    	}
	}


        stage('Publish Javadoc') {
			steps {
				script {
					sh "echo 🧹 Cleaning old Javadoc..."
                	sh "rm -rf ${JAVADOC_DEPLOY_PATH}/*"
					sh "mkdir -p ${JAVADOC_DEPLOY_PATH}"
                    sh "cp -r ${JAVADOC_PATH}/* ${JAVADOC_DEPLOY_PATH}/"
                    echo "✅ Javadoc published to ${JAVADOC_DEPLOY_PATH}"
                }
            }
        }

        stage('Move WAR to Staging Folder') {
			steps {
				script {
					sh "mv ./build/libs/${WAR_NAME} ${STAGING_PATH}"
                }
            }
        }
        /*

        stage('Stop Running App') {
			steps {
				script {
					sh """
                        PID=\$(pgrep -f "${WAR_NAME}" || true)
                        if [ -z "\$PID" ]; then
                            echo "No running instance found."
                        else
                            echo "Stopping running app (PID=\$PID)..."
                            kill \$PID
                            while kill -0 \$PID 2>/dev/null; do
                                echo "Waiting for process to stop..."
                                sleep 1
                            done
                        fi
                    """
                }
            }
        }

        stage('Start WAR File') {
			steps {
				script {
					sh '''
                        echo Starting Spring Boot WAR...
                        nohup java -jar /opt/staging/MarkDoc-Backend.war > /opt/staging/nohup.out 2>&1 &
                        sleep 5
                        pgrep -f MarkDoc-Backend.war || echo "⚠️ WAR failed to start"
                    '''
                }
            }
        }
    }
    */


    stage('Restart MarkDoc Service') {
			steps {
				script {
					sh '''
					echo "Restarting MarkDoc systemd service..."
					sudo systemctl restart markdoc.service
					sleep 5
					sudo systemctl status markdoc.service || echo "⚠️ Service failed to start"
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
