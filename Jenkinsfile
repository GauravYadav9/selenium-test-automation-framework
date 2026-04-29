@Library('jenkins-pipeline-library@v1.3.2') _

def branchConfig = getBranchConfig()

pipeline {
    agent none 

    environment {
        NETWORK_NAME = "selenium-grid-${env.BRANCH_NAME}".replaceAll('[^a-zA-Z0-9_.-]', '_')
    }

options {
    skipDefaultCheckout()
    durabilityHint(
        env.BRANCH_NAME in ['main'] ?
        'SURVIVABLE_NONATOMIC' :      // Production: safety + speed balance
        'PERFORMANCE_OPTIMIZED'        // Features: maximum speed
    )
    buildDiscarder(logRotator(
        numToKeepStr: '5',
        artifactNumToKeepStr: '5'
    ))
    disableConcurrentBuilds()
}

    triggers {
        cron('H 2 * * *')
    }

    parameters {
        choice(name: 'SUITE_NAME', choices: ['smoke', 'regression'], description: 'Select suite (only applies to manual runs).')
        choice(name: 'TARGET_ENVIRONMENT', choices: ['QA', 'STAGING', 'PRODUCTION'], description: 'Select test environment.')
        booleanParam(name: 'MANUAL_APPROVAL', defaultValue: false, description: 'Only relevant if "regression" is selected. Ignored for "smoke".')
        string(name: 'QASE_TEST_CASE_IDS', defaultValue: '', description: 'Optional: Override default Qase IDs.')
    choice(name: 'QUALITY_GATE_THRESHOLD', choices: ['0', '1', '2', '5'], description: 'Max test failures before quality gate fails')
    booleanParam(name: 'FAIL_ON_NO_TESTS', defaultValue: true, description: 'Mark build UNSTABLE if no test results found')
    }

    stages {
        stage('Determine Suite') {
            agent any
            steps {
                script {
                    env.SUITE_TO_RUN = determineTestSuite()
                }
            }
        }

        stage('Initialize & Start Grid') {
            when {
                expression { return env.BRANCH_NAME in branchConfig.pipelineBranches }
            }
            agent {
                docker {
                    image 'flight-booking-agent-prewarmed:latest'
                    alwaysPull false
                    args "-v /var/run/docker.sock:/var/run/docker.sock --entrypoint=\"\""
                }
            }
            steps {
                retry(2) {
                    echo " Starting Docker Grid..."
                    printBuildMetadata(env.SUITE_TO_RUN)
                    startDockerGrid(
                        'docker-compose-grid.yml',
                        120,
                        5,
                        'http://selenium-hub:4444/wd/hub'
                    )
                }
            }
        }

        stage('Approval Gate (Regression Only)') {
            when {
                allOf {
                    expression { return env.BRANCH_NAME in branchConfig.productionCandidateBranches }
                    expression { return env.SUITE_TO_RUN == 'regression' }
                    expression { return params.MANUAL_APPROVAL == true }
                }
            }
            agent any
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: "Proceed with full regression for branch '${env.BRANCH_NAME}'?"
                }
            }
        }

        stage('Build & Run Parallel Tests') {
            when {
                expression { return env.BRANCH_NAME in branchConfig.pipelineBranches }
            }
            agent none
            steps {
                echo "Running parallel tests for: ${env.SUITE_TO_RUN}"
                retry(2) {
                    timeout(time: 2, unit: 'HOURS') {
                        script {
                            def mvnBase = "mvn -P force-local-cache clean test -P ${env.SUITE_TO_RUN} -Denv=${params.TARGET_ENVIRONMENT} -Dtest.suite=${env.SUITE_TO_RUN} -Dbrowser.headless=true"
                            parallel(
                                Chrome: {
                                    docker.image('flight-booking-agent-prewarmed:latest').inside("-v /var/run/docker.sock:/var/run/docker.sock --network=${env.NETWORK_NAME} --entrypoint=\"\"") {
                                        cleanWs()
                                        checkout scm
                                        sh script: "${mvnBase} -Dbrowser=chrome -Dreport.dir=chrome -Dsurefire.reportsDirectory=target/chrome/surefire-reports", returnStatus: true
                                        stash name: 'chrome-artifacts', includes: 'reports/**, **/surefire-reports/**, **/*-failure-summary.txt', allowEmpty: true
                                    }
                                },
                                Firefox: {
                                    docker.image('flight-booking-agent-prewarmed:latest').inside("-v /var/run/docker.sock:/var/run/docker.sock --network=${env.NETWORK_NAME} --entrypoint=\"\"") {
                                        cleanWs()
                                        checkout scm
                                        sh script: "${mvnBase} -Dbrowser=firefox -Dreport.dir=firefox -Dsurefire.reportsDirectory=target/firefox/surefire-reports", returnStatus: true
                                        stash name: 'firefox-artifacts', includes: 'reports/**, **/surefire-reports/**, **/*-failure-summary.txt', allowEmpty: true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

post {
        always {
            script {

                // Process artifacts in isolated agent context
                docker.image('flight-booking-agent-prewarmed:latest').inside('-v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""') {
                    echo "--- Starting Guaranteed Post-Build Processing (on agent) ---"
                    try {
                        unstash 'chrome-artifacts'
                    } catch (e) {
                        echo "Chrome artifacts not found to unstash."
                    }
                    try {
                        unstash 'firefox-artifacts'
                    } catch (e) {
                        echo "Firefox artifacts not found to unstash."
                    }

                    sh "cp reports/chrome/${env.SUITE_TO_RUN}-chrome-report.html reports/chrome/index.html || echo 'Chrome report not found'"
                    sh "cp reports/firefox/${env.SUITE_TO_RUN}-firefox-report.html reports/firefox/index.html || echo 'Firefox report not found'"
                    junit testResults: '**/surefire-reports/**/*.xml', allowEmptyResults: true
                    generateDashboard(env.SUITE_TO_RUN, "${env.BUILD_NUMBER}")
                    archiveAndPublishReports()

                    // === QUALITY GATE ENFORCEMENT ===
                    def testResults = checkTestFailures()
                    def totalFailures = (testResults.failures ?: 0) + (testResults.errors ?: 0)
                    def maxFailures = params.QUALITY_GATE_THRESHOLD ?
                        params.QUALITY_GATE_THRESHOLD.toInteger() : 0

                    // Handle no tests found scenario
                    if (testResults.total == 0) {
                        if (params.FAIL_ON_NO_TESTS) {
                            currentBuild.result = 'UNSTABLE'
                            echo "Quality Gate: No tests found - marking UNSTABLE"
                        } else {
                            echo "Quality Gate: No tests found - allowed by configuration"
                        }
                    }

                    echo "Quality Gate: ${totalFailures}/${testResults.total} failures (threshold: ${maxFailures})"

                    if (totalFailures > maxFailures) {
                        currentBuild.result = 'UNSTABLE'
                        echo "Quality Gate: Build marked UNSTABLE due to ${totalFailures} failures (threshold: ${maxFailures}). Pipeline continues for telemetry."
                    } else {
                        echo "Quality Gate Passed"
                    }

                    // Advisory AI failure analysis (non-blocking)
                    // Runs AFTER quality gate — advisory only, never blocks build
                    // Only on branches configured in getBranchConfig().aiAnalysisBranches
                    if (env.BRANCH_NAME in branchConfig.aiAnalysisBranches) {
                        analyzeFailuresWithAi()
                    } else {
                        echo "AI Failure Analysis: Disabled for branch '${env.BRANCH_NAME}'"
                    }

                    echo "Stashing reports for notification step."
                    stash name: 'email-reports', includes: 'reports/**'
                    stash name: 'qase-results', includes: '**/testng-results.xml'
                }

                // Unified notifications (Controller context)
                node {
                    echo "--- Preparing notifications on Jenkins Controller (in a node context) ---"
                    try {
                        unstash 'email-reports'
                        unstash 'qase-results'
                    } catch (e) {
                        echo "Could not unstash reports for notification: ${e.getMessage()}"
                    }

                    checkout scm

                    // Qase TMS integration (production branches only)
                    if (env.BRANCH_NAME in branchConfig.productionCandidateBranches) {
                        echo "Running Qase update for production-candidate branch..."
                        try {
                            def qaseConfig = readJSON file: 'cicd/qase_config.json'
                            def suiteSettings = qaseConfig[env.SUITE_TO_RUN]
                            if (suiteSettings) {
                                def qaseIds = (params.QASE_TEST_CASE_IDS?.trim()) ? params.QASE_TEST_CASE_IDS : suiteSettings.testCaseIds
                                updateQase(
                                    projectCode: 'FB',
                                    credentialsId: 'qase-api-token',
                                    testCaseIds: qaseIds
                                )
                            }
                        } catch (err) {
                            echo "Qase update failed: ${err.getMessage()}"
                        }
                    }

                    // Conditional email notifications
                    def currentResult = currentBuild.result ?: 'SUCCESS'
                    def previousResult = currentBuild.previousBuild?.result ?: 'SUCCESS'

                    if (currentResult == 'UNSTABLE' || currentResult == 'FAILURE') {
                        if (env.BRANCH_NAME in branchConfig.productionCandidateBranches || currentResult != previousResult) {
                            echo "Sending notification for build status: ${currentResult}."
                            try {
                                sendBuildSummaryEmail(
                                    suiteName: env.SUITE_TO_RUN,
                                    branchName: env.BRANCH_NAME
                                )
                            } catch (err) {
                                echo "Email notification failed: ${err.getMessage()}"
                            }
                        } else {
                            echo "Skipping email: Build status (${currentResult}) is unchanged from previous build."
                        }
                    }
                }
            }
        }

        success {
            echo "Build SUCCESS. All tests passed."
            script {
                echo "Build duration: ${currentBuild.durationString}"
            }
        }

        unstable {
            script {
                echo "Build UNSTABLE. Tests failed. Check the 'Test Dashboard' for detailed results."
                echo "Build duration: ${currentBuild.durationString}"
                if (env.BRANCH_NAME in branchConfig.productionCandidateBranches) {
                    error("Failing build due to test failures in protected branch '${env.BRANCH_NAME}'.")
                }
            }
        }
        failure {
            echo "Build FAILED. A critical error occurred in one of the stages."
            script {
                echo "Build duration: ${currentBuild.durationString}"
            }
        }
        cleanup {
            script {
                docker.image('flight-booking-agent-prewarmed:latest').inside('-v /var/run/docker.sock:/var/run/docker.sock --entrypoint=""') {
                    echo 'GUARANTEED CLEANUP: Shutting down Selenium Grid...'
                    stopDockerGrid('docker-compose-grid.yml')
                }
            }
        }
    }
}
