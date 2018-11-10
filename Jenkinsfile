pipeline {
	agent any
	stages {
		stage('Build') {
			steps {
				sh 'rm -f private.gradle'
				sh './gradlew clean build sourcesJar'
				archive 'build/libs/*jar'
			}
		}
		stage('Deploy') {
			steps {
				withCredentials([file(credentialsId: 'privateGradlePublish', variable: 'PRIVATEGRADLE')]) {
					sh '''
						cp "$PRIVATEGRADLE" private.gradle
						./gradlew publish
					'''
				}
			}
		}
	}
}
