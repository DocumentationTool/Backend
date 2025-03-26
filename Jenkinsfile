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
							sh '''
		                echo "Stopping Tomcat..."
		                sudo /opt/tomcat/bin/shutdown.sh || true

		                # Wait for Tomcat to actually stop
		                while pgrep -f 'org.apache.catalina.startup.Bootstrap' > /dev/null; do
		                    echo "Waiting for Tomcat to stop..."
		                    sleep 1
		                done

		                echo "Tomcat stopped."
		            '''
		        }
		    }
		}


        stage('Deploy WAR to Tomcat') {
			steps {
				script {
					sh 'sudo rm -rf /opt/tomcat/webapps/DocumentationTool*.war'
                    sh 'sudo rm -rf /opt/tomcat/webapps/DocumentationTool*/'
                    sh 'sudo mv /opt/staging/DocumentationTool-0.8.9-plain.war /opt/tomcat/webapps/'
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
