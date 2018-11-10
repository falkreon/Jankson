pipeline {
	agent any
	stages {
		stage('Build') {
			steps {
				sh './gradlew'
				archive 'build/libs/*jar'
			}
		}
	}
}
